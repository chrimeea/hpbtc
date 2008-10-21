/*
 * Created on 18.10.2008
 */

package hpbtc.protocol.dht;

import hpbtc.protocol.network.NetworkComponent;
import hpbtc.protocol.torrent.Peer;
import java.net.SocketAddress;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class Node extends NetworkComponent {

    public enum Status {GOOD, UNKNOWN, BAD};
    
    private Status status;
    private Peer peer;
    private long lastSeen = System.currentTimeMillis();

    public Node(final SocketAddress address) {
        super(address);
    }

    public Peer getPeer() {
        return peer;
    }

    public Status getStatus() {
        return status;
    }
}
