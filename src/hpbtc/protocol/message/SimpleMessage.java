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
public class SimpleMessage {
    
    public static final byte TYPE_BITFIELD = 5;
    public static final byte TYPE_CANCEL = 8;
    public static final byte TYPE_CHOKE = 0;
    public static final byte TYPE_HAVE = 4;
    public static final byte TYPE_INTERESTED = 2;
    public static final byte TYPE_NOT_INTERESTED = 3;
    public static final byte TYPE_PIECE = 7;
    public static final byte TYPE_REQUEST = 6;
    public static final byte TYPE_UNCHOKE = 1;
    
    protected int messageLength;
    protected byte disc;
    
    public SimpleMessage() {
    }
    
    public SimpleMessage(int len, byte disc) {
        messageLength = len + 1;
        this.disc = disc;
    }
    
    public int getMessageLength() {
        return messageLength;
    }
    
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(messageLength + 4);
        bb.putInt(messageLength);
        bb.put(disc);
        return bb;
    }
}
