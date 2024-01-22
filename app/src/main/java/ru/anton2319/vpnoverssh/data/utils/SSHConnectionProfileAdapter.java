package ru.anton2319.vpnoverssh.data.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import ru.anton2319.vpnoverssh.data.SSHConnectionProfile;

public class SSHConnectionProfileAdapter extends ArrayAdapter<SSHConnectionProfile> {
    private LayoutInflater inflater;

    public SSHConnectionProfileAdapter(Context context, List<SSHConnectionProfile> profiles) {
        super(context, 0, profiles);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        SSHConnectionProfile profile = SSHConnectionProfile.fromLinkedTreeMap(getItem(position));

        if (profile != null) {
            String displayText = profile.getUsername() + "@" + profile.getServerIP() + ":" + profile.getServerPort();
            textView.setText(displayText);
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        SSHConnectionProfile profile = SSHConnectionProfile.fromLinkedTreeMap(getItem(position));

        if (profile != null) {
            String displayText = profile.getUsername() + "@" + profile.getServerIP() + ":" + profile.getServerPort();
            textView.setText(displayText);
        }

        return convertView;
    }
}