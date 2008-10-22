/*
 * Created on 18.10.2008
 */

package hpbtc.protocol.dht;

import hpbtc.protocol.torrent.Peer;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DHTNode {

    public enum Status {GOOD, UNKNOWN, BAD};
    
    private Status status;
    private Peer peer;
    private long lastSeen = System.currentTimeMillis();
    private byte[] id;

    public Peer getPeer() {
        return peer;
    }

    public Status getStatus() {
        return status;
    }

    public byte[] getId() {
        return id;
    }
}
