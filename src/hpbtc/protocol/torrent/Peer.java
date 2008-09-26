package hpbtc.protocol.torrent;

import hpbtc.protocol.message.LengthPrefixMessage;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import util.IOUtil;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Peer {

    private SocketChannel channel;
    private byte[] id;
    private boolean messagesReceived;
    private BitSet pieces = new BitSet();
    private boolean peerChoking = true;
    private boolean peerInterested;
    private boolean clientInterested;
    private boolean clientChoking = true;
    private InetSocketAddress address;
    private boolean handshakeReceived;
    private boolean handshakeSent;
    private int uploaded;
    private int downloaded;
    private ByteBuffer data;
    private boolean expectBody;
    private Map<Integer, BitSet> requests = new Hashtable<Integer, BitSet>();
    private AtomicInteger totalRequests = new AtomicInteger();
    private TimerTask keepAliveRead;
    private TimerTask keepAliveWrite;
    private Queue<LengthPrefixMessage> messagesToSend =
            new ConcurrentLinkedQueue<LengthPrefixMessage>();
    private Torrent torrent;

    public Peer(final InetSocketAddress address, final byte[] id) {
        this.address = address;
        this.id = id;
    }

    public Peer(final SocketChannel chn) {
        this.channel = chn;
        this.address = IOUtil.getAddress(chn);
    }

    public void setTorrent(final Torrent torrent) {
        this.torrent = torrent;
    }
    
    public Torrent getTorrent() {
        return torrent;
    }
    
    public boolean cancelKeepAliveWrite() {
        if (keepAliveWrite != null) {
            return keepAliveWrite.cancel();
        } else {
            return true;
        }
    }
    
    public void setKeepAliveWrite(TimerTask keepAlive) {
        this.keepAliveWrite = keepAlive;
    }
    
    public boolean cancelKeepAliveRead() {
        if (keepAliveRead != null) {
            return keepAliveRead.cancel();
        } else {
            return true;
        }
    }
    
    public void setKeepAliveRead(TimerTask keepAlive) {
        this.keepAliveRead = keepAlive;
    }
    
    public int countTotalRequests() {
        return totalRequests.get();
    }
    
    public BitSet getRequests(final int index) {
        return requests.get(index);
    }

    public void addRequest(final int index, final int begin) {
        BitSet bs = requests.get(index);
        if (bs == null) {
            bs = new BitSet(torrent.computeChunksInPiece(index));
            requests.put(index, bs);
        }
        bs.set(TorrentUtil.computeBeginIndex(begin, torrent.getChunkSize()));
        totalRequests.getAndIncrement();
    }

    public void removeRequest(final int index, final int begin,
            final int chunkSize) {
        BitSet bs = requests.get(index);
        if (bs != null) {
            int i = TorrentUtil.computeBeginIndex(begin, chunkSize);
            if (bs.get(i)) {
                bs.clear(i);
                if (bs.isEmpty()) {
                    requests.remove(index);
                }
                totalRequests.getAndDecrement();
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

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(final SocketChannel channel) {
        this.channel = channel;
    }

    public void setId(final byte[] id) {
        this.id = id;
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
        messagesToSend.clear();
        if (torrent != null) {
            torrent.removePeer(this);
        }
        this.torrent = null;
        cancelKeepAliveRead();
        cancelKeepAliveWrite();
    }

    public boolean isMessagesToSendEmpty() {
        return messagesToSend.isEmpty();
    }
    
    public LengthPrefixMessage getMessageToSend() {
        return messagesToSend.poll();
    }
    
    public void addMessageToSend(final LengthPrefixMessage message) {
        messagesToSend.add(message);
    }
    
    public Iterable<LengthPrefixMessage> listMessagesToSend() {
        return messagesToSend;
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
