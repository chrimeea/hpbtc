package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

/**
 *
 * @author Cristian Mocanu
 */
public class KeepAliveMessage extends SimpleMessage {

    public KeepAliveMessage(final Peer destination) {
        this.destination = destination;
        this.disc =  SimpleMessage.TYPE_KEEPALIVE;
        this.messageLength = 0;
    }

    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(0);
        return super.send();
    }
}
