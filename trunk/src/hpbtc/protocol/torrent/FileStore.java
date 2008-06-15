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
    private int fileLength;
    private int pieceLength;

    public int getNrPieces() {
        return nrPieces;
    }
    
    private void init(int pieceLength, byte[] pieceHash) {
        this.pieceLength = pieceLength;
        nrPieces = fileLength / pieceLength;
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        pieces = new BitSet[nrPieces];
        for (int i = 0; i < nrPieces; i++) {
            pieces[i] = new BitSet(chunkSize);
        }
        this.pieceHash = pieceHash;
    }

    public int getFileLength() {
        return fileLength;
    }
    
    public List<BTFile> getFiles() {
        return files;
    }
    
    public FileStore(int pieceLength, byte[] pieceHash, String rootFolder, List<Map> fls) {
        files = new ArrayList<BTFile>(fls.size());
        fileLength = 0;
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
            files.add(new BTFile(sb.substring(0, sb.length() - 1).toString(), fl));
        }
        init(pieceLength, pieceHash);
    }

    public FileStore(int pieceLength, byte[] pieceHash, String rootFolder,
            String fileName, int fileLength) {
        files = new ArrayList<BTFile>(1);
        this.fileLength = fileLength;
        files.add(new BTFile(rootFolder + File.separator + fileName, fileLength));
        init(pieceLength, pieceHash);
    }

    public void loadFileChunk(List<File> files, int begin, ByteBuffer dest)
            throws IOException {
        Iterator<File> i = files.iterator();
        IOUtil.readFromFile(i.next(), begin, dest);
        for (int j = 0; j < files.size() - 1; j++) {
            IOUtil.readFromFile(i.next(), 0, dest);
        }
    }

    public void saveFileChunk(List<File> files, int begin, ByteBuffer piece)
            throws IOException {
        Iterator<File> i = files.iterator();
        IOUtil.writeToFile(i.next(), begin, piece);
        for (int j = 0; j < files.size() - 1; j++) {
            IOUtil.writeToFile(i.next(), 0, piece);
        }
    }

    public boolean isPieceComplete(int index) {
        return pieces[index].cardinality() == chunkSize;
    }

    public void savePiece(int begin, int index, ByteBuffer piece)
            throws IOException, NoSuchAlgorithmException {
        pieces[index].set(begin / chunkSize, 1 + (begin + piece.remaining()) / chunkSize);
        saveFileChunk(getFileList(begin, index, piece.remaining()), offset, piece);
        if (isPieceComplete(index) && !isHashCorrect(index)) {
            pieces[index].clear();
        }
    }

    private List<File> getFileList(int begin, int index, int length) {
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
        while (o >= 0) {
            f = i.next();
            fls.add(f.getFile());
            o -= f.getLength();
        }
        return fls;
    }

    public ByteBuffer loadPiece(int begin, int index, int length)
            throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(length);
        loadFileChunk(getFileList(begin, index, length), offset, bb);
        return bb;
    }

    private boolean isHashCorrect(int index) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(loadPiece(0, index, pieceLength));
        int i = index * 20;
        return Arrays.equals(md.digest(), Arrays.copyOfRange(pieceHash, i, i + 20));
    }
}
