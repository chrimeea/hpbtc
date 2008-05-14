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
public class PieceMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 7;
    
    private int begin;
    private int index;
    private ByteBuffer piece;
    
    public PieceMessage(ByteBuffer message, int len) {
        super(len, TYPE_DISCRIMINATOR);
        begin = message.getInt();
        index = message.getInt();
        piece = message;
    }

    public PieceMessage(int begin, int index, ByteBuffer piece) {
        super(9 + piece.remaining(), TYPE_DISCRIMINATOR);
        this.begin = begin;
        this.index = index;
        this.piece = piece;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.putInt(index);
        bb.putInt(begin);
        bb.put(piece);
        return bb;
    }

    public ByteBuffer getPiece() {
        return piece;
    }
}
