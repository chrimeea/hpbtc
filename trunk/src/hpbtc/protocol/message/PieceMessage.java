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
public class PieceMessage extends BlockMessage {

    public static final byte TYPE_DISCRIMINATOR = 7;
    
    private ByteBuffer piece;
    
    public PieceMessage(ByteBuffer message) {
        super(message.getInt(), message.getInt(), message.remaining());
        piece = message;
    }

    public PieceMessage(int begin, int index, int length, ByteBuffer piece) {
        super(begin, index, length);
        this.piece = piece;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(MessageProcessor processor) {
        processor.process(this);
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(13 + length);
        bb.putInt(9 + length);
        bb.put(TYPE_DISCRIMINATOR);
        bb.putInt(index);
        bb.putInt(begin);
        bb.put(piece);
        return bb;
    }

    public ByteBuffer getPiece() {
        return piece;
    }
}
