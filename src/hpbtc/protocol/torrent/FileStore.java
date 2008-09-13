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
import util.IOUtil;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class FileStore {

    private int chunkSize = 16384;
    private int nrPieces;
    private BitSet[] pieces;
    private int offset;
    private byte[] pieceHash;
    private List<BTFile> files;
    private long fileLength;
    private int pieceLength;
    private BitSet completePieces;

    public int getNrPieces() {
        return nrPieces;
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

    private void init(final int pieceLength, final byte[] pieceHash)
            throws IOException, NoSuchAlgorithmException {
        completePieces = new BitSet(nrPieces);
        this.pieceLength = pieceLength;
        nrPieces = (int) (fileLength / pieceLength);
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        pieces = new BitSet[nrPieces];
        for (int i = 0; i < nrPieces; i++) {
            pieces[i] = new BitSet(chunkSize);
        }
        this.pieceHash = pieceHash;
        for (int i = 0; i < nrPieces; i++) {
            try {
                if (isHashCorrect(i)) {
                    pieces[i].set(0, chunkSize);
                }
            } catch (IOException e) {
            }
        }
    }

    public long getFileLength() {
        return fileLength;
    }

    public List<BTFile> getFiles() {
        return files;
    }

    public FileStore(final int pieceLength, final byte[] pieceHash,
            final String rootFolder, final List<Map> fls)
            throws IOException, NoSuchAlgorithmException {
        files = new ArrayList<BTFile>(fls.size());
        fileLength = 0L;
        for (Map fd : fls) {
            List<String> dirs = (List<String>) fd.get("path");
            StringBuilder sb = new StringBuilder(rootFolder);
            sb.append(File.separator);
            for (String dir : dirs) {
                sb.append(dir);
                sb.append(File.separator);
            }
            int fl = (Integer) fd.get("length");
            fileLength += fl;
            files.add(
                    new BTFile(sb.substring(0, sb.length() - 1).toString(), fl));
        }
        init(pieceLength, pieceHash);
    }

    public FileStore(final int pieceLength, final byte[] pieceHash,
            final String rootFolder, final String fileName,
            final int fileLength) throws IOException, NoSuchAlgorithmException {
        files = new ArrayList<BTFile>(1);
        this.fileLength = fileLength;
        files.add(new BTFile(rootFolder + File.separator + fileName, fileLength));
        init(pieceLength, pieceHash);
    }

    private void loadFileChunk(final List<File> files, final int begin,
            final ByteBuffer dest) throws IOException {
        Iterator<File> i = files.iterator();
        IOUtil.readFromFile(i.next(), begin, dest);
        for (int j = 0; j < files.size() - 1; j++) {
            IOUtil.readFromFile(i.next(), 0, dest);
        }
    }

    private void saveFileChunk(final List<File> files, final int begin,
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

    public boolean savePiece(final int begin, final int index,
            final ByteBuffer piece) throws IOException, NoSuchAlgorithmException {
        pieces[index].set(TorrentUtil.computeBeginIndex(begin, chunkSize),
                TorrentUtil.computeEndIndex(begin, piece.remaining(), chunkSize));
        saveFileChunk(getFileList(begin, index, piece.remaining()), offset,
                piece);
        if (pieces[index].cardinality() == chunkSize && !isHashCorrect(index)) {
            pieces[index].clear();
            return false;
        } else {
            completePieces.set(index);
            return true;
        }
    }

    private List<File> getFileList(final int begin, final int index,
            final int length) {
        int o = index * chunkSize + begin;
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
        return bb;
    }

    private boolean isHashCorrect(final int index)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(loadPiece(0, index, pieceLength));
        int i = index * 20;
        return Arrays.equals(md.digest(), Arrays.copyOfRange(pieceHash, i, i +
                20));
    }
}
