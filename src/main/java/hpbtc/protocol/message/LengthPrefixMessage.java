package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

/**
 *
 * @author Cristian Mocanu
 */
public class LengthPrefixMessage {

    protected int messageLength;
    protected Peer destination;
    
    public LengthPrefixMessage(final int length, final Peer destination) {
        this.destination = destination;
        this.messageLength = length;
    }
    
    public Peer getDestination() {
        return destination;
    }
    
    public int getMessageLength() {
        return messageLength;
    }

    @Override
    public String toString() {
        return "Peer: " + destination;
    }
    
    public ByteBuffer send() {
        final ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(messageLength);
        return bb;
    }
}
