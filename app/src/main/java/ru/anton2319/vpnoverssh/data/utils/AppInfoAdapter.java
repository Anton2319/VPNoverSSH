package ru.anton2319.vpnoverssh.data.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.anton2319.vpnoverssh.R;
import ru.anton2319.vpnoverssh.data.AppInfo;

public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.ViewHolder> {

    private List<AppInfo> appInfoList;
    private Set<String> selectedApps = new HashSet<>();

    public AppInfoAdapter(List<AppInfo> appInfoList) {
        this.appInfoList = appInfoList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo appInfo = appInfoList.get(position);
        holder.humanName.setText(appInfo.getHumanName());
        holder.appName.setText(appInfo.getPackageName());
        holder.appIcon.setImageDrawable(appInfo.getIcon());

        holder.itemView.setActivated(selectedApps.contains(appInfo.getPackageName()));
        holder.itemView.setOnClickListener(v -> {
            if (selectedApps.contains(appInfo.getPackageName())) {
                selectedApps.remove(appInfo.getPackageName());
            } else {
                selectedApps.add(appInfo.getPackageName());
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView humanName;
        TextView appName;
        ImageView appIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            humanName = itemView.findViewById(R.id.appName); // ID of TextView in item_app_info.xml
            appName = itemView.findViewById(R.id.packageName); // ID of TextView in item_app_info.xml
            appIcon = itemView.findViewById(R.id.appIcon); // ID of ImageView in item_app_info.xml
        }
    }

    public Set<String> getSelectedApps() {
        return selectedApps;
    }

    public void setSelectedApps(Set<String> selectedApps) {
        this.selectedApps = selectedApps;
    }
}