package hpbtc.protocol.torrent;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
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

    private void loadFileChunk(int k, long begin,
            final ByteBuffer dest) throws IOException {
        int len = dest.remaining();
        int r;
        do {
            r = IOUtil.readFromFile(files.get(k++).getFile(), begin, dest);
            len -= r;
            begin = 0;
        } while (r > 0 && len > 0 && k < files.size());
    }

    private void saveFileChunk(int k, final long begin,
            final ByteBuffer piece) throws IOException {
        int len = piece.remaining();
        BTFile f = files.get(k++);
        long j = f.getLength() - begin + piece.position();
        if (j < piece.capacity()) {
            piece.limit((int) j);
        }
        len -= IOUtil.writeToFile(f.getFile(), begin, piece);
        while (len > 0 && k < files.size()) {
            f = files.get(k++);
            j = f.getLength() + piece.position();
            if (j > piece.capacity()) {
                j = piece.capacity();
            }
            piece.limit((int) j);
            len -= IOUtil.writeToFile(f.getFile(), 0, piece);
        }
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
            long[] l = getFileList(begin, index);
            saveFileChunk((int) l[0], l[1], piece);
            if (pieces[index].cardinality() == computeChunksInPiece(index)) {
                if (isHashCorrect(index,
                        ByteBuffer.allocate(computePieceLength(index)))) {
                    completePieces.set(index);
                    logger.info("Have piece " + index);
                    return true;
                } else {
                    pieces[index].clear();
                    logger.info("Incorrect hash " + index);
                }
            }
        } else {
            logger.fine("Already have Index " + index + ", Begin " + begin);
        }
        return false;
    }

    private long[] getFileList(final int begin, final int index) {
        long o = index * pieceLength + begin;
        int k = -1;
        BTFile f;
        do {
            f = files.get(++k);
            o -= f.getLength();
        } while (o > 0);
        return new long[] {k, o + f.getLength()};
    }

    public void loadPiece(final int begin, final int index,
            final ByteBuffer bb) throws IOException {
        long[] l = getFileList(begin, index);
        loadFileChunk((int) l[0], l[1], bb);
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
