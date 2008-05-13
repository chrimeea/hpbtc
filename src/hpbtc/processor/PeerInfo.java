package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
import java.net.InetSocketAddress;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerInfo extends Peer {
    
    private boolean messagesReceived;

    public PeerInfo(InetSocketAddress address, byte[] id) {
        super(address, id);
    }
    
    public PeerInfo(InetSocketAddress address) {
        super(address);
    }

    public boolean isMessagesReceived() {
        return messagesReceived;
    }

    public void setMessagesReceived(boolean messagesReceived) {
        this.messagesReceived = messagesReceived;
    }
}
