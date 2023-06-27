package ru.anton2319.vpnoverssh;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SSHConnectionProfileManager {
    private static final String TAG = "SSHConnectionProfileManager";
    private static final String FILE_NAME = "ssh_profiles.json";

    private Context context;

    public SSHConnectionProfileManager(Context context) {
        this.context = context;
    }

    public void saveProfiles(List<SSHConnectionProfile> profiles) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(profiles);

        try {
            FileOutputStream fileOutputStream = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fileOutputStream.write(json.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error saving SSH connection profiles: " + e.getMessage());
        }
    }

    public void saveProfile(SSHConnectionProfile profile) {
        List<SSHConnectionProfile> existingProfiles = loadProfiles();
        existingProfiles.add(profile);

        saveProfiles(existingProfiles);
    }

    public List<SSHConnectionProfile> loadProfiles() {
        List<SSHConnectionProfile> profiles = new ArrayList<>();

        try {
            FileInputStream fileInputStream = context.openFileInput(FILE_NAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String json = stringBuilder.toString();

            Gson gson = new Gson();
            profiles = gson.fromJson(json, ArrayList.class);
        } catch (IOException e) {
            Log.e(TAG, "Error loading SSH connection profiles: " + e.getMessage());
        }

        return profiles;
    }
}
