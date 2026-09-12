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
public class PieceMessage extends SimpleMessage {

    private int begin;
    private int index;
    private ByteBuffer piece;

    public PieceMessage(final ByteBuffer message, final Peer destination) {
        super(message.remaining(), TYPE_PIECE, destination);
        index = message.getInt();
        begin = message.getInt();
        piece = message;
    }

    public PieceMessage(final int begin, final int index, final int len,
            final Peer destination) {
        super(8 + len, TYPE_PIECE, destination);
        this.begin = begin;
        this.index = index;
    }

    public void setPiece(ByteBuffer piece) {
        this.piece = piece;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        final ByteBuffer bb = super.send();
        bb.putInt(index);
        bb.putInt(begin);
        bb.put(piece);
        return bb;
    }

    public int getLength() {
        return messageLength - 9;
    }

    public ByteBuffer getPiece() {
        return piece;
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    @Override
    public String toString() {
        return super.toString() + ", Index: " + index + ", Begin: " + begin;
    }
}
