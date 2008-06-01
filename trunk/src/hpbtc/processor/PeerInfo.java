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
    private boolean peerChoking;
    private boolean peerInterested;

    public PeerInfo(InetSocketAddress address, byte[] id) {
        super(address, id);
        peerChoking = true;
    }
    
    public PeerInfo(InetSocketAddress address) {
        super(address);
    }
    
    public void setPeerInterested(boolean interested) {
        peerInterested = interested;
    }
    
    public boolean isPeerInterested() {
        return peerInterested;
    }
    
    public void setPeerChoking(boolean choking) {
        peerChoking = choking;
    }
    
    public boolean isPeerChoking() {
        return peerChoking;
    }
    
    public void setPieces(BitSet bs) {
        pieces = bs;
    }
    
    public void setPiece(int index) {
        pieces.set(index);
    }
    
    public boolean isMessagesReceived() {
        return messagesReceived;
    }

    public void setMessagesReceived() {
        this.messagesReceived = true;
    }
}
