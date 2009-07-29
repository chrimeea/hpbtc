package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageWriter;
import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

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
            SelectableChannel pChannel = (SelectableChannel) peer.getChannel();
            register.registerNow(pChannel, Register.SELECTOR_TYPE.TCP_READ,
                    SelectionKey.OP_READ, peer);
            register.registerNow(pChannel, Register.SELECTOR_TYPE.TCP_WRITE,
                    SelectionKey.OP_WRITE, peer);
            writer.connect(peer);
            logger.info("Connected to " + peer);
        }
        if (key.isWritable() && ch.isConnected()) {
            try {
                if (!writeNext(peer)) {
                    register.registerNow((SelectableChannel) peer.getChannel(),
                            Register.SELECTOR_TYPE.TCP_WRITE, 0, peer);
                }
            } catch (InvalidPeerException ex) {
                throw new IOException(ex);
            }
        }
    }

    private boolean writeNext(Peer peer) throws IOException,
            InvalidPeerException {
        while (writer.writeNext(peer));
        return writer.hasMoreMessages(peer);
    }

    @Override
    protected boolean registerOperation(final int op, final Object peer) {
        final Peer p = (Peer) peer;
        final SocketChannel ch = (SocketChannel) p.getChannel();
        try {
            if (ch != null && ch.isConnected() &&
                    (op & SelectionKey.OP_WRITE) != 0) {
                return writeNext(p);
            }
        } catch (Exception ex) {
            try {
                writer.disconnect(p);
            } catch (Exception ex1) {
            }
            logger.log(Level.INFO, ex.getLocalizedMessage(), ex);
            return false;
        }
        return true;
    }

    @Override
    protected void disconnect(final SelectionKey key) throws IOException {
        final Peer peer = (Peer) key.attachment();
        try {
            writer.disconnect(peer);
        } catch (InvalidPeerException ex) {
            throw new IOException("Invalid peer " + peer);
        }
    }
}
