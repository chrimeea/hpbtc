package hpbtc.protocol.torrent;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 *
 * @author Cristian Mocanu
 */
public class Peer {

    private InetSocketAddress address;
    private byte[] id;
    
    public Peer(InetSocketAddress address, byte[] id) {
        this.address = address;
        this.id = id;
    }
    
    public Peer(InetSocketAddress address) {
        this(address, null);
    }
    
    public byte[] getId() {
        return id;
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }

    public void setId(byte[] id) {
        this.id = id;
    }
    
    @Override
    public boolean equals(Object arg) {
        if (arg instanceof Peer && arg != null) {
            Peer obj = (Peer) arg;
            return Arrays.equals(id, obj.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new String(id).hashCode();
    }
}
