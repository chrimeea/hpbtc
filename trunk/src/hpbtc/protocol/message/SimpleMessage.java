/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

/**
 * @author Cristian Mocanu
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
    public static final byte TYPE_HANDSHAKE = -1;
    public static final byte TYPE_KEEPALIVE = -2;
    
    protected int messageLength;
    protected byte disc;
    protected Peer destination;
    
    public SimpleMessage() {
    }
    
    public void setDestination(final Peer destination) {
        this.destination = destination;
    }
    
    public SimpleMessage(final byte disc, final Peer destination) {
        this(0, disc, destination);
    }
    
    public SimpleMessage(final int len, final byte disc,
            final Peer destination) {
        messageLength = len + 1;
        this.disc = disc;
        this.destination = destination;
    }
    
    public byte getMessageType() {
        return disc;
    }
    
    public Peer getDestination() {
        return destination;
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

    @Override
    public String toString() {
        return "Type: " + disc + ", Peer: " + destination;
    }
}
