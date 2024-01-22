package ru.anton2319.vpnoverssh.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import ru.anton2319.vpnoverssh.data.AppInfo;

public class AppInfoExtractor {

    private PackageManager packageManager;

    public AppInfoExtractor(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    public List<AppInfo> getAllInstalledApps() {
        List<AppInfo> appList = new ArrayList<>();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            String humanName = packageInfo.loadLabel(packageManager).toString();
            String packageName = packageInfo.packageName;
            Drawable icon = packageManager.getApplicationIcon(packageInfo);
            appList.add(new AppInfo(humanName, packageName, icon));
        }

        return appList;
    }
}