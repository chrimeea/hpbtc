package hpbtc.protocol.torrent;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.BitSet;

/**
 *
 * @author Cristian Mocanu
 */
public class Peer {

    private InetSocketAddress address;
    private byte[] id;
    private boolean messagesReceived;
    private BitSet pieces;
    private boolean peerChoking;
    private boolean peerInterested;
    private byte[] infoHash;
    
    public Peer(InetSocketAddress address, byte[] id) {
        this.address = address;
        this.id = id;
        peerChoking = true;
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
    
    public void setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }

    public byte[] getInfoHash() {
        return infoHash;
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
