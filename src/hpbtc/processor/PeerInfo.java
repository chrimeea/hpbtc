package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
import java.net.InetSocketAddress;
import java.util.BitSet;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerInfo extends Peer {
    
    private boolean messagesReceived;
    private BitSet pieces;

    public PeerInfo(InetSocketAddress address, byte[] id) {
        super(address, id);
    }
    
    public PeerInfo(InetSocketAddress address) {
        super(address);
    }
    
    public void setPieces(BitSet bs) {
        pieces = bs;
    }
    
    public boolean isMessagesReceived() {
        return messagesReceived;
    }

    public void setMessagesReceived() {
        this.messagesReceived = true;
    }
}
