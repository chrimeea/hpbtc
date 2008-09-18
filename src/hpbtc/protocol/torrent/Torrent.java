package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.SimpleMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Torrent {

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
    private Random random;
    private long uploaded;
    private long downloaded;
    private int optimisticCounter;

    public Torrent(final InputStream is, final String rootFolder)
            throws IOException, NoSuchAlgorithmException {
        random = new Random();
        BencodingReader parser = new BencodingReader(is);
        Map<byte[], Object> meta = parser.readNextDictionary();
        Map<byte[], Object> info = (Map) meta.get("info".getBytes(byteEncoding));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BencodingWriter w = new BencodingWriter(os);
        w.write(info);
        infoHash = TorrentUtil.computeInfoHash(os.toByteArray());
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
        boolean multiple = info.containsKey("files".getBytes(byteEncoding));
        int pieceLength = ((Long) info.get("piece length".getBytes(
                byteEncoding))).intValue();
        byte[] pieceHash = (byte[]) info.get("pieces".getBytes(byteEncoding));
        if (multiple) {
            List<Map> fls = (List<Map>) info.get("files".getBytes(byteEncoding));
            fileStore = new FileStore(pieceLength, pieceHash, rootFolder, fls,
                    byteEncoding);
        } else {
            String fileName = new String((byte[]) info.get("name".getBytes(
                    byteEncoding)), byteEncoding);
            long fileLength = (Long) info.get("length".getBytes(byteEncoding));
            fileStore = new FileStore(pieceLength, pieceHash, rootFolder,
                    fileName, fileLength);
        }
        peers = new CopyOnWriteArraySet<Peer>();
        freshPeers = new HashSet<Peer>();
    }

    public String getByteEncoding() {
        return byteEncoding;
    }

    public Collection<Peer> getConnectedPeers() {
        return peers;
    }

    public synchronized void addFreshPeers(Set<Peer> otherPeers) {
        freshPeers.addAll(otherPeers);
    }

    public synchronized Set<Peer> getFreshPeers() {
        Set<Peer> p = freshPeers;
        freshPeers = null;
        p.removeAll(peers);
        return p;
    }

    public List<SimpleMessage> decideChoking() {
        List<Peer> prs = new ArrayList<Peer>(peers);
        Comparator<Peer> comp = isTorrentComplete() ? new Comparator<Peer>() {

            public int compare(Peer p1, Peer p2) {
                return p2.countUploaded() - p1.countUploaded();
            }
        }
                : new Comparator<Peer>() {

            public int compare(Peer p1, Peer p2) {
                return p2.countDownloaded() - p1.countDownloaded();
            }
        };
        if (++optimisticCounter == 3 && !prs.isEmpty()) {
            Peer optimisticPeer = prs.remove(random.nextInt(prs.size()));
            Collections.sort(prs, comp);
            prs.add(0, optimisticPeer);
            optimisticCounter = 0;
        } else {
            Collections.sort(prs, comp);
        }
        int k = 0;
        List<SimpleMessage> result = new LinkedList<SimpleMessage>();
        for (Peer p : prs) {
            if (k < 4) {
                if (p.isClientChoking()) {
                    SimpleMessage mUnchoke = new SimpleMessage(
                            SimpleMessage.TYPE_UNCHOKE, p);
                    result.add(mUnchoke);
                }
                if (p.isPeerInterested()) {
                    k++;
                }
            } else if (!p.isClientChoking()) {
                SimpleMessage mChoke = new SimpleMessage(
                        SimpleMessage.TYPE_CHOKE, p);
                result.add(mChoke);
            }
            p.resetCounters();
        }
        return result;
    }

    public int getChunkSize() {
        return fileStore.getChunkSize();
    }

    public int computeChunksInPiece(int index) {
        return fileStore.computeChunksInPiece(index);
    }

    public BitSet getOtherPieces(final Peer peer) {
        BitSet pieces = peer.getPieces();
        BitSet bs = getCompletePieces();
        if (pieces == null) {
            pieces = new BitSet(bs.size());
            return pieces;
        } else {
            BitSet c = (BitSet) pieces.clone();
            c.andNot(bs);
            return c;
        }
    }
    
    private BitSet getChunksSavedAndRequested(Peer peer, int index) {
        BitSet saved = (BitSet) fileStore.getChunksIndex(index).clone();
        BitSet req = peer.getRequests(index);
        if (req != null) {
            saved.or(req);
        }
        return saved;
    }
    
    public BlockMessage decideNextPiece(final Peer peer) {
        BitSet peerPieces = (BitSet) peer.getPieces().clone();
        peerPieces.andNot(getCompletePieces());
        int max = 0;
        int index = -1;
        int beginIndex = 0;
        int n = getNrPieces();
        BitSet rest = (BitSet) peerPieces.clone();
        for (int i = peerPieces.nextSetBit(0); i >= 0;
        i = peerPieces.nextSetBit(i + 1)) {
            BitSet sar = getChunksSavedAndRequested(peer, i);
            int card = sar.cardinality();
            int ch = sar.nextClearBit(0);
            if (ch < computeChunksInPiece(i)) {
                if (card > max) {
                    max = card;
                    index = i;
                    beginIndex = ch;
                }
            } else {
                rest.clear(i);
            }
        }
        if (index < 0) {
            int r = random.nextInt(rest.cardinality());
            index = rest.nextSetBit(0);
            for (; index < r; index = peerPieces.nextSetBit(index + 1)) {
            }
        }
        int cs = getChunkSize();
        int begin = TorrentUtil.computeBeginPosition(beginIndex, cs);
        return new BlockMessage(begin, index,
                TorrentUtil.computeChunkSize(index, begin, cs, getFileLength(),
                n, getPieceLength()), SimpleMessage.TYPE_REQUEST, peer);
    }

    public void removePeer(final Peer peer) {
        peers.remove(peer);
    }

    public void addPeer(final Peer peer) {
        peers.add(peer);
    }

    public long getUploaded() {
        return uploaded;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public boolean savePiece(final int begin, final int index,
            final ByteBuffer piece) throws IOException,
            NoSuchAlgorithmException {
        downloaded += piece.remaining();
        return fileStore.savePiece(begin, index, piece);
    }

    public boolean isTorrentComplete() {
        return fileStore.isTorrentComplete();
    }

    public ByteBuffer loadPiece(final int begin, final int index,
            final int length) throws IOException {
        uploaded += length;
        return fileStore.loadPiece(begin, index, length);
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
