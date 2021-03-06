package hpbtc.protocol.torrent;

import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.message.PieceMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import hpbtc.util.TorrentUtil;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Peer {

    private static Logger logger = Logger.getLogger(Peer.class.getName());
    private ByteBuffer data;
    private int uploaded;
    private ByteBuffer currentWrite;
    private int downloaded;
    private boolean messagesReceived;
    private BitSet pieces = new BitSet();
    private boolean peerChoking = true;
    private boolean peerInterested;
    private boolean clientInterested;
    private boolean clientChoking = true;
    private boolean handshakeReceived;
    private boolean handshakeSent;
    private boolean expectBody;
    private BitSet[] requests;
    private AtomicInteger totalRequests = new AtomicInteger();
    private TimerTask keepAliveRead;
    private TimerTask keepAliveWrite;
    private TimerTask uploadLimitTask;
    private final Queue<LengthPrefixMessage> messagesToSend =
            new ConcurrentLinkedQueue<LengthPrefixMessage>();
    private AtomicReference<Torrent> torrent = new AtomicReference<Torrent>();
    private ByteChannel channel;
    private SocketAddress address;
    private byte[] id;
    private AtomicBoolean valid = new AtomicBoolean(true);
    private AtomicBoolean isReading = new AtomicBoolean(false);
    private AtomicBoolean isWriting = new AtomicBoolean(false);

    public Peer(final SocketAddress address) {
        this.address = address;
    }

    public boolean isValid() {
        return valid.get();
    }

    public boolean isReading() {
        return isReading.get();
    }

    public boolean isWriting() {
        return isWriting.get();
    }

    public void setReading(boolean reading) {
        isReading.set(reading);
    }

    public void setWriting(boolean writing) {
        isWriting.set(writing);
    }

    public void incrementUploaded(int i) throws InvalidPeerException {
        uploaded += i;
        getTorrent().incrementUploaded(i);
    }

    public TimerTask getUploadLimitTask() {
        return uploadLimitTask;
    }

    public void setUploadLimitTask(TimerTask uploadLimitTask) {
        this.uploadLimitTask = uploadLimitTask;
    }

    public void setTorrent(final Torrent torrent) {
        this.torrent.set(torrent);
        requests = new BitSet[torrent.getNrPieces()];
    }

    public Torrent getTorrent() throws InvalidPeerException {
        if (!valid.get()) {
            throw new InvalidPeerException();
        }
        return torrent.get();
    }

    public boolean cancelKeepAliveWrite() {
        if (keepAliveWrite != null) {
            return keepAliveWrite.cancel();
        } else {
            return true;
        }
    }

    public void setKeepAliveWrite(TimerTask keepAlive)
            throws InvalidPeerException {
        if (!valid.get()) {
            throw new InvalidPeerException();
        }
        this.keepAliveWrite = keepAlive;
    }

    public boolean cancelKeepAliveRead() {
        if (keepAliveRead != null) {
            return keepAliveRead.cancel();
        } else {
            return true;
        }
    }

    public void setKeepAliveRead(TimerTask keepAlive)
            throws InvalidPeerException {
        if (!valid.get()) {
            throw new InvalidPeerException();
        }
        this.keepAliveRead = keepAlive;
    }

    public int countTotalRequests() {
        return totalRequests.get();
    }

    public BitSet getRequests(final int index) {
        return requests[index];
    }

    public void addRequest(final int index, final int begin)
            throws InvalidPeerException {
        if (!valid.get()) {
            throw new InvalidPeerException();
        }
        Torrent t = getTorrent();
        BitSet bs = requests[index];
        if (bs == null) {
            bs = new BitSet(t.computeChunksInPiece(index));
            requests[index] = bs;
        }
        bs.set(TorrentUtil.computeBeginIndex(begin, t.getChunkSize()));
        totalRequests.getAndIncrement();
    }

    public boolean removeRequest(final int index, final int begin)
            throws InvalidPeerException {
        final BitSet bs = requests[index];
        if (bs != null) {
            final int i = TorrentUtil.computeBeginIndex(begin,
                    getTorrent().getChunkSize());
            if (bs.get(i)) {
                bs.clear(i);
                if (bs.isEmpty()) {
                    requests[index] = null;
                }
                totalRequests.getAndDecrement();
                return true;
            }
        }
        return false;
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

    public void resetCounters() {
        uploaded = 0;
        downloaded = 0;
    }

    public int countUploaded() {
        return uploaded;
    }

    public int countDownloaded() {
        return downloaded;
    }

    public void setNextDataExpectation(final int i) {
        data = ByteBuffer.allocate(i);
    }

    public ByteBuffer getData() {
        return data;
    }

    public void incrementDownloaded(int i) throws InvalidPeerException {
        downloaded += i;
        final Torrent t = torrent.get();
        if (t != null) {
            t.incrementDownloaded(i);
        }
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

    public boolean isConnected() {
        return handshakeReceived;
    }

    public void setPeerInterested(final boolean interested) {
        peerInterested = interested;
    }

    public boolean isPeerInterested() {
        return peerInterested;
    }

    private void clearRequests() {
        if (requests != null) {
            for (int i = 0; i < requests.length; i++) {
                requests[i] = null;
            }
        }
    }

    public void setPeerChoking(final boolean choking) {
        peerChoking = choking;
        clearRequests();
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

    public synchronized void disconnect() throws IOException {
        valid.set(false);
        final Torrent t = torrent.get();
        if (t != null) {
            t.removePeer(this);
            torrent.set(null);
        }
        clearRequests();
        messagesToSend.clear();
        cancelKeepAliveRead();
        cancelKeepAliveWrite();
        data = null;
        setReading(false);
        setWriting(false);
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

    public void cancelPieceMessage(final int begin, final int index,
            final int length) throws InvalidPeerException {
        final Iterator<LengthPrefixMessage> i = messagesToSend.iterator();
        while (i.hasNext()) {
            final LengthPrefixMessage m = i.next();
            if (m instanceof PieceMessage) {
                final PieceMessage pm = (PieceMessage) m;
                if (pm.getIndex() == index && pm.getBegin() == begin &&
                        pm.getLength() == length) {
                    i.remove();
                    removeRequest(pm.getIndex(), pm.getBegin());
                }
            }
        }
    }

    public void cancelPieceMessage() throws InvalidPeerException {
        final Iterator<LengthPrefixMessage> i = messagesToSend.iterator();
        while (i.hasNext()) {
            final LengthPrefixMessage m = i.next();
            if (m instanceof PieceMessage) {
                i.remove();
                final PieceMessage pm = (PieceMessage) m;
                removeRequest(pm.getIndex(), pm.getBegin());
            }
        }
    }

    public void setId(final byte[] id) {
        this.id = id;
    }

    public byte[] getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        try {
            final Peer other = (Peer) obj;
            return address.equals(other.address);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return address.toString();
    }

    public SocketAddress getAddress() {
        return address;
    }

    public ByteChannel getChannel() {
        return channel;
    }

    public void setChannel(ByteChannel channel) {
        this.channel = channel;
    }

    public boolean hasMoreMessages() {
        return (currentWrite == null || currentWrite.remaining() > 0 ||
                !isMessagesToSendEmpty());
    }

    public ByteBuffer getCurrentWrite()
            throws InvalidPeerException, IOException {
        if (currentWrite == null || currentWrite.remaining() == 0) {
            final LengthPrefixMessage sm = getMessageToSend();
            if (sm != null) {
                if (sm instanceof PieceMessage) {
                    final PieceMessage pm = (PieceMessage) sm;
                    pm.setPiece(getTorrent().loadPiece(pm.getBegin(),
                            pm.getIndex(), pm.getLength()));
                }
                currentWrite = sm.send();
                currentWrite.flip();
                logger.fine("Sending: " + sm);
            }
        }
        return currentWrite;
    }
}
