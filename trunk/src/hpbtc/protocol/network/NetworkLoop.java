/*
 * Created on 19.10.2008
 */
package hpbtc.protocol.network;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public abstract class NetworkLoop {

    protected static Logger logger = Logger.getLogger(
            NetworkLoop.class.getName());
    protected boolean running;
    protected Register register;
    protected Register.SELECTOR_TYPE stype;
    protected Selector selector;

    public NetworkLoop(Register register) {
        this.register = register;
    }

    public int connect() throws IOException {
        selector = register.openSelector(stype);
        running = true;
        new Thread(new Runnable() {

            public void run() {
                try {
                    listen();
                } catch (Exception e) {
                    running = false;
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
                try {
                    register.closeSelector(stype);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        }).start();
        return 0;
    }

    private void listen() throws IOException, NoSuchAlgorithmException {
        while (running) {
            if (selector.select() > 0) {
                final Iterator<SelectionKey> i = selector.selectedKeys().
                        iterator();
                while (i.hasNext()) {
                    final SelectionKey key = i.next();
                    i.remove();
                    if (key.isValid()) {
                        try {
                            processKey(key);
                        } catch (IOException ioe) {
                            logger.log(Level.FINE, ioe.getLocalizedMessage(),
                                    ioe);
                            try {
                                disconnect(key);
                            } catch (IOException iOException) {
                                logger.log(Level.FINE,
                                        iOException.getLocalizedMessage(),
                                        iOException);
                            }
                        }
                    }
                }
            }
            performRegistration();
        }
    }

    protected void performRegistration() {
        synchronized (stype) {
            Map<Channel, RegisterOp> reg = register.getRegister();
            for (Channel channel : reg.keySet()) {
                if (channel != null && channel.isOpen()) {
                    try {
                        final RegisterOp rop = reg.get(channel);
                        if (rop != null) {
                            final Integer op = rop.getOperations().get(stype);
                            if (op != null) {
                                if (registerOperation(op.intValue(),
                                        rop.getPeer())) {
                                    ((SelectableChannel) channel).register(
                                            selector, op.intValue(),
                                            rop.getPeer());
                                }
                                rop.getOperations().remove(stype);
                                
                            }
                        }
                    } catch (ClosedChannelException e) {
                        logger.log(Level.FINE, e.getLocalizedMessage(), e);
                    }
                }
            }
        }
    }

    protected boolean registerOperation(int op, Object peer) {
        return true;
    }

    public void disconnect() {
        running = false;
        selector.wakeup();
    }

    protected void disconnect(SelectionKey key) throws IOException {
        key.cancel();
    }

    protected abstract void processKey(SelectionKey key) throws IOException,
            NoSuchAlgorithmException;
}
