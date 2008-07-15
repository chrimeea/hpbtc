package hpbtc.protocol.torrent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
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
    private boolean peerChoking = true;
    private boolean peerInterested;
    private boolean clientInterested;
    private boolean clientChoking = true;
    private byte[] infoHash;
    private InetSocketAddress address;
    private boolean handshakeReceived;
    private AtomicInteger uploaded = new AtomicInteger();
    private AtomicInteger downloaded = new AtomicInteger();
    
    public Peer(InetSocketAddress address, byte[] id) {
        this.address = address;
        this.id = id;
    }
    
    public Peer(SocketChannel channel) {
        this.channel = channel;
        this.address = IOUtil.getAddress(channel);
    }

    public int countUploaded() {
        return uploaded.get();
    }
    
    public int countDownloaded() {
        return downloaded.get();
    }
    
    public int upload(ByteBuffer bb) throws IOException {
        int i = IOUtil.writeToChannel(channel, bb);
        uploaded.addAndGet(i);
        return i;
    }
    
    public int download(ByteBuffer bb) throws IOException {
        int i = IOUtil.readFromChannel(channel, bb);
        downloaded.addAndGet(i);
        return i;
    }
    
    public boolean isClientChoking() {
        return clientChoking;
    }

    public void setClientChoking(boolean clientChoking) {
        this.clientChoking = clientChoking;
    }

    public void setClientInterested(boolean clientInterested) {
        this.clientInterested = clientInterested;
    }

    public boolean isClientInterested() {
        return clientInterested;
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
    
    public BitSet getOtherPieces(BitSet bs) {
        if (pieces == null) {
            pieces = new BitSet(bs.size());
            return pieces;
        } else {
            BitSet c = (BitSet) pieces.clone();
            c.andNot(bs);
            return c;
        }
    }

    void resetCounters() {
        uploaded.set(0);
        downloaded.set(0);
    }
}
