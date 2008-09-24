package hpbtc.protocol.torrent;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.IOUtil;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class FileStore {

    private static Logger logger = Logger.getLogger(FileStore.class.getName());
    private int chunkSize = 16384;
    private int nrPieces;
    private BitSet[] pieces;
    private long offset;
    private byte[] pieceHash;
    private List<BTFile> files;
    private long fileLength;
    private int pieceLength;
    private BitSet completePieces;
    private int chunksInPiece;
    private int chunksInLastPiece;
    private MessageDigest md;
    
    public int getNrPieces() {
        return nrPieces;
    }

    public BitSet getChunksIndex(final int index) {
        return pieces[index];
    }
    
    public int getPieceLength() {
        return pieceLength;
    }
    
    public BitSet getCompletePieces() {
        return completePieces;
    }

    int getChunkSize() {
        return chunkSize;
    }

    private int jumpToNextFile(final int index) {
        long m = index * pieceLength;
        long x = 0;
        Iterator<BTFile> i = files.iterator();
        while (x <= m && i.hasNext()) {
            BTFile f = i.next();
            x += f.getLength();
        }
        return TorrentUtil.computeNextPieceIndexFromPosition(x, pieceLength);
    }
    
    private void init(final int pieceLength, final byte[] pieceHash)
            throws IOException, NoSuchAlgorithmException {
        md = MessageDigest.getInstance("SHA1");
        completePieces = new BitSet(nrPieces);
        this.pieceLength = pieceLength;
        nrPieces = TorrentUtil.computeNrPieces(fileLength, pieceLength);
        pieces = new BitSet[nrPieces];
        chunksInPiece = TorrentUtil.computeChunksInNotLastPiece(pieceLength,
                chunkSize);
        for (int i = 0; i < nrPieces - 1; i++) {
            pieces[i] = new BitSet(chunksInPiece);
        }
        chunksInLastPiece = TorrentUtil.computeChunksInLastPiece(
                fileLength, pieceLength, chunkSize);
        pieces[nrPieces - 1] = new BitSet(chunksInLastPiece);
        this.pieceHash = pieceHash;
        int k = 0;
        for (int i = 0; i < nrPieces;) {
            try {
                if (isHashCorrect(i, computePieceLength(i))) {
                    pieces[i].set(0, computeChunksInPiece(i));
                    completePieces.set(i);
                    k++;
                }
                i++;
            } catch (IOException e) {
                logger.log(Level.FINER, e.getLocalizedMessage(), e);
                i = jumpToNextFile(i);
            }
        }
        logger.info("Restored " + k + " pieces");
    }

    public long getFileLength() {
        return fileLength;
    }

    public List<BTFile> getFiles() {
        return files;
    }

    public FileStore(final int pieceLength, final byte[] pieceHash,
            final String rootFolder, final List<Map> fls, String byteEncoding)
            throws IOException, NoSuchAlgorithmException {
        files = new ArrayList<BTFile>(fls.size());
        fileLength = 0L;
        for (Map fd : fls) {
            List<byte[]> dirs = (List<byte[]>) fd.get("path".getBytes(
                    byteEncoding));
            StringBuilder sb = new StringBuilder(rootFolder);
            sb.append(File.separator);
            for (byte[] dir : dirs) {
                sb.append(new String(dir, byteEncoding));
                sb.append(File.separator);
            }
            long fl = (Long) fd.get("length".getBytes(byteEncoding));
            fileLength += fl;
            files.add(
                    new BTFile(sb.substring(0, sb.length() - 1).toString(), fl));
        }
        init(pieceLength, pieceHash);
    }

    public FileStore(final int pieceLength, final byte[] pieceHash,
            final String rootFolder, final String fileName,
            final long fileLength) throws IOException, NoSuchAlgorithmException {
        files = new ArrayList<BTFile>(1);
        this.fileLength = fileLength;
        files.add(new BTFile(rootFolder + File.separator + fileName, fileLength));
        init(pieceLength, pieceHash);
    }

    private void loadFileChunk(final List<File> files, final long begin,
            final ByteBuffer dest) throws IOException {
        Iterator<File> i = files.iterator();
        IOUtil.readFromFile(i.next(), begin, dest);
        while (i.hasNext()) {
            IOUtil.readFromFile(i.next(), 0, dest);
        }
    }

    private void saveFileChunk(final List<File> files, final long begin,
            final ByteBuffer piece) throws IOException {
        Iterator<File> i = files.iterator();
        IOUtil.writeToFile(i.next(), begin, piece);
        for (int j = 0; j < files.size() - 1; j++) {
            IOUtil.writeToFile(i.next(), 0, piece);
        }
    }

    public boolean isTorrentComplete() {
        return completePieces.cardinality() == nrPieces;
    }
    
    public boolean isPieceComplete(final int index) {
        return completePieces.get(index);
    }

    public int computeChunksInPiece(int index) {
        return index == nrPieces - 1 ? chunksInLastPiece : chunksInPiece;
    }
    
    private int computePieceLength(int index) {
        return index == nrPieces - 1 ? TorrentUtil.computeRemainingLastPiece(
                0, fileLength, pieceLength): pieceLength;
    }
    
    public boolean savePiece(final int begin, final int index,
            final ByteBuffer piece) throws IOException, NoSuchAlgorithmException {
        pieces[index].set(TorrentUtil.computeBeginIndex(begin, chunkSize),
                TorrentUtil.computeEndIndex(begin, piece.remaining(), chunkSize));
        saveFileChunk(getFileList(begin, index, piece.remaining()), offset,
                piece);
        if (pieces[index].cardinality() == computeChunksInPiece(index)) {
            if (isHashCorrect(index, computePieceLength(index))) {
                completePieces.set(index);
                logger.info("Have piece " + index);
                return true;
            } else {
                pieces[index].clear();
                return false;                
            }
        } else {
            return false;
        }
    }

    private List<File> getFileList(final int begin, final int index,
            final int length) {
        long o = index * pieceLength + begin;
        Iterator<BTFile> i = files.iterator();
        BTFile f;
        do {
            f = i.next();
            o -= f.getLength();
        } while (o > 0);
        offset = o + f.getLength();
        List<File> fls = new LinkedList<File>();
        o += length;
        fls.add(f.getFile());
        while (o >= 0 && i.hasNext()) {
            f = i.next();
            fls.add(f.getFile());
            o -= f.getLength();
        }
        return fls;
    }

    public ByteBuffer loadPiece(final int begin, final int index,
            final int length) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(length);
        loadFileChunk(getFileList(begin, index, length), offset, bb);
        bb.rewind();
        return bb;
    }

    private boolean isHashCorrect(final int index, final int pLength)
            throws NoSuchAlgorithmException, IOException {
        md.update(loadPiece(0, index, pLength));
        byte[] dig = md.digest();
        md.reset();
        int i = index * 20;
        return Arrays.equals(dig, Arrays.copyOfRange(pieceHash, i, i + 20));
    }
}
