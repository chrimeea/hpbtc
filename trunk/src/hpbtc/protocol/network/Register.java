package hpbtc.protocol.network;

import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Register {

    private static Logger logger = Logger.getLogger(Register.class.getName());
    private Queue<RegisterOp> registeredRead;
    private Queue<RegisterOp> registeredWrite;
    private Selector reader;
    private Selector writer;

    public Register() {
        registeredRead = new ConcurrentLinkedQueue<RegisterOp>();
        registeredWrite = new ConcurrentLinkedQueue<RegisterOp>();
    }

    Selector openReadSelector() throws IOException {
        reader = Selector.open();
        return reader;
    }

    Selector openWriteSelector() throws IOException {
        writer = Selector.open();
        return writer;
    }

    public void disconnect(final Peer peer) {
        SelectableChannel channel = peer.getChannel();
        SelectionKey key = channel.keyFor(reader);
        if (key != null) {
            key.cancel();
        }
        key = channel.keyFor(writer);
        if (key != null) {
            key.cancel();
        }
    }

    public void registerRead(final Peer peer) throws IOException {
        registerNow(peer, SelectionKey.OP_READ, reader, registeredRead);
    }

    public void registerWrite(final Peer peer) throws IOException {
        registerNow(peer, SelectionKey.OP_WRITE, writer, registeredWrite);
    }

    public void clearWrite(final Peer peer) throws IOException {
        registerNow(peer, 0, writer, registeredWrite);
    }

    private void registerNow(final Peer peer, final int op,
            final Selector selector, final Queue<RegisterOp> registered)
            throws IOException {
        SocketChannel ch = peer.getChannel();
        if (ch != null) {
            if (ch.isOpen()) {
                SelectionKey sk = ch.keyFor(selector);
                if (sk == null || (sk.isValid() && (sk.interestOps() & op) == 0)) {
                    registered.add(new RegisterOp(op, peer));
                    selector.wakeup();
                }
            }
        } else {
            ch = SocketChannel.open();
            ch.configureBlocking(false);
            peer.setChannel(ch);
            if (ch.connect(peer.getAddress())) {
                registered.add(new RegisterOp(op, peer));
            } else {
                registered.add(
                        new RegisterOp(SelectionKey.OP_CONNECT | op, peer));
            }
            selector.wakeup();
        }
    }

    void performReadRegistration() {
        performRegistration(reader, registeredRead);
    }

    void performWriteRegistration() {
        performRegistration(writer, registeredWrite);
    }

    private void performRegistration(final Selector selector,
            final Queue<RegisterOp> registered) {
        RegisterOp ro = registered.poll();
        while (ro != null) {
            SelectableChannel q = ro.getPeer().getChannel();
            if (q.isOpen()) {
                try {
                    q.register(selector, ro.getOperation(), ro.getPeer());
                } catch (ClosedChannelException e) {
                    logger.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
            ro = registered.poll();
        }
    }

    private class RegisterOp {

        private Peer peer;
        private int operation;

        private RegisterOp(final int op, final Peer peer) {
            this.operation = op;
            this.peer = peer;
        }

        private int getOperation() {
            return operation;
        }

        private Peer getPeer() {
            return peer;
        }
    }
}
