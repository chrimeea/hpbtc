package hpbtc.protocol.network;

import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
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
    private Map<Selector, Queue<RegisterOp>> reg;

    public Register() {
        reg = new HashMap<Selector, Queue<RegisterOp>>();
    }

    public Selector openSelector() throws IOException {
        Selector s = Selector.open();
        reg.put(s, new ConcurrentLinkedQueue<RegisterOp>());
        return s;
    }

    public void disconnect(final Peer peer) {
        synchronized (peer) {
            final SelectableChannel channel = peer.getChannel();
            for (Selector s : reg.keySet()) {
                SelectionKey key = channel.keyFor(s);
                if (key != null) {
                    key.cancel();
                }
            }
        }
    }

    public void registerRead(final Peer peer, final Selector selector)
            throws IOException {
        registerNow(peer, SelectionKey.OP_READ, selector, reg.get(selector));
    }

    public void registerWrite(final Peer peer, final Selector selector)
            throws IOException {
        registerNow(peer, SelectionKey.OP_WRITE, selector, reg.get(selector));
    }

    public void clearWrite(final Peer peer, final Selector selector)
            throws IOException {
        registerNow(peer, 0, selector, reg.get(selector));
    }

    private void registerNow(final Peer peer, final int op,
            final Selector selector, final Queue<RegisterOp> registered)
            throws IOException {
        SocketChannel ch = peer.getChannel();
        if (ch != null) {
            if (ch.isOpen()) {
                final SelectionKey sk = ch.keyFor(selector);
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

    public void performRegistration(final Selector selector) {
        Queue<RegisterOp> registered = reg.get(selector);
        RegisterOp ro = registered.poll();
        while (ro != null) {
            final Peer peer = ro.getPeer();
            synchronized (peer) {
                final SelectableChannel q = peer.getChannel();
                if (q != null && q.isOpen()) {
                    try {
                        q.register(selector, ro.getOperation(), ro.getPeer());
                    } catch (ClosedChannelException e) {
                        logger.log(Level.FINE, e.getLocalizedMessage(), e);
                    }
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
