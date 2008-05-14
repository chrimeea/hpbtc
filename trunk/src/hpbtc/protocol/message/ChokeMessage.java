/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

/**
 * @author chris
 *
 */
public class ChokeMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 0;
    
    public ChokeMessage() {
        super(1, TYPE_DISCRIMINATOR);
    }
    
    public ChokeMessage(int len) {
        super(len, TYPE_DISCRIMINATOR);
    }
}
