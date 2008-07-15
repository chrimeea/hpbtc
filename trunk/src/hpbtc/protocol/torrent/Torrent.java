package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Torrent {

    private static Logger logger = Logger.getLogger(Torrent.class.getName());
    private List<LinkedList<String>> trackers;
    private byte[] infoHash;
    private Date creationDate;
    private String comment;
    private String createdBy;
    private String encoding;
    private FileStore fileStore;
    private Tracker tracker;
    private Set<Peer> peers;
    private BitSet[] requests;
    private Random random;
    private Network network;
    private int uploaded;
    private int downloaded;
    private int optimisticCounter;

    public Torrent(InputStream is, String rootFolder, byte[] peerId, Network network)
            throws IOException, NoSuchAlgorithmException {
        random = new Random();
        this.network = network;
        BencodingReader parser = new BencodingReader(is);
        Map<String, Object> meta = parser.readNextDictionary();
        Map<String, Object> info = (Map) meta.get("info");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BencodingWriter w = new BencodingWriter(os);
        w.write(info);
        infoHash = TorrentUtil.computeInfoHash(os.toByteArray());
        os.close();
        if (meta.containsKey("announce-list")) {
            trackers = (List<LinkedList<String>>) meta.get("announce-list");
        } else {
            trackers = new ArrayList<LinkedList<String>>(1);
            LinkedList<String> ul = new LinkedList<String>();
            ul.add((String) meta.get("announce"));
            trackers.add(ul);
        }
        if (meta.containsKey("creation date")) {
            creationDate = new Date(((Integer) meta.get("creation date")) * 1000L);
        }
        if (meta.containsKey("comment")) {
            comment = (String) meta.get("comment");
        }
        if (meta.containsKey("created by")) {
            createdBy = (String) meta.get("created by");
        }
        if (meta.containsKey("encoding")) {
            encoding = (String) meta.get("encoding");
        }
        boolean multiple = info.containsKey("files");
        int pieceLength = (Integer) info.get("piece length");
        byte[] pieceHash = ((String) info.get("pieces")).getBytes(encoding);
        if (multiple) {
            List<Map> fls = (List<Map>) info.get("files");
            fileStore = new FileStore(pieceLength, pieceHash, rootFolder, fls);
        } else {
            String fileName = (String) info.get("name");
            int fileLength = (Integer) info.get("length");
            fileStore = new FileStore(pieceLength, pieceHash, rootFolder, fileName, fileLength);
        }
        tracker = new Tracker(infoHash, peerId, network.getPort(), trackers);
        peers = new HashSet<Peer>();
        requests = new BitSet[getNrPieces()];
    }

    public void decideChoking() {
        List<Peer> prs = new LinkedList<Peer>(peers);
        Comparator<Peer> comp = new Comparator<Peer>() {

            public int compare(Peer p1, Peer p2) {
                return p2.countUploaded() - p1.countUploaded();
            }
        };
        if (++optimisticCounter == 3) {
            Peer optimisticPeer = prs.remove(random.nextInt(prs.size()));
            Collections.sort(prs, Collections.reverseOrder(comp));
            prs.add(optimisticPeer);
            Collections.reverse(prs);
            optimisticCounter = 0;
        } else {
            Collections.sort(prs, comp);
        }
        SimpleMessage mUnchoke = new SimpleMessage(SimpleMessage.TYPE_UNCHOKE);
        SimpleMessage mChoke = new SimpleMessage(SimpleMessage.TYPE_CHOKE);
        int k = 0;
        for (Peer p : prs) {
            if (p.isClientChoking() && k < 3) {
                try {
                    network.postMessage(p, mUnchoke);
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
                }
                if (p.isPeerInterested()) {
                    k++;
                }
            } else if (!p.isClientChoking()) {
                try {
                    network.postMessage(p, mChoke);
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
                }                
            }
            p.resetCounters();
        }
    }

    private int getActualPieceSize(int index) {
        int n = getNrPieces();
        int l = getPieceLength();
        return index == n - 1 ? (n - 1) * l + getFileLength() : l;
    }

    public void decideNextPiece(Peer peer) throws IOException {
        BitSet bs = peer.getOtherPieces(getCompletePieces());
        int chunkSize = fileStore.getChunkSize();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            if (requests[i].cardinality() == chunkSize) {
                bs.clear(i);
            }
        }
        int c = bs.cardinality();
        if (c > 0) {
            int r = random.nextInt(c);
            int index = bs.nextSetBit(0);
            for (; index < r; index = bs.nextSetBit(index + 1)) {
            }
            int begin = TorrentUtil.computeBeginPosition(requests[index].nextSetBit(0), chunkSize);
            int length = getActualPieceSize(index);
            SimpleMessage message = new BlockMessage(begin, index, length, SimpleMessage.TYPE_REQUEST);
            network.postMessage(peer, message);
            requests[index].set(TorrentUtil.computeBeginIndex(begin, chunkSize),
                    TorrentUtil.computeEndIndex(begin, length, chunkSize));
        }
    }

    public Iterable<Peer> getPeers() {
        return peers;
    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
    }

    public void beginTracker() {
        peers.addAll(tracker.beginTracker(getFileLength()));
    }

    public void savePiece(int begin, int index, ByteBuffer piece)
            throws IOException, NoSuchAlgorithmException {
        downloaded += piece.remaining();
        if (fileStore.savePiece(begin, index, piece)) {
            SimpleMessage message = new HaveMessage(index);
            for (Peer p : peers) {
                if (p.isConnected()) {
                    network.postMessage(p, message);
                    if (p.getOtherPieces(getCompletePieces()).isEmpty()) {
                        SimpleMessage smessage = new SimpleMessage(SimpleMessage.TYPE_NOT_INTERESTED);
                        network.postMessage(p, smessage);
                    }
                }
            }
        }
    }

    public ByteBuffer loadPiece(int begin, int index, int length)
            throws IOException {
        uploaded += length;
        return fileStore.loadPiece(begin, index, length);
    }

    public List<BTFile> getFiles() {
        return fileStore.getFiles();
    }

    public int getFileLength() {
        return fileStore.getFileLength();
    }

    public int getNrPieces() {
        return fileStore.getNrPieces();
    }

    public boolean isPieceComplete(int index) {
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

    public List<LinkedList<String>> getTrackers() {
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
