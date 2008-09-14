package util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentUtil {
    
    public static int computeChunksInLastPiece(final long fileLength,
            final int nrPieces, final int chunkSize) {
        return computeChunksInNotLastPiece(computeLastPieceSize(fileLength,
                nrPieces, chunkSize), chunkSize);
    }
    
     public static int computeChunksInNotLastPiece(final int pieceLength,
             final int chunkSize) {
        int c = pieceLength / chunkSize;
        int r = pieceLength % chunkSize;
        return r == 0 ? c : c + 1;
    }

    public static int computeLastPieceSize(final long fileLength,
            final int nrPieces, final int chunkSize) {
        return (int) (fileLength - (nrPieces - 1) * chunkSize);
    }
     
    public static int computeRemainingPiece(final int index, final int begin,
            final int chunkSize, final long fileLength, final int nrPieces,
            final int pieceLength) {
        long i = index == nrPieces - 1 ? fileLength : (index + 1) * pieceLength;
        return (int) (i - index * pieceLength - begin);
    }
    
    public static int computeChunkSize(final int index, final int begin,
            final int chunkSize, final long fileLength, final int nrPieces,
            final int pieceLength) {
        return Math.min(chunkSize, computeRemainingPiece(index, begin,
                chunkSize, fileLength, nrPieces, pieceLength));
    }
    
    public static int computeNrPieces(long fileLength, int pieceLength) {
        int nrPieces = (int) (fileLength / pieceLength);
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        return nrPieces;
    }
    
    public static int computeBeginIndex(final int begin, final int chunkSize) {
        return begin / chunkSize;
    }

    public static int computeEndIndex(final int begin, final int length,
            final int chunkSize) {
        return 1 + (begin + length) / chunkSize;
    }

    public static int computeBeginPosition(final int begin,
            final int chunkSize) {
        return begin * chunkSize;
    }

    public static byte[] computeInfoHash(final byte[] info)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        return md.digest(info);
    }
    
    public static byte[] generateId() throws UnsupportedEncodingException {
        byte[] pid = new byte[20];
        ByteBuffer bb = ByteBuffer.wrap(pid);
        bb.put("CristianMocanu_2.0_".getBytes("US-ASCII"));
        Random r = new Random();
        pid[19] = (byte) (r.nextInt(256) - 128);
        return pid;
    }
}
