package ru.anton2319.vpnoverssh;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText hostnameEditText;
    private EditText portEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve references to the UI views
        usernameEditText = findViewById(R.id.ssh_username);
        passwordEditText = findViewById(R.id.ssh_password);
        hostnameEditText = findViewById(R.id.ssh_hostname);
        portEditText = findViewById(R.id.ssh_port);

        // Set a click listener on the Connect button
        Button connectButton = findViewById(R.id.ssh_connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Retrieve the user input from the EditText fields
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String hostname = hostnameEditText.getText().toString();
                //int port = Integer.parseInt(portEditText.getText().toString());

                // Pass the user input to the startVpn() method
                startVpn(username, password, hostname, 1080);
            }
        });
    }

    private void startVpn(String username, String password, String hostname, int port) {
        Intent intentPrepare = VpnService.prepare(this);
        if(intentPrepare != null) {
            startActivityForResult(intentPrepare, 0);
        }
        Intent vpnIntent = new Intent(this, SocksProxyService.class);
        vpnIntent.putExtra("socksHostname", "127.0.0.1");
        vpnIntent.putExtra("socksPort", 1080);
        startService(vpnIntent);
    }
}