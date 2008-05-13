package hpbtc.protocol.torrent;

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
    
    public Peer(InetSocketAddress address) {
        this(address, null);
    }
    
    public String getId() {
        return id;
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public boolean equals(Object arg) {
        if (arg instanceof Peer && arg != null) {
            Peer obj = (Peer) arg;
            if (id.equals(obj.id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
