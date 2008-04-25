/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.bencoding.element;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public abstract class BencodedElement {
    
    private static Logger logger = Logger.getLogger(BencodedElement.class.getName());
    
    /**
     * @return
     */
    protected abstract int getEncodedSize();
    
    /**
     * @return
     */
    public byte[] getDigest() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(getEncoded());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            logger.severe("SHA1 is not available");
        }
        return null;
    }
    
    /**
     * @return
     */
    public byte[] getEncoded() {
        byte[] b = new byte[getEncodedSize()];
        encode(b, 0);
        return b;
    }

    protected abstract int encode(byte[] b, int offset);
}
