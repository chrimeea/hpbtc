package hpbtc.protocol.network;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Cristian Mocanu
 */
public class Register {

    public static enum SELECTOR_TYPE {

        TCP_READ, TCP_WRITE, UDP
    }
    private Map<Channel, RegisterOp> reg;
    private Map<SELECTOR_TYPE, Selector> selectors;

    public Register() {
        reg = new LinkedHashMap<Channel, RegisterOp>();
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

    public void registerNow(final SelectableChannel channel,
            final SELECTOR_TYPE stype, final int op, final Object peer)
            throws IOException {
        if (channel != null && channel.isOpen()) {
            final Selector selector = selectors.get(stype);
            synchronized (stype) {
                final SelectionKey sk = channel.keyFor(selector);
                RegisterOp rop = reg.get(channel);
                if (rop == null) {
                    rop = new RegisterOp(peer);
                    reg.put(channel, rop);
                }
                if (sk != null && sk.isValid() && sk.interestOps() == op) {
                    rop.getOperations().remove(stype);
                } else {
                    rop.getOperations().put(stype, op);
                    selector.wakeup();
                }
            }
        }
    }

    public void removeChannel(final Channel channel) throws IOException {
        reg.remove(channel);
        channel.close();
    }

    public Map<Channel, RegisterOp> getRegister() {
        return reg;
    }
}
