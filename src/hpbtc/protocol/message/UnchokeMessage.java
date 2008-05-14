 /*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

/**
 * @author chris
 *
 */
public class UnchokeMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 1;
    
    public UnchokeMessage() {
        super(1, TYPE_DISCRIMINATOR);
    }
    
    public UnchokeMessage(int len) {
        super(len, TYPE_DISCRIMINATOR);
    }
}
