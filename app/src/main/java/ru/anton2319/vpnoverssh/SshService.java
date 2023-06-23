package ru.anton2319.vpnoverssh;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


import com.trilead.ssh2.*;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.DynamicPortForwarder;

import java.io.File;
import java.io.IOException;

public class SshService extends Service {

    private static final String TAG = "SshService";
    private Session session;
    private Thread sshThread;
    Connection conn;
    DynamicPortForwarder forwarder;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the SSH tunneling code
        sshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.setProperty("user.home", getFilesDir().getAbsolutePath());
                    initiateSSH(intent);
                } catch (InterruptedException e) {
                    forwarder.close();
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "trilead.ssh2 failed, and there is no fallback yet ¯\\_(ツ)_/¯");
                }
            }
        });
        sshThread.start();

        return START_STICKY;
    }

    public void initiateSSH(Intent intent) throws Exception {
        Log.d(TAG, "Starting trilead.ssh2 service");

        String user = intent.getStringExtra("user");
        String host = intent.getStringExtra("hostname");
        String password = intent.getStringExtra("password");
        int port = Integer.parseInt(intent.getStringExtra("port"));
        String privateKey = intent.getStringExtra("privateKey");
        String privateKeyFilePath = File.createTempFile("vpnoverssh", null, null).getPath();


        conn = new Connection(host);
        conn.connect();

        // Authenticate with the SSH server
        boolean isAuthenticated = conn.authenticateWithPassword(user, password);
        if (!isAuthenticated) {
            throw new IOException("Authentication failed.");
        }

        forwarder = conn.createDynamicPortForwarder(1080);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Shutting down gracefully");
        sshThread.interrupt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
