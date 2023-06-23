package ru.anton2319.vpnoverssh;

import android.content.Intent;
import android.content.res.Resources;
import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.Uri;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import engine.Engine;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;

public class SocksProxyService extends VpnService {

    private static final String TAG = "SocksProxyService";

    private Thread vpnThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the VPN thread
        vpnThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startVpn();
                }
                catch (IOException e) {
                    Log.d(TAG, "Failed to release system resources! This behaviour may lead to memory leaks!");
                }
                finally {
                    stopSelf();
                }
            }
        });
        vpnThread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy method invoked");
        shutdownVpn();
    }

    private void startVpn() throws IOException {
        try {
            // Get the FileDescriptor for the VPN interface
            Builder builder = new VpnService.Builder();
            ParcelFileDescriptor vpnInterface = builder
                    .setMtu(1500)
                    .addAddress("26.26.26.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .excludeRoute(new IpPrefix(InetAddress.getByName("8.8.8.8"), 32))
                    .addDnsServer("8.8.8.8")
                    .addDisallowedApplication("com.termux")
                    .addDisallowedApplication("ru.anton2319.vpnoverssh")
                    .establish();
            FileDescriptor tunInterface = vpnInterface.getFileDescriptor();

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

            while (true) {}
        }
        catch (Exception e) {
            Log.e(TAG, "VPN thread error: ", e);
            e.printStackTrace();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            vpnThread.interrupt();
        }
    }

    private void shutdownVpn() {
        // Stop the VPN thread
        Log.d(TAG, "Shutting down gracefully");
        vpnThread.interrupt();
        stopForeground(true);
    }
}