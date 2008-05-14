/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class CancelMessage extends BlockMessage {

    public static final byte TYPE_DISCRIMINATOR = 8;

    public CancelMessage(ByteBuffer message, int len) {
        super(message, len, TYPE_DISCRIMINATOR);
    }
    
    /**
     * @param p
     */
    public CancelMessage(int begin, int index, int length) {
        super(begin, index, length, TYPE_DISCRIMINATOR);
    }
}
