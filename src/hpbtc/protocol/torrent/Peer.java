package hpbtc.protocol.torrent;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import util.IOUtil;
import util.TorrentUtil;

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
    private boolean expectBody;
    private Map<Integer, BitSet> requests = new Hashtable<Integer, BitSet>();
    private AtomicInteger totalRequests;
    private TimerTask keepAlive;

    public Peer(final InetSocketAddress address, final byte[] infoHash,
            final byte[] id) {
        this.address = address;
        this.id = id;
        this.infoHash = infoHash;
        this.totalRequests = new AtomicInteger();
    }

    public Peer(final SocketChannel chn) {
        this.channel = chn;
        this.address = IOUtil.getAddress(chn);
    }

    public void cancelKeepAlive() {
        if (keepAlive != null) {
            keepAlive.cancel();
        }
    }
    
    public void setKeepAlive(TimerTask keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public int countTotalRequests() {
        return totalRequests.get();
    }
    
    public BitSet getRequests(final int index) {
        return requests.get(index);
    }

    public void addRequest(final int index, final int begin,
            final int chunkSize, final int chunks) {
        BitSet bs = requests.get(index);
        if (bs == null) {
            bs = new BitSet(chunks);
            requests.put(index, bs);
        }
        bs.set(TorrentUtil.computeBeginIndex(begin, chunkSize));
        totalRequests.getAndIncrement();
    }

    public void removeRequest(final int index, final int begin,
            final int chunkSize) {
        BitSet bs = requests.get(index);
        if (bs != null) {
            bs.clear(TorrentUtil.computeBeginIndex(begin, chunkSize));
            if (bs.isEmpty()) {
                requests.remove(index);
            }
            totalRequests.getAndDecrement();
        }
    }

    public void setExpectBody(boolean expectBody) {
        this.expectBody = expectBody;
    }

    public boolean isExpectBody() {
        return expectBody;
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
        if (i >= 0) {
            downloaded += i;
        } else {
            throw new EOFException();
        }
        return !data.hasRemaining();
    }

    public ByteBuffer getData() {
        ByteBuffer result = data;
        data = null;
        result.rewind();
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
        return handshakeReceived;
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
        requests.clear();
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

    public void disconnect() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        requests.clear();
        cancelKeepAlive();
    }

    public void resetCounters() {
        uploaded = 0;
        downloaded = 0;
    }

    @Override
    public String toString() {
        return address.toString();
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Peer other = (Peer) obj;
            return address.equals(other.address);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }
}
