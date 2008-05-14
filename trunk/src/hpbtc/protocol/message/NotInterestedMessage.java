/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

/**
 * @author chris
 *
 */
public class NotInterestedMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 3;
    
    public NotInterestedMessage() {
        super(1, TYPE_DISCRIMINATOR);
    }
    
    public NotInterestedMessage(int len) {
        super(len, TYPE_DISCRIMINATOR);
    }
}
