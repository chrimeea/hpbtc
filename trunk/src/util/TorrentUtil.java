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
            final int pieceLength, final int chunkSize) {
        return computeChunksInNotLastPiece(computeRemainingLastPiece(0,
                fileLength, pieceLength), chunkSize);
    }

    public static int computeChunksInNotLastPiece(final int pieceLength,
            final int chunkSize) {
        int c = pieceLength / chunkSize;
        int r = pieceLength % chunkSize;
        return r == 0 ? c : c + 1;
    }

    public static int computeRemainingLastPiece(final int begin,
            final long fileLength, final int pieceLength) {
        return (int) (fileLength -
                (computeNrPieces(fileLength, pieceLength) - 1) * pieceLength -
                begin);
    }

    public static int computeChunkSize(final int index, final int begin,
            final int chunkSize, final long fileLength, final int pieceLength) {
        if (index < computeNrPieces(fileLength, pieceLength) - 1) {
            return chunkSize;
        }
        return Math.min(chunkSize, computeRemainingLastPiece(begin, fileLength,
                pieceLength));
    }

    public static int computeNrPieces(long fileLength, int pieceLength) {
        int nrPieces = (int) (fileLength / pieceLength);
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        return nrPieces;
    }

    public static int computeNextPieceIndexFromPosition(final long position,
            final int pieceLength) {
        int n = (int) (position / pieceLength);
        if (position % pieceLength > 0) {
            n++;
        }
        return n;
    }

    public static int computeBeginIndex(final int begin, final int chunkSize) {
        return begin / chunkSize;
    }

    public static int computeEndIndex(final int begin, final int length,
            final int chunkSize) {
        int i = begin + length;
        int n = i / chunkSize;
        if (i % chunkSize > 0) {
            n++;
        }
        return n;
    }

    public static int computeBeginPosition(final int begin,
            final int chunkSize) {
        return begin * chunkSize;
    }

    public static byte[] generateId() throws UnsupportedEncodingException {
        byte[] pid = new byte[20];
        ByteBuffer bb = ByteBuffer.wrap(pid);
        bb.put("CristianMocanu_2.0_".getBytes("US-ASCII"));
        Random r = new Random();
        pid[19] = (byte) (r.nextInt(256) - 128);
        return pid;
    }

    public static byte[] getSupportedProtocol() throws
            UnsupportedEncodingException {
        byte[] protocol = new byte[20];
        ByteBuffer pr = ByteBuffer.wrap(protocol);
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes("US-ASCII"));
        return protocol;
    }
}
