package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import hpbtc.bencoding.BencodingWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu
 */
public class Torrent {

    private List<LinkedList<String>> trackers;
    private byte[] infoHash;
    private boolean multiple;
    private int pieceLength;
    private List<BTFile> files;
    private int fileLength;
    private int nrPieces;
    private byte[] pieceHash;
    private Date creationDate;
    private String comment;
    private String createdBy;
    private String encoding;
    private int chunkSize = 16384;
    private BitSet[] pieces;
    private FileStore fileStore;

    public Torrent(FileStore fileStore, InputStream is, File rootFolder)
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
        multiple = info.containsKey("files");
        String fileName = (String) info.get("name");
        pieceLength = (Integer) info.get("piece length");
        if (multiple) {
            List<Map> fls = (List<Map>) info.get("files");
            files = new ArrayList<BTFile>(fls.size());
            fileLength = 0;
            for (Map fd : fls) {
                List<String> dirs = (List<String>) fd.get("path");
                StringBuilder sb = new StringBuilder(fileName);
                sb.append(File.separator);
                for (String dir : dirs) {
                    sb.append(dir);
                    sb.append(File.separator);
                }
                int fl = (Integer) fd.get("length");
                fileLength += fl;
                files.add(new BTFile(rootFolder, sb.substring(0, sb.length() - 1).toString(), fl));
            }
        } else {
            files = new ArrayList<BTFile>(1);
            fileLength = (Integer) info.get("length");
            files.add(new BTFile(rootFolder, fileName, fileLength));
        }
        nrPieces = (int) (fileLength / pieceLength);
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        pieceHash = ((String) info.get("pieces")).getBytes(encoding);
        pieces = new BitSet[nrPieces];
        for (int i = 0; i < nrPieces; i++) {
            pieces[i] = new BitSet(chunkSize);
        }
        this.fileStore = fileStore;
    }

    public boolean isPieceComplete(int index) {
        return pieces[index].cardinality() == chunkSize;
    }

    public void savePiece(int begin, int index, ByteBuffer piece) {
        pieces[index].set(begin / chunkSize, 1 + (begin + piece.remaining()) / chunkSize);
    //TODO save to filestore
    //TODO daca piesa e completa verifica hashul
    }

    public ByteBuffer loadPiece(int begin, int index, int length) {
        int o = index * chunkSize + begin;
        Iterator<BTFile> i = files.iterator();
        BTFile f;
        do {
            f = i.next();
            o -= f.getLength();
        } while (o > 0);
        int b = o + f.getLength();
        List<BTFile> fls = new LinkedList<BTFile>();
        o = length + o;
        fls.add(f);
        while (o >= 0) {
            f = i.next();
            fls.add(f);
            o -= f.getLength();
        }
        return fileStore.loadFileChunk(fls, b, length);
    }

    private static byte[] computeInfoHash(Map<String, Object> info) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BencodingWriter w = new BencodingWriter(os);
        w.write(info);
        md.update(os.toByteArray());
        return md.digest();
    }

    public int getFileLength() {
        return fileLength;
    }

    public List<BTFile> getFiles() {
        return files;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public int getNrPieces() {
        return nrPieces;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public boolean isMultiple() {
        return multiple;
    }

    private boolean isHashCorrect(ByteBuffer bb, int index) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        bb.rewind();
        md.update(bb);
        int i = index * 20;
        return Arrays.equals(md.digest(), Arrays.copyOfRange(pieceHash, i, i + 20));
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
