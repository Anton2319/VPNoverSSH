package ru.anton2319.vpnoverssh;

import android.content.Intent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

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
                ArrayList<Long> excludedIps = new ArrayList<>();
                excludedIps.add(ipATON("1.1.1.1"));
                excludedIps.add(ipATON("8.8.8.8"));
                addRoutesExcluding(builder, excludedIps);
            }
            vpnInterface = builder.addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
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

    public static void addRoutesExcluding(Builder builder, ArrayList<Long> excludedIpsAton) {
        Collections.sort(excludedIpsAton);
        // bypass local subnet
        long currentAddress = ipATON("0.0.0.0");
        long endAddress = ipATON("126.255.255.255");

        while(currentAddress <= endAddress) {
            int mask = getMaximumMask(currentAddress, excludedIpsAton.isEmpty() ? endAddress : excludedIpsAton.get(0));
            long resultingAddress = currentAddress + subnetSize(mask);
            if(excludedIpsAton.contains(currentAddress)) {
                Log.d(TAG, "Excluding: "+ipNTOA(currentAddress)+"/"+mask);
                excludedIpsAton.remove(0);
            }
            else {
                Log.v(TAG, "Adding: "+ipNTOA(currentAddress)+"/"+mask);
                builder.addRoute(ipNTOA(currentAddress), mask);
            }
            currentAddress = resultingAddress;
        }

        // bypass multicast
        currentAddress = ipATON("128.0.0.0");
        endAddress = ipATON("223.255.255.255");


        while(currentAddress <= endAddress) {
            int mask = getMaximumMask(currentAddress, excludedIpsAton.isEmpty() ? endAddress : excludedIpsAton.get(0));
            long resultingAddress = currentAddress + subnetSize(mask);
            if(excludedIpsAton.contains(currentAddress)) {
                Log.d(TAG, "Excluding: "+ipNTOA(currentAddress)+"/"+mask);
                excludedIpsAton.remove(0);
            }
            else {
                Log.v(TAG, "Adding: "+ipNTOA(currentAddress)+"/"+mask);
                builder.addRoute(ipNTOA(currentAddress), mask);
            }
            currentAddress = resultingAddress;
        }

        // all other addresses
        currentAddress = ipATON("240.0.0.0");
        endAddress = ipATON("255.255.255.255");


        while(currentAddress <= endAddress) {
            int mask = getMaximumMask(currentAddress, excludedIpsAton.isEmpty() ? endAddress : excludedIpsAton.get(0));
            long resultingAddress = currentAddress + subnetSize(mask);
            if(excludedIpsAton.contains(currentAddress)) {
                Log.d(TAG, "Excluding: "+ipNTOA(currentAddress)+"/"+mask);
                excludedIpsAton.remove(0);
            }
            else {
                Log.v(TAG, "Adding: "+ipNTOA(currentAddress)+"/"+mask);
                builder.addRoute(ipNTOA(currentAddress), mask);
            }
            currentAddress = resultingAddress;
        }
    }

    public static int getMaximumMask(long startingAddress, long maximumAddress) {
        int subnetMask = 32;
        int maximumSubnetLimit = 8;
        String[] ntoa_split = ipNTOA(startingAddress).split("\\.");
        if(!ntoa_split[1].equals("0")) {
            maximumSubnetLimit = 16;
            if(!ntoa_split[2].equals("0")) {
                maximumSubnetLimit = 24;
                if(!ntoa_split[3].equals("0")) {
                    maximumSubnetLimit = 32;
                }
            }
        }
        while (subnetMask > maximumSubnetLimit) {
            long subnetSize = subnetSize(subnetMask - 1);
            if((startingAddress + subnetSize) < maximumAddress) {
                subnetMask = subnetMask - 1;
            }
            else {
                break;
            }
        }
        return subnetMask;
    }

    public static long subnetSize(long subnetMask) {
        return (long) Math.pow(2, 32 - subnetMask);
    }

    public static long ipATON(String ip) {
        String[] addrArray = ip.split("\\.");
        long num = 0;
        for (int i = 0; i < addrArray.length; i++)
        {
            int power = 3 - i;
            num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
        }
        return num;
    }

    public static String ipNTOA(long binaryIp) {
        StringBuilder dottedDecimal = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            long octet = (binaryIp >> (i * 8)) & 0xFF;
            dottedDecimal.append(octet);
            if (i > 0) {
                dottedDecimal.append(".");
            }
        }
        return dottedDecimal.toString();
    }
}