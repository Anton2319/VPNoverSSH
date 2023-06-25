package ru.anton2319.vpnoverssh;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int READ_REQUEST_CODE = 42;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText hostnameEditText;
    private EditText portEditText;

    private String privateKey;

    Intent vpnIntent;

    Intent sshIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(StatusInfo.getInstance().getVpnIntent() == null) {
            StatusInfo.getInstance().setVpnIntent(new Intent(this, SocksProxyService.class));
        }
        if(StatusInfo.getInstance().getSshIntent() == null) {
            StatusInfo.getInstance().setSshIntent(new Intent(this, SshService.class));
        }
        vpnIntent = StatusInfo.getInstance().getVpnIntent();
        sshIntent = StatusInfo.getInstance().getSshIntent();
        setContentView(R.layout.activity_main);
        usernameEditText = findViewById(R.id.ssh_username);
        passwordEditText = findViewById(R.id.ssh_password);
        hostnameEditText = findViewById(R.id.ssh_hostname);
        portEditText = findViewById(R.id.ssh_port);

        Button connectButton = findViewById(R.id.ssh_connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String hostname = hostnameEditText.getText().toString();
                int port = 22;
                try {
                    port = Integer.parseInt(portEditText.getText().toString());
                }
                catch (Exception e) {}
                startVpn(username, password, privateKey, hostname, port);
            }
        });
    }

    public void pickFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                privateKey = readTextFromUri(uri);
            }
        }
    }

    private String readTextFromUri(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private void startVpn(String username, String password, String privateKey, String hostname, int port) {
        if(!StatusInfo.getInstance().isActive()) {
            Log.d(TAG, "Toggled on");
            StatusInfo.getInstance().setActive(true);
            try {
                Log.d(TAG, "Setting up port forward");
                sshIntent.putExtra("user", username);
                sshIntent.putExtra("password", password);
                if (privateKey != null && !privateKey.equals("")) {
                    sshIntent.putExtra("privateKey", privateKey);
                }
                sshIntent.putExtra("hostname", hostname);
                if(!String.valueOf(port).equals("") || between(port, 1 , 65535)) {
                    sshIntent.putExtra("port", String.valueOf(port));
                }
                else {
                    sshIntent.putExtra("port", String.valueOf(22));
                }
                startService(sshIntent);

                Log.d(TAG, "Starting proxy");
                Intent intentPrepare = VpnService.prepare(this);
                if (intentPrepare != null) {
                    startActivityForResult(intentPrepare, 0);
                }
                vpnIntent.putExtra("socksPort", 1080);
                startService(vpnIntent);


                StatusInfo.getInstance().setActive(true);
            } catch (Exception e) {
                StatusInfo.getInstance().setActive(false);
                e.printStackTrace();
            }
        }
        else {
            Log.d(TAG, "Toggled off");
            StatusInfo.getInstance().setActive(false);
            Thread vpnThread = SocksPersistent.getInstance().getVpnThread();
            if(vpnThread != null) {
                vpnThread.interrupt();
            }
            sshIntent = StatusInfo.getInstance().getSshIntent();
            stopService(sshIntent);
        }
    }
    private static boolean between(int variable, int minValueInclusive, int maxValueInclusive) {
        return variable >= minValueInclusive && variable <= maxValueInclusive;
    }
}