/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import util.IOUtil;

/**
 * @author chris
 *
 */
public class Network {

    public static final int MIN_PORT = 6881;
    public static final int MAX_PORT = 6999;
    private static Logger logger = Logger.getLogger(Network.class.getName());
    private ServerSocketChannel serverCh;
    private Selector selector;
    private Queue<RegisterOp> registered;
    private Map<InetSocketAddress, Queue<ByteBuffer>> messagesToSend;
    private Queue<RawMessage> messagesReceived;
    private ByteBuffer current;
    private boolean isRunning;

    public Network() {
        messagesReceived = new ConcurrentLinkedQueue<RawMessage>();
        messagesToSend = new ConcurrentHashMap<InetSocketAddress, Queue<ByteBuffer>>();
        registered = new ConcurrentLinkedQueue<RegisterOp>();
        current = ByteBuffer.allocate(16384);
    }

    public void connect() throws IOException {
        int port = MIN_PORT;
        serverCh = ServerSocketChannel.open();
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
            serverCh.configureBlocking(false);
            selector = Selector.open();
            serverCh.register(selector, SelectionKey.OP_ACCEPT);
        }
        isRunning = true;
        new Thread(new Runnable() {

            public void run() {
                try {
                    listen();
                } catch (IOException e) {
                    isRunning = false;
                    logger.warning(e.getLocalizedMessage());
                }
                try {
                    selector.close();
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
                }
                try {
                    serverCh.close();
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void disconnect() {
        isRunning = false;
    }

    /**
     * @return
     */
    public int getPort() {
        return serverCh.socket().getLocalPort();
    }

    private SocketChannel findByAddress(InetSocketAddress peer) {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey k : keys) {
            SelectableChannel ch = k.channel();
            if (ch instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) ch;
                if (((InetSocketAddress) sc.socket().getRemoteSocketAddress()).equals(peer)) {
                    return sc;
                }
            }
        }
        return null;
    }

    private void registerNow(InetSocketAddress peer, int op) throws IOException {
        SocketChannel ch = findByAddress(peer);
        if (ch != null) {
            if ((ch.keyFor(selector).interestOps() & op) == 0) {
                registered.add(new RegisterOp(op, ch));
            }
        } else {
            ch = SocketChannel.open();
            ch.configureBlocking(false);
            if (ch.connect(peer)) {
                registered.add(new RegisterOp(op, ch));
            } else {
                registered.add(new RegisterOp(SelectionKey.OP_CONNECT | op, ch));
            }
        }
        selector.wakeup();
    }

    public boolean hasUnreadMessages() {
        return !messagesReceived.isEmpty();
    }

    private void readMessage(SocketChannel ch) {
        int i;
        try {
            i = IOUtil.readFromChannel(ch, current);
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
            i = current.position();
            try {
                ch.close();
            } catch (IOException x) {
                logger.warning(x.getLocalizedMessage());
            }
        }
        if (i > 0) {
            byte[] b = new byte[i];
            current.rewind();
            current.get(b);
            current.clear();
            messagesReceived.add(new RawMessage(IOUtil.getAddress(ch), b));
            synchronized (this) {
                notify();
            }
        }
    }

    public void closeConnection(InetSocketAddress address) throws IOException {
        SocketChannel ch = findByAddress(address);
        if (ch != null) {
            ch.close();
        }
    }
    
    private void writeNext(SocketChannel ch) {
        Queue<ByteBuffer> q = messagesToSend.get(IOUtil.getAddress(ch));
        try {
            do {
                ByteBuffer b = q.poll();
                do {
                    IOUtil.writeToChannel(ch, b);
                } while (b.remaining() == 0);
            } while (!q.isEmpty());
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
            try {
                ch.close();
            } catch (IOException x) {
                logger.warning(x.getLocalizedMessage());
            }
        }
    }

    public RawMessage takeMessage() {
        return messagesReceived.poll();
    }

    public void postMessage(InetSocketAddress peer, ByteBuffer message) throws IOException {
        Queue<ByteBuffer> q = messagesToSend.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<ByteBuffer>();
            messagesToSend.put(peer, q);
        }
        q.add(message);
        registerNow(peer, SelectionKey.OP_WRITE);
    }

    private void listen() throws IOException {
        while (isRunning) {
            int n = selector.select();
            RegisterOp ro = registered.poll();
            while (ro != null) {
                SelectableChannel q = ro.channel;
                SelectionKey w = q.keyFor(selector);
                if (w != null && w.isValid()) {
                    q.register(selector, w.interestOps() | ro.operation);
                } else if (w == null) {
                    q.register(selector, ro.operation);
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
                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        SocketChannel chan = serverCh.accept();
                        chan.configureBlocking(false);
                        chan.register(selector, SelectionKey.OP_READ);
                    } else {
                        SocketChannel ch = (SocketChannel) key.channel();
                        if (key.isConnectable() && ch.finishConnect()) {
                            ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT);
                        } else {
                            if (key.isReadable()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_READ);
                                readMessage(ch);
                                if (ch.isOpen()) {
                                    ch.register(selector, key.interestOps() | SelectionKey.OP_READ);
                                }
                            }
                            if (key.isValid() && key.isWritable()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_WRITE);
                                writeNext(ch);
                                if (ch.isOpen() && !messagesToSend.isEmpty()) {
                                    ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE);
                                }
                            }
                        }
                    }
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
