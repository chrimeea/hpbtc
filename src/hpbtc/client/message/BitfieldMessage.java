/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.client.message;

import hpbtc.client.Client;
import hpbtc.client.download.DownloadItem;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class BitfieldMessage extends ProtocolMessage {

    public BitfieldMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        Client client = Client.getInstance();
        DownloadItem item = client.getDownloadItem();
        peer.removePieces();
        int j = 0;
        int k = 0;
        int l = message.remaining();
        for (int i = 0; i < l; i++) {
            byte bit = message.get();
            byte c = (byte) 128;
            for (int p = 0; p < 8; p++) {
                if ((bit & c) == c) {
                    item.getPiece(j).addPeer(peer);
                    peer.addPiece(j);
                    k++;
                }
                bit <<= 1;
                j++;
                if (j > item.getTotalPieces()) {
                    break;
                }
            }
        }
        client.getObserver().fireProcessMessageEvent(this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type BITFIELD, peer " + peer.getIp();
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client client = Client.getInstance();
        DownloadItem item = client.getDownloadItem();
        int n = 1 + item.getTotalPieces() / 8;
        if (item.getTotalPieces() % 8 > 0) {
            n++;
        }
        ByteBuffer bb = ByteBuffer.allocate(n + 4);
        bb.putInt(n);
        byte x = ProtocolMessage.TYPE_BITFIELD;
        byte y = (byte) -128;
        boolean hasp = false;
        for (int i = 0; i < item.getTotalPieces(); i++) {
            if (i % 8 == 0) {
                bb.put(x);
                x = 0;
                y = (byte) -128;
            }
            if (item.getPiece(i).isComplete()) {
                x |= y;
                hasp = true;
            }
            y >>= 1;
            if (y < 0) {
                y ^= (byte) -128;
            }
        }
        if (hasp) {
            if (y != 0) {
                bb.put(x);
            }
            client.getObserver().fireSendMessageEvent(this);
            return bb;
        }
        return ByteBuffer.allocate(0);
    }
}
