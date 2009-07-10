/*
 * Created on 18.10.2008
 */

package hpbtc.protocol.dht;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DHTNode {

    public enum Status {GOOD, UNKNOWN, BAD};

    private long lastSeen;
    private Status status;
    private InetSocketAddress peer;
    private byte[] id;
    private Timer timer;
    private TimerTask refreshTask;

    public DHTNode(final byte[] id, final InetSocketAddress peer,
            final Timer timer) {
        this.status = Status.GOOD;
        this.id = id;
        this.peer = peer;
        updateLastSeen();
    }

    public synchronized Status getStatus() {
        return status;
    }

    public byte[] getId() {
        return id;
    }

    public String getCompactNodeInfo() {
        byte[] compact = Arrays.copyOf(id, 26);
        byte[] ip = peer.getAddress().getAddress();
        compact[20] = ip[0];
        compact[21] = ip[1];
        compact[22] = ip[2];
        compact[23] = ip[3];
        int port = peer.getPort();
        compact[24] = (byte) (port % 256);
        compact[25] = (byte) ((port / 256) % 256);
        return new String(compact);
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        refreshTask = new TimerTask() {

                @Override
                public void run() {
                    setStatus(Status.UNKNOWN);
        }};
        timer.schedule(refreshTask, 900000L);
    }

    public synchronized void setStatus(final Status status) {
        this.status = status;
    }
}
