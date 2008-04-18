/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.client.message;

import hpbtc.client.Client;
import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public abstract class ProtocolMessage {

    public static final byte TYPE_CHOKE = 0;
    public static final byte TYPE_UNCHOKE = 1;
    public static final byte TYPE_INTERESTED = 2;
    public static final byte TYPE_NOT_INTERESTED = 3;
    public static final byte TYPE_HAVE = 4;
    public static final byte TYPE_BITFIELD = 5;
    public static final byte TYPE_REQUEST = 6;
    public static final byte TYPE_PIECE = 7;
    public static final byte TYPE_CANCEL = 8;
    
    public ProtocolMessage() {
    }
      
    public void process(ByteBuffer message, MessageProcessor processor) {
        Client.getInstance().getObserver().fireProcessMessageEvent(this);
    }
    
    public abstract ByteBuffer send();
}
