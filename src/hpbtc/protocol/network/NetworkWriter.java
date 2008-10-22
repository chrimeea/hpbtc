package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageWriter;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Cristian Mocanu
 */
public class NetworkWriter extends NetworkLoop {

    private MessageWriter writer;

    public NetworkWriter(final MessageWriter writer, final Register register) {
        super(register);
        this.writer = writer;
    }

    @Override
    public int connect() throws IOException {
        selector = register.openSelector();
        final int port = super.connect();
        writer.setWriteSelector(selector);
        return port;
    }

    protected void processKey(final SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        final SocketChannel ch = (SocketChannel) key.channel();
        final Peer peer = (Peer) key.attachment();
        if (key.isConnectable() && ch.finishConnect()) {
            writer.connect(peer);
            logger.info("Connected to " + peer);
        } else if (key.isWritable()) {
            writer.writeNext(peer);
        }
    }

    @Override
    protected void disconnect(final SelectionKey key) throws IOException {
        writer.disconnect((Peer) key.attachment());
    }
}
