package ru.anton2319.vpnoverssh.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.UUID;

public class SSHConnectionProfile {
    private String serverIP;
    private int serverPort;
    private String username;
    private AuthenticationType authenticationType;

    private String password;

    private String privateKey;

    public enum AuthenticationType {
        PASSWORD,
        PRIVATE_KEY
    }

    public UUID uuid = UUID.randomUUID();

    public SSHConnectionProfile() {}

    public SSHConnectionProfile(String serverIP, int serverPort, String username, AuthenticationType authenticationType) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.username = username;
        this.authenticationType = authenticationType;
    }


    // Avoid compilation checks for types (GSON converts types from SSHConnectionProfile to LinkedTreeMap)
    public static SSHConnectionProfile fromLinkedTreeMap(Object linkedTreeMap) {
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(linkedTreeMap);
        return gson.fromJson(json, SSHConnectionProfile.class);
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(AuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPassword() {
        return password;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}
