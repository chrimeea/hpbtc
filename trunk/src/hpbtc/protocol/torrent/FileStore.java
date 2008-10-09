package hpbtc.protocol.torrent;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
        final long m = index * pieceLength;
        long x = 0;
        final Iterator<BTFile> i = files.iterator();
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
        if (pieceLength < chunkSize) {
            chunkSize = pieceLength;
        }
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
        final ByteBuffer bb = ByteBuffer.allocate(pieceLength);
        for (int i = 0; i < nrPieces;) {
            try {
                bb.limit(computePieceLength(i));
                if (isHashCorrect(i, bb)) {
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
            final String rootFolder, final List<Map<byte[], Object>> fls,
            String byteEncoding) throws IOException, NoSuchAlgorithmException {
        files = new ArrayList<BTFile>(fls.size());
        fileLength = 0L;
        for (Map<byte[], Object> fd : fls) {
            final List<byte[]> dirs = (List<byte[]>) fd.get("path".getBytes(
                    byteEncoding));
            final StringBuilder sb = new StringBuilder(rootFolder);
            sb.append(File.separator);
            for (byte[] dir : dirs) {
                sb.append(new String(dir, byteEncoding));
                sb.append(File.separator);
            }
            final long fl = (Long) fd.get("length".getBytes(byteEncoding));
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

    private void loadFileChunk(final List<BTFile> files, final long begin,
            final ByteBuffer dest) throws IOException {
        final Iterator<BTFile> i = files.iterator();
        IOUtil.readFromFile(i.next().getFile(), begin, dest);
        while (i.hasNext()) {
            IOUtil.readFromFile(i.next().getFile(), 0, dest);
        }
    }

    private void saveFileChunk(final List<BTFile> files, final long begin,
            final ByteBuffer piece) throws IOException {
        final Iterator<BTFile> i = files.iterator();
        BTFile f = i.next();
        long j = f.getLength() - begin;
        if (j < piece.capacity()) {
            piece.limit((int) j);
        }
        IOUtil.writeToFile(f.getFile(), begin, piece);
        while (i.hasNext()) {
            f = i.next();
            j = f.getLength() + piece.position();
            if (j > piece.capacity()) {
                j = piece.capacity();
            }
            piece.limit((int) j);
            IOUtil.writeToFile(f.getFile(), 0, piece);
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
                0, fileLength, pieceLength) : pieceLength;
    }

    public boolean savePiece(final int begin, final int index,
            final ByteBuffer piece) throws IOException, NoSuchAlgorithmException {
        final int bi = TorrentUtil.computeBeginIndex(begin, chunkSize);
        final int ei = TorrentUtil.computeEndIndex(begin, piece.remaining(),
                chunkSize);
        final int nsb = pieces[index].nextSetBit(bi);
        if (nsb < 0 || nsb >= ei) {
            pieces[index].set(bi, ei);
            saveFileChunk(getFileList(begin, index, piece.remaining()), offset,
                    piece);
            if (pieces[index].cardinality() == computeChunksInPiece(index)) {
                final ByteBuffer bb =
                        ByteBuffer.allocate(computePieceLength(index));
                if (isHashCorrect(index, bb)) {
                    completePieces.set(index);
                    logger.info("Have piece " + index);
                    return true;
                } else {
                    pieces[index].clear();
                }
            }
        } else {
            logger.fine("Already have this chunk, index " + index + ", begin " + begin);
        }
        return false;
    }

    private List<BTFile> getFileList(final int begin, final int index,
            final int length) {
        long o = index * pieceLength + begin;
        final Iterator<BTFile> i = files.iterator();
        BTFile f;
        do {
            f = i.next();
            o -= f.getLength();
        } while (o > 0);
        offset = o + f.getLength();
        final List<BTFile> fls = new LinkedList<BTFile>();
        o += length;
        fls.add(f);
        while (o >= 0 && i.hasNext()) {
            f = i.next();
            fls.add(f);
            o -= f.getLength();
        }
        return fls;
    }

    public void loadPiece(final int begin, final int index,
            final ByteBuffer bb) throws IOException {
        loadFileChunk(getFileList(begin, index, bb.remaining()), offset, bb);
        bb.rewind();
    }

    private boolean isHashCorrect(final int index, final ByteBuffer bb)
            throws NoSuchAlgorithmException, IOException {
        loadPiece(0, index, bb);
        md.update(bb);
        final byte[] dig = md.digest();
        bb.rewind();
        int i = index * 20;
        for (int j = 0; j < 20; j++) {
            if (dig[j] != pieceHash[i++]) {
                return false;
            }
        }
        return true;
    }
}
