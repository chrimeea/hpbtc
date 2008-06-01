package hpbtc.processor;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *
 * @author Cristian Mocanu
 */
public class PieceRepository {
    
    public PieceRepository() {
    }

    private boolean isHashCorrect(ByteBuffer bb, byte[] pieceHash) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        bb.rewind();
        md.update(bb);
        return Arrays.equals(md.digest(), pieceHash);
    }
    
    public boolean isPiece(byte[] infoHash, int index) {
        return false;
    }
    
    public ByteBuffer getPiece(byte[] infoHash, int begin, int index, int length) {
        return null;
    }
    
    public void savePiece(byte[] infoHash, int begin, int index, ByteBuffer piece) {
    }
    
}
