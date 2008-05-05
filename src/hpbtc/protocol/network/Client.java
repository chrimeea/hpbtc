/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
import util.IOUtil;

/**
 * @author chris
 *
 */
public class Client {

    public static final int MIN_PORT = 6881;
    public static final int MAX_PORT = 6999;
    private ServerSocketChannel serverCh;
    private Selector selector;
    private Queue<RegisterOp> registered;
    private Map<InetSocketAddress, Queue<ByteBuffer>> messagesToSend;
    private Queue<ClientProtocolMessage> messagesReceived;
    private Map<InetSocketAddress, SocketChannel> openChannels;
    private ByteBuffer current;

    public Client() {
        messagesReceived = new ConcurrentLinkedQueue<ClientProtocolMessage>();
        messagesToSend = new ConcurrentHashMap<InetSocketAddress, Queue<ByteBuffer>>();
        openChannels = new HashMap<InetSocketAddress, SocketChannel>();
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
        if (ch != null) {
            if ((ch.keyFor(selector).interestOps() & op) == 0) {
                registered.add(new RegisterOp(op, ch));
                selector.wakeup();
            }
        } else {
            ch = SocketChannel.open();
            ch.configureBlocking(false);
            if (ch.connect(peer)) {
                openChannels.put(peer, ch);
                registerNow(peer, op);
            } else {
                registerNow(peer, SelectionKey.OP_CONNECT | op);
            }
        }
    }

    private void readMessage(SocketChannel ch) throws IOException {
        int i = IOUtil.readFromChannel(ch, current);
        byte[] b = new byte[i];
        current.get(b);
        current.clear();
        messagesReceived.add(new ClientProtocolMessage(IOUtil.getAddress(ch), b));
        notify();
    }

    private void writeNext(SocketChannel ch) throws IOException {
        Queue<ByteBuffer> q = messagesToSend.get(IOUtil.getAddress(ch));
        ByteBuffer b;
        do {
            b = q.poll();
            IOUtil.writeToChannel(ch, b);
        } while (b.remaining() == 0 && !q.isEmpty());
    }

    public ClientProtocolMessage takeMessage() {
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

    public void listen() throws IOException {
        while (true) {
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
                        writeNext(ch);
                        if (!messagesToSend.isEmpty()) {
                            ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE);
                        }
                    }
                    if (key.isAcceptable()) {
                        SocketChannel chan = serverCh.accept();
                        chan.configureBlocking(false);
                        openChannels.put(IOUtil.getAddress(chan), chan);
                        chan.register(selector, SelectionKey.OP_READ);
                    } else if (key.isConnectable() && ch.finishConnect()) {
                        openChannels.put(IOUtil.getAddress(ch), ch);
                        ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT);
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
