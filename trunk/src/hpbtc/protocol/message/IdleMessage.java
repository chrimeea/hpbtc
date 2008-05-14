/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

/**
 * @author chris
 *
 */
public class IdleMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 0;
    
    public IdleMessage() {
        super(0, TYPE_DISCRIMINATOR);
    }
    
    public IdleMessage(int len) {
        super(len, TYPE_DISCRIMINATOR);
    }
}
