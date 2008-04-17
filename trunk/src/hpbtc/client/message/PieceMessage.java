/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.client.message;

import hpbtc.client.Client;
import hpbtc.client.download.DownloadItem;
import hpbtc.client.piece.Piece;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimerTask;

/**
 * @author chris
 *
 */
public class PieceMessage extends BlockMessage {
    
    public PieceMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        index = message.getInt();
        begin = message.getInt();
        length = message.remaining();
        Client client = Client.getInstance();
        client.getObserver().fireProcessMessageEvent(this);
        final DownloadItem item = client.getDownloadItem();
        final Piece pe = item.getPiece(index);
        if (pe.requestDone(this)) {
            peer.requestDone();
        }
        if (!pe.haveAll(begin, length)) {
            item.getRateTimer().schedule(new TimerTask() {
                public void run() {
                    if (pe.savePiece(begin, message)) {
                        item.broadcastHave(index);
                    }
                    item.findNextPiece();
                }
            }, 0);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type PIECE, peer " + peer.getIp() + " index " + (index + 1) + " begin " + begin + " length " + length;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(13 + length);
        DownloadItem item = Client.getInstance().getDownloadItem();
        bb.putInt(9 + length);
        bb.put(ProtocolMessage.TYPE_PIECE);
        bb.putInt(index);
        bb.putInt(begin);
        ByteBuffer x = item.getPiece(index).getPiece(begin, length);
        x.rewind();
        bb.put(x);
        return bb;
    }
}
