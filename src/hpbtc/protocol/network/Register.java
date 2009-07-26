package hpbtc.protocol.network;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Register {

    public static enum SELECTOR_TYPE {

        TCP_READ, TCP_WRITE, UDP
    }
    private static Logger logger = Logger.getLogger(Register.class.getName());
    private Map<Channel, RegisterOp> reg;
    private Map<SELECTOR_TYPE, Selector> selectors;

    public Register() {
        reg = new ConcurrentHashMap<Channel, RegisterOp>();
        selectors = new ConcurrentHashMap<SELECTOR_TYPE, Selector>();
    }

    public void closeSelector(SELECTOR_TYPE stype) throws IOException {
        selectors.remove(stype).close();
    }

    public Selector openSelector(SELECTOR_TYPE stype) throws IOException {
        final Selector s = Selector.open();
        selectors.put(stype, s);
        return s;
    }

    public void disconnect(final SelectableChannel channel) {
        final RegisterOp m = reg.get(channel);
        for (SELECTOR_TYPE s : m.operations.keySet()) {
            SelectionKey key = channel.keyFor(selectors.get(s));
            if (key != null) {
                key.cancel();
            }
        }
    }

    public void registerNow(final SelectableChannel channel,
            final SELECTOR_TYPE stype, final int op, final Object peer)
            throws IOException {
        if (channel != null && channel.isOpen()) {
            Selector selector = selectors.get(stype);
            final SelectionKey sk = channel.keyFor(selector);
            RegisterOp rop = reg.get(channel);
            if (rop == null) {
                rop = new RegisterOp(peer);
                reg.put(channel, rop);
            }
            Integer c = rop.operations.get(stype);
            if (c != null && sk != null &&
                    sk.isValid() && sk.interestOps() == op) {
                rop.operations.remove(stype);
            } else if (c == null || c != op) {
                rop.operations.put(stype, op);
                if (sk == null || (sk.isValid() && sk.interestOps() != op)) {
                    selector.wakeup();
                }
            }
        }
    }

    public void performRegistration(final SELECTOR_TYPE stype) {
        final Selector selector = selectors.get(stype);
        for (Channel channel : reg.keySet()) {
            if (channel != null && channel.isOpen()) {
                try {
                    RegisterOp rop = reg.get(channel);
                    if (rop != null) {
                        Integer op = rop.operations.get(stype);
                        if (op != null) {
                            ((SelectableChannel) channel).register(selector,
                                    op.intValue(), rop.peer);
                            rop.operations.remove(stype);
                        }
                    }
                } catch (ClosedChannelException e) {
                    logger.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        }
    }

    private class RegisterOp {

        private Map<SELECTOR_TYPE, Integer> operations;
        private Object peer;

        private RegisterOp(final Object peer) {
            operations = new ConcurrentHashMap<SELECTOR_TYPE, Integer>();
            this.peer = peer;
        }
    }
}
