package ru.anton2319.vpnoverssh.data.singleton;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.DynamicPortForwarder;

public class PortForward {
    private static volatile PortForward instance = null;
    private static final Object lock = new Object();

    private Thread sshThread;
    private Connection conn;
    private DynamicPortForwarder forwarder;


    private PortForward() {}

    public static PortForward getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new PortForward();
                }
            }
        }
        return instance;
    }

    public void setSshThread(Thread sshThread) {
        this.sshThread = sshThread;
    }

    public Thread getSshThread() {
        return sshThread;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public Connection getConn() {
        return conn;
    }

    public void setForwarder(DynamicPortForwarder forwarder) {
        this.forwarder = forwarder;
    }

    public DynamicPortForwarder getForwarder() {
        return forwarder;
    }
}
