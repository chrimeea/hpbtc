/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class PieceMessage extends SimpleMessage {

    private int begin;
    private int index;
    private ByteBuffer piece;
    
    public PieceMessage(ByteBuffer message, Peer destination) {
        super(message.remaining(), TYPE_PIECE, destination);
        index = message.getInt();
        begin = message.getInt();
        piece = message;
    }

    public PieceMessage(int begin, int index, ByteBuffer piece, int len,
            Peer destination) {
        super(8 + len, TYPE_PIECE, destination);
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
}
