package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
import java.net.InetSocketAddress;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerInfo extends Peer {
    
    private boolean messagesReceived;
    private byte[] infoHash;

    public PeerInfo(InetSocketAddress address, byte[] id, byte[] infoHash) {
        super(address, id);
        this.infoHash = infoHash;
    }
    
    public PeerInfo(InetSocketAddress address) {
        super(address);
    }

    public byte[] getInfoHash() {
        return infoHash;
    }
    
    public boolean isMessagesReceived() {
        return messagesReceived;
    }

    public void setMessagesReceived() {
        this.messagesReceived = true;
    }
}
