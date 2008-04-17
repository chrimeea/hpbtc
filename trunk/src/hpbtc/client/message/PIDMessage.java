package hpbtc.client.message;

import hpbtc.client.Client;
import hpbtc.client.observer.TorrentObserver;
import hpbtc.client.peer.PeerConnection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class PIDMessage extends ProtocolMessage {
    
    public PIDMessage() {
    }

    private void checked() {
        Client.getInstance().getObserver().firePIDOKEvent(peer);
        PeerConnection pc = peer.getConnection();
        BitfieldMessage bm = new BitfieldMessage();
        bm.setPeer(peer);
        pc.addUploadMessage(bm);
        pc.startAllTimers();
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        TorrentObserver to = Client.getInstance().getObserver();
        to.fireProcessMessageEvent(this);
        PeerConnection pc = peer.getConnection();
        if (pc != null && pc.getPeer().getId() == null) {
            String pid;
            try {
                pid = new String(message.array(), message.arrayOffset(), 20, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                return;
            }
            peer.setId(pid);
            checked();
        } else {
            ByteBuffer pid;
            try {
                pid = ByteBuffer.wrap(peer.getId().getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException e) {
                return;
            }
            if (message.equals(pid)) {
                checked();
                return;
            }
            Client.getInstance().getObserver().firePIDErrorEvent(peer);
            pc.close();
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type PID, peer " + peer.getIp();
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(20);
        bb.put(Client.getInstance().getPIDBytes());
        return bb;
    }
}
