package hpbtc.message;

import hpbtc.Client;
import hpbtc.download.DownloadItem;
import hpbtc.observer.TorrentObserver;
import hpbtc.peer.PeerConnection;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HandshakeMessage extends ProtocolMessage {

    public HandshakeMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        TorrentObserver to = Client.getInstance().getObserver();
        to.fireProcessMessageEvent(this);
        message.limit(20);
        ByteBuffer h = ByteBuffer.allocate(20);
        getProtocol(h);
        h.rewind();
        PeerConnection pc = peer.getConnection();
        if (message.equals(h)) {
            message.limit(48);
            message.position(28);
            DownloadItem i = Client.getInstance().getDownloadItem();
            if (message.equals(ByteBuffer.wrap(i.getInfoHash()))) {
                to.fireHandshakeOKEvent(peer);
                if (!pc.isHandshakeSent()) {
                    PIDMessage pm = new PIDMessage();
                    pm.setPeer(peer);
                    pc.addUploadMessage(this);
                    pc.addUploadMessage(pm);
                    pc.setHandshakeSent(true);
                }
                return;
            }
        }
        to.fireHandshakeErrorEvent(peer);
        pc.close();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type HANDSHAKE, peer " + peer.getIp();
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(48);
        getProtocol(bb);
        for (int i = 0; i < 8; i++) {
            bb.put((byte) 0);
        }
        bb.put(Client.getInstance().getDownloadItem().getInfoHash());
        return bb;
    }
    
    private void getProtocol(ByteBuffer pr) {
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes());        
    }
}
