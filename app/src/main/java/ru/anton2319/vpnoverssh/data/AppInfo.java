package ru.anton2319.vpnoverssh.data;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String humanName;
    private String packageName;
    private Drawable icon;

    public AppInfo(String humanName, String packageName, Drawable icon) {
        this.humanName = humanName;
        this.packageName = packageName;
        this.icon = icon;
    }

    public String getHumanName() {
        return humanName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }
}
