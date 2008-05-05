/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class Client {

    public static final int MIN_PORT = 6881;
    public static final int MAX_PORT = 6999;
    private static Logger logger = Logger.getLogger(Client.class.getName());
    
    private ServerSocketChannel serverCh;
    private Selector selector;
    private Queue<RegisterOp> registered;
    private Map<InetSocketAddress, Queue<byte[]>> messagesToSend;
    private long uploaded;
    private long downloaded;
    private Queue<ClientProtocolMessage> messagesReceived;
    private Map<InetSocketAddress, SocketChannel> openChannels;

    public Client() {
        messagesReceived = new ConcurrentLinkedQueue<ClientProtocolMessage>();
        messagesToSend = new ConcurrentHashMap<InetSocketAddress, Queue<byte[]>>();
        openChannels = new HashMap<InetSocketAddress, SocketChannel>();
        registered = new ConcurrentLinkedQueue<RegisterOp>();
    }
    
    public void connect() throws IOException {
        int port = MIN_PORT;
        try {
            serverCh = ServerSocketChannel.open();
        } catch (IOException e) {
            logger.severe("Can not open server socket");
            return;
        }
        while (port <= MAX_PORT) {
            try {
                InetSocketAddress isa = new InetSocketAddress(
                        InetAddress.getLocalHost(), port);
                serverCh.socket().bind(isa);
                break;
            } catch (IOException e) {
                port++;
            }
        }
        if (port > MAX_PORT) {
            throw new IOException("No ports available");
        } else {
            logger.info("server started " + port);
            serverCh.configureBlocking(false);
            selector = Selector.open();
            serverCh.register(selector, SelectionKey.OP_ACCEPT, null);
        }
    }

    /**
     * @return
     */
    public int getPort() {
        return serverCh.socket().getLocalPort();
    }

    private void registerNow(InetSocketAddress peer, int op) throws IOException {
        SocketChannel ch = openChannels.get(peer);
        if (ch != null && (ch.keyFor(selector).interestOps() & op) == 0) {
            registered.add(new RegisterOp(op, ch));
            selector.wakeup();
        }
    }
    
    public ClientProtocolMessage takeMessage() {
        return messagesReceived.poll();
    }
    
    public void postMessage(InetSocketAddress peer, byte[] message) {
        Queue<byte[]> q = messagesToSend.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<byte[]>();
            messagesToSend.put(peer, q);
        }
        q.add(message);
        registerNow(peer, SelectionKey.OP_WRITE);
    }
    
    public void listen() {
        while (true) {
            int n;
            try {
                n = selector.select();
            } catch (ClosedSelectorException e) {
                break;
            } catch (IOException e) {
                continue;
            }
            RegisterOp ro = registered.poll();
            while (ro != null) {
                SelectableChannel q = ro.channel;
                SelectionKey w = q.keyFor(selector);
                try {
                    if (w != null && w.isValid()) {
                        q.register(selector, w.interestOps() | ro.operation);
                    } else if (w == null) {
                        q.register(selector, ro.operation);
                    }
                } catch (CancelledKeyException e) {
                    logger.warning("A key was unexpectedly canceled");
                } catch (ClosedChannelException e) {
                    logger.warning("A channel was unexpectedly closed");
                }
                ro = registered.poll();
            }
            if (n == 0) {
                continue;
            }
            Iterator<SelectionKey> i = selector.selectedKeys().iterator();
            while (i.hasNext()) {
                SelectionKey key = i.next();
                i.remove();
                try {
                    if (key.isValid()) {
                        SocketChannel ch = (SocketChannel) key.channel();
                        if (key.isReadable()) {
                            if (ch.isConnected()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_READ);
                                readMessage(ch);
                                ch.register(selector, key.interestOps() | SelectionKey.OP_READ);
                            }
                        }
                        if (key.isWritable()) {
                            ch.register(selector, key.interestOps() & ~SelectionKey.OP_WRITE);
                            if (writeNext(ch)) {
                                ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE);
                            }
                        }
                        if (key.isAcceptable()) {
                            SocketChannel chan = serverCh.accept();
                            Socket s = chan.socket();
                            chan.configureBlocking(false);
                            chan.register(selector, SelectionKey.OP_READ);
                            logger.info("incoming connection " + s.getInetAddress().getHostAddress());
                        } else if (key.isConnectable()) {
                            try {
                                if (ch.finishConnect()) {
                                    ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT);
                                }
                            } catch (IOException e) {
                                logger.info("connection failed " + e.getMessage());
                                ch.socket().close();
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    private class RegisterOp {

        private SelectableChannel channel;
        private int operation;

        private RegisterOp(int op, SelectableChannel channel) {
            this.operation = op;
            this.channel = channel;
        }
    }
}
