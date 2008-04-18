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
public class PieceMessage extends BlockMessage {
    
    private ByteBuffer piece;
    
    public PieceMessage() {
    }

    public PieceMessage(int begin, int index, int length, ByteBuffer piece) {
        super(begin, index, length);
        this.piece = piece;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message,MessageProcessor processor) {
        index = message.getInt();
        begin = message.getInt();
        length = message.remaining();
        piece = message;
        processor.process(this);
        super.process(message, processor);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type PIECE";
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(13 + length);
        bb.putInt(9 + length);
        bb.put(ProtocolMessage.TYPE_PIECE);
        bb.putInt(index);
        bb.putInt(begin);
        bb.put(piece);
        return bb;
    }

    public ByteBuffer getPiece() {
        return piece;
    }
}
