package ru.anton2319.vpnoverssh;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// whoever reads this... I'm sorry
// In case you encountered any bugs here report them to issues, don't spend your precious time
// If for some reason you are debugging this:
// Use debugger, and everything will suddenly make sense

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
        List<LinkedTreeMap> existingProfiles = loadProfilesAsLinkedTreeMap();
        boolean profileExists = false;

        for (int i = 0; i < existingProfiles.size(); i++) {
            if (existingProfiles.get(i).get("uuid").equals(profile.uuid.toString())) {
                existingProfiles.set(i, new Gson().fromJson(new Gson().toJson(profile), LinkedTreeMap.class));
                profileExists = true;
                break;
            }
        }

        if (!profileExists) {
            existingProfiles.add(new Gson().fromJson(new Gson().toJson(profile), LinkedTreeMap.class));
        }

        saveProfiles(new Gson().fromJson(new Gson().toJson(existingProfiles), TypeToken.getParameterized(List.class, SSHConnectionProfile.class).getType()));
    }

    public SSHConnectionProfile loadProfileByUUID(UUID uuid) {
        List<SSHConnectionProfile> profiles = new Gson().fromJson(new Gson().toJson(loadProfiles()), TypeToken.getParameterized(List.class, SSHConnectionProfile.class).getType());

        for (SSHConnectionProfile profile : profiles) {
            if (profile.uuid.equals(uuid)) {
                return profile;
            }
        }

        return null;
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

    public List<LinkedTreeMap> loadProfilesAsLinkedTreeMap() {
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
            profiles = gson.fromJson(json, TypeToken.getParameterized(List.class, SSHConnectionProfile.class).getType());
        } catch (IOException e) {
            Log.e(TAG, "Error loading SSH connection profiles: " + e.getMessage());
        }

        return new Gson().fromJson(new Gson().toJson(profiles), TypeToken.getParameterized(List.class, LinkedTreeMap.class).getType());
    }
}
