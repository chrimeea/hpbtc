/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

/**
 * @author chris
 *
 */
public class InterestedMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 2;
    
    public InterestedMessage() {
        super(1, TYPE_DISCRIMINATOR);
    }
    
    public InterestedMessage(int len) {
        super(len, TYPE_DISCRIMINATOR);
    }
}
