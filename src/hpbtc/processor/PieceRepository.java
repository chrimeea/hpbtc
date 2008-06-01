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

    private boolean isHashCorrect(ByteBuffer bb, byte[] hash) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        bb.rewind();
        md.update(bb);
        return Arrays.equals(md.digest(), hash);
    }
    
}
