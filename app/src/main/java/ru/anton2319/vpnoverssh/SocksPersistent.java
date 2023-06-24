package ru.anton2319.vpnoverssh;

import android.os.ParcelFileDescriptor;

public class SocksPersistent {
    private static volatile SocksPersistent instance = null;
    private static final Object lock = new Object();

    private Thread vpnThread;

    ParcelFileDescriptor vpnInterface;

    private SocksPersistent() {}

    public static SocksPersistent getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new SocksPersistent();
                }
            }
        }
        return instance;
    }

    public void setVpnThread(Thread vpnThread) {
        this.vpnThread = vpnThread;
    }

    public Thread getVpnThread() {
        return vpnThread;
    }

    public ParcelFileDescriptor getVpnInterface() {
        return vpnInterface;
    }

    public void setVpnInterface(ParcelFileDescriptor vpnInterface) {
        this.vpnInterface = vpnInterface;
    }
}
