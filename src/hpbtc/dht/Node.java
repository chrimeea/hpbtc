/*
 * Created on 18.10.2008
 */

package hpbtc.dht;

import hpbtc.protocol.torrent.Peer;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class Node {

    public enum Status {GOOD, UNKNOWN, BAD};
    
    private byte[] nodeID;
    private Status status;
    private Peer peer;
    private long lastSeen = System.currentTimeMillis();

    public Node(byte[] nodeID) {
        this.nodeID = nodeID;
    }
    
    public byte[] getNodeID() {
        return nodeID;
    }

    public Peer getPeer() {
        return peer;
    }

    public Status getStatus() {
        return status;
    }
}
