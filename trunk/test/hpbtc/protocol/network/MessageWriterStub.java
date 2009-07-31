/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageWriter;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageWriterStub extends MessageWriter {

    public MessageWriterStub(final Register r) {
        super(r, null, null, null);
    }

    @Override
    public void connect(final Peer peer) throws IOException {
        if (peer.getChannel() == null && !connectPeer(peer)) {
            ((SocketChannel) peer.getChannel()).finishConnect();
        }
        register.registerNow((SelectableChannel) peer.getChannel(),
                Register.SELECTOR_TYPE.TCP_WRITE, SelectionKey.OP_WRITE, peer);
    }
}
