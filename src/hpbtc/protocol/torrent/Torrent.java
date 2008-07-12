package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import hpbtc.bencoding.BencodingWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Cristian Mocanu
 */
public class Torrent {

    private List<LinkedList<String>> trackers;
    private byte[] infoHash;
    private Date creationDate;
    private String comment;
    private String createdBy;
    private String encoding;
    private FileStore fileStore;
    private Tracker tracker;
    private Set<Peer> peers;
    private Random random;

    public Torrent(InputStream is, String rootFolder, byte[] peerId, int port)
            throws IOException, NoSuchAlgorithmException {
        BencodingReader parser = new BencodingReader(is);
        Map<String, Object> meta = parser.readNextDictionary();
        Map<String, Object> info = (Map) meta.get("info");
        infoHash = computeInfoHash(info);
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
        tracker = new Tracker(infoHash, peerId, port, trackers);
        peers = new HashSet<Peer>();
        random = new Random();
    }

    public int decideNextDownloadPiece(Peer peer) {
        BitSet bs = peer.getPieces();
        int piece = random.nextInt(bs.cardinality());
        int j = bs.nextSetBit(0);
        for (int i = 1; i < piece; i++) {
            j = bs.nextSetBit(j + 1);
        }
        return j;
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
        fileStore.savePiece(begin, index, piece);
    }
    
    public ByteBuffer loadPiece(int begin, int index, int length)
            throws IOException {
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
    
    private static byte[] computeInfoHash(Map<String, Object> info)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BencodingWriter w = new BencodingWriter(os);
        w.write(info);
        md.update(os.toByteArray());
        return md.digest();
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

    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof Torrent) {
            Torrent t = (Torrent) arg0;
            return Arrays.equals(infoHash, t.infoHash);
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {
        return infoHash.hashCode();
    }
}
