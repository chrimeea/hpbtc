/*
 * Created on 18.10.2008
 */

package hpbtc.protocol.dht;

import hpbtc.protocol.torrent.Peer;
import java.util.BitSet;
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
    private Peer peer;
    private byte[] id;
    private Timer timer;
    private TimerTask refreshTask;

    public DHTNode(final byte[] id, final Peer peer, final Timer timer) {
        this.status = Status.GOOD;
        this.id = id;
        this.peer = peer;
        updateLastSeen();
    }

    public Peer getPeer() {
        return peer;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public byte[] getId() {
        return id;
    }

    public String getCompactNodeInfo() {
        //TODO: implement
        return null;
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
        timer.schedule(refreshTask, 900000);
    }

    public synchronized void setStatus(final Status status) {
        this.status = status;
    }
}
