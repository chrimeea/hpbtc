package hpbtc.protocol.torrent;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import util.IOUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Peer {

    private SocketChannel channel;
    private byte[] id;
    private boolean messagesReceived;
    private BitSet pieces;
    private boolean peerChoking;
    private boolean peerInterested;
    private byte[] infoHash;
    private InetSocketAddress address;
    private boolean handshakeReceived;
    
    public Peer(InetSocketAddress address, byte[] id) {
        this.address = address;
        this.id = id;
        peerChoking = true;
    }
    
    public Peer(SocketChannel channel) {
        this.channel = channel;
        this.address = IOUtil.getAddress(channel);
        peerChoking = true;
    }

    public boolean isHandshakeReceived() {
        return handshakeReceived;
    }

    public void setHandshakeReceived() {
        this.handshakeReceived = true;
    }
    
    public byte[] getId() {
        return id;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
    
    public boolean isConnected() {
        return channel != null;
    }
    
    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }
    
    public void setId(byte[] id) {
        this.id = id;
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
    
    public BitSet getPieces() {
        return pieces;
    }
    
    public boolean hasOtherPieces(BitSet bs) {
        BitSet c = (BitSet) pieces.clone();
        c.andNot(bs);
        return !c.isEmpty();
    }
}
