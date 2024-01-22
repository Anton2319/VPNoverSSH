package ru.anton2319.vpnoverssh.data.singleton;

import android.content.Intent;

public class StatusInfo {
    private static volatile StatusInfo instance = null;
    private static final Object lock = new Object();

    private boolean active = false;

    private Intent vpnIntent;

    private Intent sshIntent;

    private StatusInfo() {
        this.active = false;
    }

    public static StatusInfo getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new StatusInfo();
                }
            }
        }
        return instance;
    }

    public synchronized boolean isActive() {
        return this.active;
    }

    public synchronized void setActive(Boolean active) {
        this.active = active;
    }

    public Intent getVpnIntent() {
        return vpnIntent;
    }

    public void setVpnIntent(Intent vpnIntent) {
        this.vpnIntent = vpnIntent;
    }

    public Intent getSshIntent() {
        return sshIntent;
    }

    public void setSshIntent(Intent sshIntent) {
        this.sshIntent = sshIntent;
    }
}
