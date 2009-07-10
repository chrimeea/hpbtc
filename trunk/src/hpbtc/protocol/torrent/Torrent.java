package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import hpbtc.bencoding.BencodingWriter;
import hpbtc.util.TorrentUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Torrent {

    private static Logger logger = Logger.getLogger(Torrent.class.getName());
    private List<LinkedList<byte[]>> trackers;
    private String byteEncoding = "US-ASCII";
    private byte[] infoHash;
    private Date creationDate;
    private String comment;
    private String createdBy;
    private String encoding;
    private FileStore fileStore;
    private Set<Peer> peers;
    private Set<Peer> freshPeers;
    private AtomicLong uploaded;
    private AtomicLong downloaded;
    private Tracker tracker;
    private AtomicIntegerArray availability;
    private int optimisticCounter;
    private AtomicInteger remainingPeers;
    private TimerTask trackerTask;

    public Torrent(final InputStream is, final String rootFolder,
            final byte[] peerId, final int port)
            throws IOException, NoSuchAlgorithmException {
        final BencodingReader parser = new BencodingReader(is);
        final Map<byte[], Object> meta = parser.readNextDictionary();
        final Map<byte[], Object> info = (Map) meta.get("info".getBytes(
                byteEncoding));
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BencodingWriter w = new BencodingWriter(os);
        w.write(info);
        infoHash = MessageDigest.getInstance("SHA1").digest(os.toByteArray());
        os.close();
        if (meta.containsKey("announce-list".getBytes(byteEncoding))) {
            trackers = (List<LinkedList<byte[]>>) meta.get("announce-list".
                    getBytes(byteEncoding));
        } else {
            trackers = new ArrayList<LinkedList<byte[]>>(1);
            LinkedList<byte[]> ul = new LinkedList<byte[]>();
            ul.add((byte[]) meta.get("announce".getBytes(byteEncoding)));
            trackers.add(ul);
        }
        if (meta.containsKey("creation date".getBytes(byteEncoding))) {
            creationDate = new Date(((Long) meta.get(
                    "creation date".getBytes(byteEncoding))) *
                    1000L);
        }
        if (meta.containsKey("comment".getBytes(byteEncoding))) {
            comment = new String((byte[]) meta.get("comment".getBytes(
                    byteEncoding)), byteEncoding);
        }
        if (meta.containsKey("created by".getBytes(byteEncoding))) {
            createdBy = new String((byte[]) meta.get("created by".getBytes(
                    byteEncoding)), byteEncoding);
        }
        if (meta.containsKey("encoding".getBytes(byteEncoding))) {
            encoding = new String((byte[]) meta.get("encoding".getBytes(
                    byteEncoding)), byteEncoding);
        }
        final boolean multiple =
                info.containsKey("files".getBytes(byteEncoding));
        final int pieceLength = ((Long) info.get("piece length".getBytes(
                byteEncoding))).intValue();
        final byte[] pieceHash = (byte[]) info.get("pieces".getBytes(
                byteEncoding));
        if (multiple) {
            final List<Map<byte[], Object>> fls =
                    (List<Map<byte[], Object>>) info.get("files".getBytes(
                    byteEncoding));
            fileStore = new FileStore(pieceLength, pieceHash, rootFolder, fls,
                    byteEncoding);
        } else {
            final String fileName = new String((byte[]) info.get("name".getBytes(
                    byteEncoding)), byteEncoding);
            final long fileLength = (Long) info.get("length".getBytes(
                    byteEncoding));
            fileStore = new FileStore(pieceLength, pieceHash, rootFolder,
                    fileName, fileLength);
        }
        peers = Collections.synchronizedSet(new HashSet<Peer>());
        freshPeers = new HashSet<Peer>();
        tracker = new Tracker(infoHash, peerId, port, trackers,
                byteEncoding);
        availability = new AtomicIntegerArray(getNrPieces());
        remainingPeers = new AtomicInteger();
        uploaded = new AtomicLong();
        downloaded = new AtomicLong();
    }

    public void setTrackerTask(TimerTask trackerTask) {
        this.trackerTask = trackerTask;
    }

    public boolean hasTrackerTask() {
        return trackerTask != null;
    }

    public boolean cancelTrackerTask() {
        if (trackerTask != null) {
            return trackerTask.cancel();
        }
        return true;
    }

    public int increaseOptimisticCounter() {
        return ++optimisticCounter;
    }

    public void setOptimisticCounter(int optimisticCounter) {
        this.optimisticCounter = optimisticCounter;
    }

    public void updateAvailability(BitSet bs) {
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            availability.getAndIncrement(i);
        }
    }

    public Tracker getTracker() {
        return tracker;
    }

    public void updateAvailability(int index) {
        availability.getAndIncrement(index);
    }

    public int getAvailability(int index) {
        return availability.get(index);
    }

    public int getOptimisticCounter() {
        return optimisticCounter;
    }

    public String getByteEncoding() {
        return byteEncoding;
    }

    public Set<Peer> getConnectedPeers() {
        return peers;
    }

    private void completeFreshPeers(Set<Peer> pr) {
        for (Peer p : pr) {
            p.setTorrent(this);
        }
        if (!pr.isEmpty()) {
            synchronized(freshPeers) {
                freshPeers = pr;
            }
        }
    }

    public void endTracker() {
        completeFreshPeers(tracker.endTracker(uploaded.get(), downloaded.get()));
    }
    
    public void beginTracker() {
        completeFreshPeers(tracker.beginTracker(
                TorrentUtil.computeRemainingBytes(getFileLength(),
                getPieceLength(), getCompletePieces().cardinality(),
                isPieceComplete(getNrPieces() - 1))));
    }

    public void updateTracker() {
        completeFreshPeers(tracker.updateTracker(null, uploaded.get(),
                downloaded.get(), TorrentUtil.computeRemainingBytes(
                getFileLength(), getPieceLength(),
                getCompletePieces().cardinality(),
                isPieceComplete(getNrPieces() - 1)), true));
    }

    public Set<Peer> getFreshPeers() {
        int s;
        synchronized(peers) {
            freshPeers.removeAll(peers);
            s = freshPeers.size();
        }
        remainingPeers.getAndAdd(s);
        return freshPeers;
    }

    public int getChunkSize() {
        return fileStore.getChunkSize();
    }

    public int computeChunksInPiece(int index) {
        return fileStore.computeChunksInPiece(index);
    }

    public BitSet getOtherPieces(final Peer peer) {
        BitSet pieces = peer.getPieces();
        final BitSet bs = getCompletePieces();
        if (pieces == null) {
            pieces = new BitSet(bs.size());
            return pieces;
        } else {
            final BitSet c = (BitSet) pieces.clone();
            c.andNot(bs);
            return c;
        }
    }

    public BitSet getChunksRequested(final int index) {
        final BitSet b = new BitSet(computeChunksInPiece(index));
        synchronized (peers) {
            for (Peer p : peers) {
                final BitSet req = p.getRequests(index);
                if (req != null) {
                    b.or(req);
                }
            }
        }
        return b;
    }
    
    public BitSet getChunksSaved(final int index) {
        return fileStore.getChunksIndex(index);
    }

    public int getRemainingPeers() {
        return remainingPeers.get();
    }

    /**
     * Do not call this method twice for the same peer because
     * it will damage the remaining peers count
     * @param peer
     */
    public void removePeer(final Peer peer) {
        if (peers.remove(peer)) {
            final BitSet bs = peer.getPieces();
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                availability.getAndDecrement(i);
            }
        }
        final int rem = remainingPeers.decrementAndGet();
        logger.info("Have " + rem + " peers");
    }

    public void addPeer(final Peer peer, boolean isIncoming) {
        if (peers.add(peer) && isIncoming) {
            final int rem = remainingPeers.incrementAndGet();
            downloaded.getAndAdd(48);
            logger.info("Have " + rem + " peers");
        }
    }

    public long getUploaded() {
        return uploaded.get();
    }

    public long getDownloaded() {
        return downloaded.get();
    }

    public void incrementUploaded(int up) {
        uploaded.getAndAdd(up);
    }
    
    public void incrementDownloaded(int down) {
        downloaded.getAndAdd(down);
    }
    
    public boolean savePiece(final int begin, final int index,
            final ByteBuffer piece) throws IOException,
            NoSuchAlgorithmException {
        return fileStore.savePiece(begin, index, piece);
    }

    public ByteBuffer loadPiece(final int begin, final int index,
            final int length) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(length);
        fileStore.loadPiece(begin, index, bb);
        return bb;
    }

    public List<BTFile> getFiles() {
        return fileStore.getFiles();
    }

    public long getFileLength() {
        return fileStore.getFileLength();
    }

    public int getNrPieces() {
        return fileStore.getNrPieces();
    }

    public boolean isPieceComplete(final int index) {
        return fileStore.isPieceComplete(index);
    }

    public BitSet getCompletePieces() {
        return fileStore.getCompletePieces();
    }

    public int countRemainingPieces() {
        return getNrPieces() - getCompletePieces().cardinality();
    }
    
    public byte[] getInfoHash() {
        return infoHash;
    }

    public int getPieceLength() {
        return fileStore.getPieceLength();
    }

    public List<LinkedList<byte[]>> getTrackers() {
        return trackers;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getEncoding() {
        return encoding;
    }
}
