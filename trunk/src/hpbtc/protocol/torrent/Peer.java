package hpbtc.protocol.torrent;

import hpbtc.protocol.message.BlockMessage;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private Queue<BlockMessage> requests =
            new ConcurrentLinkedQueue<BlockMessage>();

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

    public int countRequests(int index) {
        int k = 0;
        for (BlockMessage bm: requests) {
            if (bm.getIndex() == index) {
                k++;
            }
        }
        return k;
    }
    
    public int getFirstFreeBegin(int index, int chunks, int chunkSize) {
        BitSet bs = new BitSet(chunks);
        for (BlockMessage bm: requests) {
            bs.set(TorrentUtil.computeBeginIndex(bm.getBegin(), chunkSize));
        }
        return bs.nextClearBit(0);
    }
    
    public void addRequest(BlockMessage bm) {
        requests.add(bm);
    }

    public void removeRequest(int index, int begin, int length) {
        Iterator<BlockMessage> i = requests.iterator();
        while (i.hasNext()) {
            BlockMessage message = i.next();
            if (message.getIndex() == index &&
                    message.getBegin() == begin &&
                    message.getLength() == length) {
                i.remove();
            }
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
    }

    void resetCounters() {
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
