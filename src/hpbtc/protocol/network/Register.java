package hpbtc.protocol.network;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
        final Selector s = Selector.open();
        reg.put(s, new ConcurrentLinkedQueue<RegisterOp>());
        return s;
    }

    public void disconnect(final SelectableChannel channel) {
        synchronized (channel) {
            for (Selector s : reg.keySet()) {
                SelectionKey key = channel.keyFor(s);
                if (key != null) {
                    key.cancel();
                }
            }
        }
    }

    public void registerNow(final SelectableChannel channel,
            final Selector selector, final int op, final Object peer)
            throws IOException {
        registerNow(peer, op, selector, reg.get(selector), channel);
    }

    private void registerNow(final Object peer, final int op,
            final Selector selector, final Queue<RegisterOp> registered,
            final SelectableChannel channel) throws IOException {
        if (channel != null && channel.isOpen()) {
            final SelectionKey sk = channel.keyFor(selector);
            if (sk == null || (sk.isValid() && (sk.interestOps() != op))) {
                registered.add(new RegisterOp(op, channel, peer));
                selector.wakeup();
            }
        }
    }

    public void performRegistration(final Selector selector) {
        final Queue<RegisterOp> registered = reg.get(selector);
        RegisterOp ro = registered.poll();
        while (ro != null) {
            final SelectableChannel channel = ro.getChannel();
            if (channel != null) {
                synchronized (channel) {
                    if (channel.isOpen()) {
                        try {
                            channel.register(selector, ro.getOperation(), ro.
                                    getPeer());
                        } catch (ClosedChannelException e) {
                            logger.log(Level.FINE, e.getLocalizedMessage(), e);
                        }
                    }
                }
            }
            ro = registered.poll();
        }
    }

    private class RegisterOp {

        private SelectableChannel channel;
        private Object peer;
        private int operation;

        private RegisterOp(final int op, final SelectableChannel channel,
                final Object peer) {
            this.channel = channel;
            this.operation = op;
            this.peer = peer;
        }

        private int getOperation() {
            return operation;
        }

        private SelectableChannel getChannel() {
            return channel;
        }

        private Object getPeer() {
            return peer;
        }
    }
}
