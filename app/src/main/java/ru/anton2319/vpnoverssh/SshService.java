package ru.anton2319.vpnoverssh;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.DynamicPortForwarder;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;

public class SshService extends Service {

    private static final String TAG = "SshService";
    Thread sshThread;
    Connection conn;
    DynamicPortForwarder forwarder;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the SSH tunneling code
        sshThread = PortForward.getInstance().getSshThread();
        if(sshThread == null) {
            sshThread = newSshThread(intent);
            PortForward.getInstance().setSshThread(sshThread);
        }
        else {
            sshThread.interrupt();
            sshThread = newSshThread(intent);
            PortForward.getInstance().setSshThread(sshThread);
        }
        sshThread.start();

        return START_STICKY;
    }

    public void initiateSSH(Intent intent) throws IOException, RuntimeException {
        Log.d(TAG, "Starting trilead.ssh2 service");

        String user = intent.getStringExtra("user");
        String host = intent.getStringExtra("hostname");
        String password = intent.getStringExtra("password");
        int port = Integer.parseInt(intent.getStringExtra("port"));
        String privateKey = intent.getStringExtra("privateKey");
        String privateKeyFilePath = File.createTempFile("vpnoverssh", null, null).getPath();

        conn = new Connection(host, port);
        conn.connect();
        PortForward.getInstance().setConn(conn);

        // Authenticate with the SSH server

        boolean isAuthenticated;


        if(privateKey != null) {
            isAuthenticated = conn.authenticateWithPublicKey(user, privateKey.toCharArray(), "");
            if (!isAuthenticated) {
                isAuthenticated = conn.authenticateWithPassword(user, password);
            }
            if (!isAuthenticated) {
                throw new RuntimeException("Cannot authenticate with the provided credentials");
            }
        } else {
            isAuthenticated = conn.authenticateWithPassword(user, password);
            if (!isAuthenticated) {
                throw new RuntimeException("Cannot authenticate with the provided credentials");
            }
        }

        forwarder = conn.createDynamicPortForwarder(1080);
        PortForward.getInstance().setForwarder(forwarder);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Shutting down gracefully");
        sshThread = PortForward.getInstance().getSshThread();
        if(sshThread != null) {
            sshThread.interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public Thread newSshThread(Intent intent) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.setProperty("user.home", getFilesDir().getAbsolutePath());
                    initiateSSH(intent);
                    while(true) {
                        if(Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    e.printStackTrace();
                    Log.d(TAG, "trilead.ssh2 failed, and there is no fallback yet ¯\\_(ツ)_/¯");
                } catch (InterruptedException e) {
                    conn = PortForward.getInstance().getConn();
                    forwarder = PortForward.getInstance().getForwarder();
                    conn.close();
                    forwarder.close();
                }
            }
        });
    }
}
