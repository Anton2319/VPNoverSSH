package ru.anton2319.vpnoverssh;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import ru.anton2319.vpnoverssh.data.SSHConnectionProfile;
import ru.anton2319.vpnoverssh.data.utils.SSHConnectionProfileManager;

public class NewConnectionActivity extends AppCompatActivity {
    View passwordInputLayout;
    TextInputEditText serverAddressInput, serverPortInput, usernameInput, passwordInput;
    View addKeyButton, keyCard;
    FloatingActionButton saveButton;
    SSHConnectionProfile.AuthenticationType authenticationType = SSHConnectionProfile.AuthenticationType.PASSWORD;
    String privateKey;
    TextView keyInfoTextView;
    private static final int READ_REQUEST_CODE = 42;

    SSHConnectionProfile sshConnectionProfile = new SSHConnectionProfile();
    SSHConnectionProfileManager sshConnectionProfileManager = new SSHConnectionProfileManager(this);
    UUID profile_uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().getStringExtra("uuid") != null) {
            profile_uuid = UUID.fromString(getIntent().getStringExtra("uuid"));
        }

        setContentView(R.layout.activity_new_connection);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        serverAddressInput = findViewById(R.id.serverAddressInput);
        serverPortInput = findViewById(R.id.serverPortInput);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        addKeyButton = findViewById(R.id.addKeyButton);
        addKeyButton.setVisibility(View.GONE);
        keyCard = findViewById(R.id.keyCard);
        saveButton = findViewById(R.id.floatingActionButton);

        AutoCompleteTextView authenticationTypeDropdown = findViewById(R.id.authenticationTypeInput);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.authentication_type_array, android.R.layout.simple_dropdown_item_1line);
        authenticationTypeDropdown.setAdapter(adapter);

        if(profile_uuid != null) {
            sshConnectionProfile = sshConnectionProfileManager.loadProfileByUUID(profile_uuid);
            serverAddressInput.setText(sshConnectionProfile.getServerIP());
            serverPortInput.setText(String.valueOf(sshConnectionProfile.getServerPort()));
            usernameInput.setText(sshConnectionProfile.getUsername());
            passwordInput.setText(sshConnectionProfile.getPassword());
            if(sshConnectionProfile.getAuthenticationType() == SSHConnectionProfile.AuthenticationType.PRIVATE_KEY) {
                authenticationType = SSHConnectionProfile.AuthenticationType.PRIVATE_KEY;
                passwordInputLayout.setVisibility(View.GONE);
                addKeyButton.setVisibility(View.VISIBLE);
                privateKey = sshConnectionProfile.getPrivateKey();
                // TODO: add resource links
                authenticationTypeDropdown.setText("Private Key", false);
                updateKeyCardData();
            }
            else {
                authenticationTypeDropdown.setText("Password", false);
            }
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sshConnectionProfile.setAuthenticationType(authenticationType);
                sshConnectionProfile.setServerIP(((TextView) serverAddressInput).getText().toString());
                sshConnectionProfile.setServerPort(Integer.parseInt(((TextView) serverPortInput).getText().toString()));
                sshConnectionProfile.setUsername(((TextView) usernameInput).getText().toString());
                if (authenticationType == SSHConnectionProfile.AuthenticationType.PASSWORD) {
                    sshConnectionProfile.setPassword(((TextView) passwordInput).getText().toString());
                }
                else if (authenticationType == SSHConnectionProfile.AuthenticationType.PRIVATE_KEY) {
                    sshConnectionProfile.setPrivateKey(privateKey);
                }
                sshConnectionProfileManager.saveProfile(sshConnectionProfile);
                finish();
            }
        });

        addKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFile();
            }
        });

        //noinspection Convert2Lambda
        authenticationTypeDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // Password selected
                    authenticationType = SSHConnectionProfile.AuthenticationType.PASSWORD;
                    passwordInputLayout.setVisibility(View.VISIBLE);
                    addKeyButton.setVisibility(View.GONE);
                    keyCard.setVisibility(View.GONE);
                } else {
                    // Private Key selected
                    authenticationType = SSHConnectionProfile.AuthenticationType.PRIVATE_KEY;
                    passwordInputLayout.setVisibility(View.GONE);
                    addKeyButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // TODO: replace deprecated method
        startActivityForResult(intent, READ_REQUEST_CODE);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                privateKey = readTextFromUri(uri);
                updateKeyCardData();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void updateKeyCardData() {
        String privateKeyString = privateKey;
        privateKeyString = privateKeyString.replace("-----BEGIN OPENSSH PRIVATE KEY-----", "")
                .replace("-----END OPENSSH PRIVATE KEY-----", "");
        privateKeyString = privateKeyString.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");
        String privatekeyPreview = "";
        keyInfoTextView = findViewById(R.id.keyInfoTextView);
        if(privateKeyString.length() > 25) {
            privatekeyPreview = privateKeyString.substring(0, 25) + "•••••" + "\n";
            keyInfoTextView.setText(privatekeyPreview);
        }
        if(privateKey.length() > 55) {
            privatekeyPreview = privatekeyPreview + "•••••" +privateKeyString.substring(privateKeyString.length() - 26, privateKeyString.length() - 1);
            keyInfoTextView.setText(privatekeyPreview);
        }
        keyCard.setVisibility(View.VISIBLE);

        MaterialButton deleteKeyButton = findViewById(R.id.deleteKeyButton);

        //noinspection Convert2Lambda
        deleteKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyInfoTextView.setText("");
                privateKey = null;
                keyCard.setVisibility(View.GONE);
            }
        });
    }
}