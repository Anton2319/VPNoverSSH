package ru.anton2319.vpnoverssh;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import ru.anton2319.vpnoverssh.data.SSHConnectionProfile;
import ru.anton2319.vpnoverssh.data.singleton.SocksPersistent;
import ru.anton2319.vpnoverssh.data.singleton.StatusInfo;
import ru.anton2319.vpnoverssh.data.utils.SSHConnectionProfileAdapter;
import ru.anton2319.vpnoverssh.data.utils.SSHConnectionProfileManager;
import ru.anton2319.vpnoverssh.services.SocksProxyService;
import ru.anton2319.vpnoverssh.services.SshService;

public class MainActivity extends AppCompatActivity {
    List<SSHConnectionProfile> sshConnectionProfileList;
    SSHConnectionProfile selectedProfile;

    private static final String TAG = "MainActivity";

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

        Context context = this;

        SSHConnectionProfileManager sshConnectionProfileManager = new SSHConnectionProfileManager(this);
        sshConnectionProfileList = sshConnectionProfileManager.loadProfiles();
        SSHConnectionProfileAdapter adapter = new SSHConnectionProfileAdapter(this, sshConnectionProfileList);

        Spinner spinner = findViewById(R.id.spinner);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProfile = SSHConnectionProfile.fromLinkedTreeMap(parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ImageButton editButton = findViewById(R.id.editProfileButton);

        //noinspection Convert2Lambda
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedProfile != null && selectedProfile.uuid != null) {
                    Intent intent = new Intent(context, NewConnectionActivity.class);
                    intent.putExtra("uuid", selectedProfile.uuid.toString());
                    startActivity(intent);
                }
            }
        });

        ImageButton addButton = findViewById(R.id.addProfileButton);

        //noinspection Convert2Lambda
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, NewConnectionActivity.class);
                startActivity(intent);
            }
        });

        Button connectButton = findViewById(R.id.ssh_connect_button);
        //noinspection Convert2Lambda
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedProfile != null) {
                    String username = selectedProfile.getUsername();
                    String password = selectedProfile.getPassword();
                    privateKey = selectedProfile.getPrivateKey();
                    String hostname = selectedProfile.getServerIP();
                    int port = selectedProfile.getServerPort();
                    startVpn(username, password, privateKey, hostname, port);
                }
                else {
                    Intent intent = new Intent(context, NewConnectionActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SSHConnectionProfileManager sshConnectionProfileManager = new SSHConnectionProfileManager(this);
        sshConnectionProfileList = sshConnectionProfileManager.loadProfiles();
        SSHConnectionProfileAdapter adapter = new SSHConnectionProfileAdapter(this, sshConnectionProfileList);
        Spinner spinner = findViewById(R.id.spinner);
        spinner.setAdapter(adapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void startVpn(String username, String password, String privateKey, String hostname, int port) {
        if(!StatusInfo.getInstance().isActive()) {
            Log.d(TAG, "Preparing VpnService");
            Intent intentPrepare = VpnService.prepare(this);
            if (intentPrepare != null) {
                // TODO: replace deprecated method
                StatusInfo.getInstance().setActive(false);
                Log.d(TAG, "Toggled off due to missing permission");
                startActivityForResult(intentPrepare, 0);
                return;
            }
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