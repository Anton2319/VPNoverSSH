package ru.anton2319.vpnoverssh;

import android.content.Intent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SocksProxyService extends VpnService {

    private static final String TAG = "SocksProxyService";

    private Thread vpnThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the VPN thread
        vpnThread = newVpnThread();
        SocksPersistent.getInstance().setVpnThread(vpnThread);
        vpnThread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Shutting down gracefully");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Intent sshIntent = StatusInfo.getInstance().getSshIntent();
                stopService(sshIntent);
            }
        });
        try {
            ParcelFileDescriptor pfd = SocksPersistent.getInstance().getVpnInterface();
            if (pfd != null) {
                pfd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Cannot handle shutdown gracefully, killing the service");
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

    private void startVpn() throws IOException {
        try {
            // Get the FileDescriptor for the VPN interface
            ParcelFileDescriptor vpnInterface;
            Builder builder = new VpnService.Builder();
            builder.setMtu(1500).addAddress("26.26.26.1", 24);
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                builder.addRoute(new IpPrefix(InetAddress.getByName("0.0.0.0"), 0));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("8.8.8.8"), 32));
            } else {
                String[] excludedIps = {"8.8.8.8"};
                addRoutesExcluding(builder, excludedIps);
            }
            vpnInterface = builder.addDnsServer("8.8.8.8")
                    .addDisallowedApplication("ru.anton2319.vpnoverssh")
                    .establish();
            SocksPersistent.getInstance().setVpnInterface(vpnInterface);

            String socksHostname = "127.0.0.1";
            int socksPort = 1080;

            // Initialize proxy
            engine.Key key = new engine.Key();
            key.setMark(0);
            key.setMTU(1500);
            key.setDevice("fd://" + vpnInterface.getFd());
            key.setInterface("");
            key.setLogLevel("warning");
            key.setProxy("socks5://127.0.0.1:1080");
            key.setRestAPI("");
            key.setTCPSendBufferSize("");
            key.setTCPReceiveBufferSize("");
            key.setTCPModerateReceiveBuffer(false);

            engine.Engine.insert(key);
            engine.Engine.start();

            while (true) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "Interruption signal received");
                    throw new InterruptedException();
                }
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Stopping service");
            onDestroy();
        } catch (Exception e) {
            Log.e(TAG, "VPN thread error: ", e);
            e.printStackTrace();
            stopSelf();
        }
    }

    private Thread newVpnThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startVpn();
                } catch (IOException e) {
                    Log.d(TAG, "Failed to release system resources! This behaviour may lead to memory leaks!");
                } finally {
                    stopSelf();
                }
            }
        });
    }

    private void addRoutesExcluding(Builder builder, String[] excludedIps) {
        for (int i = 1; i < 255; i++) {
            String block8 = i + ".0.0.0/8";
            if (!blockIsExcluded(block8, excludedIps)) {
                if(!block8.equals("127.0.0.0/8")) {
                    try {
                        builder.addRoute(i + ".0.0.0", 8);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                for (int j = 0; j < 256; j++) {
                    String block16 = i + "." + j + ".0.0/16";
                    if (!blockIsExcluded(block16, excludedIps)) {
                        builder.addRoute(i + "." + j + ".0.0", 16);
                    } else {
                        for (int k = 0; k < 256; k++) {
                            String block24 = i + "." + j + "." + k + ".0/24";
                            if (!blockIsExcluded(block24, excludedIps)) {
                                builder.addRoute(i + "." + j + "." + k + ".0", 24);
                            } else {
                                for (int l = 1; l < 256; l++) {
                                    String ipAddress = i + "." + j + "." + k + "." + l;
                                    if (!isExcluded(ipAddress, excludedIps)) {
                                        Log.d(TAG, ipAddress);
                                        builder.addRoute(ipAddress, 32);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean blockIsExcluded(String block, String[] excludedIps) {
        for (String excludedIp : excludedIps) {
            if (isIpWithinRange(excludedIp, block)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(String ipAddress, String[] excludedIps) {
        for (String excludedIp : excludedIps) {
            if (ipAddress.equals(excludedIp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIpWithinRange(String ip, String cidrNotation) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return false;
        }
        SubnetUtils subnet = new SubnetUtils(cidrNotation);
        return subnet.getInfo().isInRange(inetAddress.getHostAddress());
    }
}