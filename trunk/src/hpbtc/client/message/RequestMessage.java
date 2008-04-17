package hpbtc.client.message;

import hpbtc.client.Client;
import hpbtc.client.download.DownloadItem;
import hpbtc.client.peer.PeerConnection;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class RequestMessage extends BlockMessage implements Cloneable {
    
    public RequestMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        index = message.getInt();
        begin = message.getInt();
        length = message.getInt();
        Client client = Client.getInstance();
        client.getObserver().fireSendMessageEvent(this);
        DownloadItem item = client.getDownloadItem();
        PeerConnection c = peer.getConnection();
        if (c != null && !peer.isChokedHere() && item.getPiece(index).isComplete()) {
            PieceMessage pm = new PieceMessage();
            pm.setPeer(peer);
            pm.setIndex(index);
            pm.setBegin(begin);
            pm.setLength(length);
            c.addUploadMessage(pm);
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type REQUEST, peer " + peer.getIp() + " index " + (index + 1) + " begin " + begin + " length " + length;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.putInt(13);
        bb.put(ProtocolMessage.TYPE_REQUEST);
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);
        return bb;
    }
    
    public Object clone() throws CloneNotSupportedException {
        RequestMessage rm = new RequestMessage();
        rm.setIndex(index);
        rm.setBegin(begin);
        rm.setLength(length);
        return rm;
    }
}
