package hpbtc.client;

import java.net.InetSocketAddress;

/**
 *
 * @author Cristian Mocanu
 */
public class Peer {

    private InetSocketAddress address;
    private String id;
    
    public Peer(InetSocketAddress address, String id) {
        this.address = address;
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }
}
