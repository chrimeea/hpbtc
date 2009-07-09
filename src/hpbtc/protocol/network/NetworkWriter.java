package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageWriter;
import hpbtc.protocol.torrent.InvalidPeerException;
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
        this.stype = Register.SELECTOR_TYPE.TCP_WRITE;
        this.writer = writer;
    }

    protected void processKey(final SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        final SocketChannel ch = (SocketChannel) key.channel();
        final Peer peer = (Peer) key.attachment();
        if (key.isConnectable() && ch.finishConnect()) {
            writer.connect(peer);
            logger.info("Connected to " + peer);
        } else if (key.isWritable()) {
            try {
                writer.writeNext(peer);
            } catch (InvalidPeerException ex) {
                throw new IOException();
            }
        }
    }

    @Override
    protected void disconnect(final SelectionKey key) throws IOException {
        Peer peer = (Peer) key.attachment();
        try {
            writer.disconnect(peer);
        } catch (InvalidPeerException ex) {
            throw new IOException("Invalid peer " + peer);
        }
    }
}
