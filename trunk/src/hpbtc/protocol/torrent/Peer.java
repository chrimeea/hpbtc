package hpbtc.protocol.torrent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import util.IOUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Peer {

    private ByteChannel channel;
    private byte[] id;
    private boolean messagesReceived;
    private BitSet pieces = new BitSet();
    private boolean peerChoking = true;
    private boolean peerInterested;
    private boolean clientInterested;
    private boolean clientChoking = true;
    private byte[] infoHash;
    private InetSocketAddress address;
    private boolean handshakeReceived;
    private boolean handshakeSent;
    private int uploaded;
    private int downloaded;
    private ByteBuffer data;

    public Peer(final InetSocketAddress address, final byte[] infoHash,
            final byte[] id) {
        this.address = address;
        this.id = id;
        this.infoHash = infoHash;
    }

    public Peer(final SocketChannel chn) {
        this.channel = chn;
        this.address = IOUtil.getAddress(chn);
    }

    public boolean isHandshakeSent() {
        return handshakeSent;
    }
    
    public void setHandshakeSent() {
        handshakeSent = true;
    }
    
    public int countUploaded() {
        return uploaded;
    }

    public int countDownloaded() {
        return downloaded;
    }

    public int upload(final ByteBuffer bb) throws IOException {
        int i = IOUtil.writeToChannel(channel, bb);
        uploaded += i;
        return i;
    }
    
    public void setNextDataExpectation(final int i) {
        if (data == null || data.capacity() < i) {
            data = ByteBuffer.allocate(i);
        }
    }
    
    public boolean download() throws IOException {
        int i = IOUtil.readFromChannel(channel, data);
        if (i > 0) {
            downloaded += i;
        } else {
            throw new IOException();
        }
        return !data.hasRemaining();
    }
    
    public ByteBuffer getData() {
        data.rewind();
        ByteBuffer result = data;
        data = null;
        return result;
    }
    
    public boolean isClientChoking() {
        return clientChoking;
    }

    public void setClientChoking(final boolean clientChoking) {
        this.clientChoking = clientChoking;
    }

    public void setClientInterested(final boolean clientInterested) {
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
        return channel != null && handshakeReceived;
    }

    public ByteChannel getChannel() {
        return channel;
    }

    public void setChannel(final ByteChannel channel) {
        this.channel = channel;
    }

    public void setId(final byte[] id) {
        this.id = id;
    }

    public void setInfoHash(final byte[] infoHash) {
        this.infoHash = infoHash;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public void setPeerInterested(final boolean interested) {
        peerInterested = interested;
    }

    public boolean isPeerInterested() {
        return peerInterested;
    }

    public void setPeerChoking(final boolean choking) {
        peerChoking = choking;
    }

    public boolean isPeerChoking() {
        return peerChoking;
    }

    public void setPieces(final BitSet bs) {
        pieces = bs;
    }

    public void setPiece(final int index) {
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

    public BitSet getOtherPieces(final BitSet bs) {
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
        uploaded = 0;
        downloaded = 0;
    }

    @Override
    public String toString() {
        return address.toString();
    }
}
