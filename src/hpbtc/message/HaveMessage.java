/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.message;

import hpbtc.Client;
import hpbtc.download.DownloadItem;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class HaveMessage extends ProtocolMessage {

    private int index;
    
    public HaveMessage() {
    }

    public void setIndex(int i) {
        index = i;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        Client client = Client.getInstance();
        DownloadItem item = client.getDownloadItem();
        index = message.getInt();
        client.getObserver().fireProcessMessageEvent(this);
        if (!peer.hasPiece(index)) {
            item.getPiece(index).addPeer(peer);
            peer.addPiece(index);
        }
        item.findAllPieces();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type HAVE, peer " + peer.getIp() + " piece " + (index + 1);
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.putInt(5);
        bb.put(ProtocolMessage.TYPE_HAVE);
        bb.putInt(index);
        return bb;
    }
}
