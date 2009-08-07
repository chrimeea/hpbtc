package hpbtc.protocol.network;

import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Register {

    public static enum SELECTOR_TYPE {

        TCP_READ, TCP_WRITE, UDP
    }
    protected static Logger logger = Logger.getLogger(
            Register.class.getName());
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
                final Peer p = (Peer) peer;
                if (sk != null && sk.isValid() && sk.interestOps() == op) {
                    rop.getOperations().remove(stype);
                } else if (((op & SelectionKey.OP_READ) != 0 && !p.isReading())
                        || ((op & SelectionKey.OP_WRITE) != 0 && !p.isWriting())) {
                    rop.getOperations().put(stype, op);
                    selector.wakeup();
                }
            }
        }
    }

    public void removeChannel(final Channel channel) throws IOException {
        channel.close();
        reg.remove(channel);
    }

    public Map<Channel, RegisterOp> getRegister() {
        return reg;
    }
}
