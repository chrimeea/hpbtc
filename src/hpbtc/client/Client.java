/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.client;

import hpbtc.client.DownloadItem;
import hpbtc.client.observer.TorrentObserver;
import hpbtc.client.peer.Peer;
import hpbtc.client.peer.PeerConnection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class Client {

    public static final int REQUEST_PIECE_LENGTH = 32768;
    public static final int MIN_PORT = 6881;
    public static final int MAX_PORT = 6999;
    
    private static Logger logger = Logger.getLogger(Client.class.getName());
    
    private DownloadItem item;
    private ServerSocketChannel serverCh;
    private Selector selector;
    private static Client instance;
    private byte[] peerId;
    private Random r;
    private Queue<RegisterOp> registered;
    private TorrentObserver tobs;
    
    private Client() throws IOException {
        registered = new ConcurrentLinkedQueue<RegisterOp>();
        r = new Random();
        peerId = generateId();
    }
    
    public void setObserver(TorrentObserver ob) {
        tobs = ob;
    }
    
    /**
     * @return
     */
    public TorrentObserver getObserver() {
        return tobs;
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
                logger.warning("Port " + port + " is not available");
                port++;
            }
        }
        if (port > MAX_PORT) {
            throw new IOException("No ports available");
        } else {
            tobs.fireServerStartedEvent(port);
            serverCh.configureBlocking(false);
            selector = Selector.open();
            serverCh.register(selector, SelectionKey.OP_ACCEPT, null);
        }        
    }

    /**
     * @return
     */
    public byte[] getPIDBytes() {
        return peerId;
    }
    
    /**
     * @return
     */
    public String getPID() {
        String s;
        try {
            s = new String(peerId, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            s = null;
            logger.severe("ISO-8859-1 is not available");
        }
        return s;
    }

    private byte[] generateId() {
        byte[] pid = new byte[20];
        pid[0] = '-';
        pid[1] = 'H';
        pid[2] = 'P';
        pid[3] = '0';
        pid[4] = '1';
        pid[5] = '0';
        pid[6] = '0';
        pid[7] = '-';
        for (int i = 8; i < 20; i++) {
            pid[i] = (byte) (r.nextInt(256) - 128);
        }
        return pid;
    }
    
    /**
     * @return
     */
    public static Client getInstance() {
        if (instance == null) {
            try {
                instance = new Client();
            } catch (IOException e) {
                logger.severe("Client not started " + e.getMessage());
            }
        }
        return instance;
    }

    /**
     * @return
     */
    public DownloadItem getDownloadItem() {
        return item;
    }

    /**
     * @param torrent
     */
    public void setDownload(String torrent) throws IOException {
        item = new DownloadItem(torrent);
    }

    /**
     * @return
     */
    public int getPort() {
        return serverCh.socket().getLocalPort();
    }

    public void registerNow(PeerConnection pc, int op) {
        RegisterOp o = new RegisterOp(pc, op);
        registered.add(o);
        selector.wakeup();
    }

    public void listen() {
        item.startDownload();
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
                PeerConnection po = ro.getPeerConnection();
                SocketChannel q = po.getChannel();
                SelectionKey w = q.keyFor(selector);
                try {
                    if (w != null && w.isValid()) {
                        q.register(selector, w.interestOps() | ro.getOp(), po);
                    } else if (w == null) {
                        q.register(selector, ro.getOp(), po);
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
                        PeerConnection con = (PeerConnection) key.attachment();
                        SocketChannel ch = con == null ? null : con.getChannel();
                        if (key.isReadable()) {
                            if (con.getChannel().isConnected()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_READ, con);
                                con.readMessage();
                                ch.register(selector, key.interestOps() | SelectionKey.OP_READ, con);
                            }
                        }
                        if (key.isWritable()) {
                            ch.register(selector, key.interestOps() & ~SelectionKey.OP_WRITE, con);
                            if (!con.isUploadEmpty()) {
                                con.writeNext();
                                if (!con.isUploadEmpty()) {
                                    ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE, con);
                                }
                            }
                        }
                        if (key.isAcceptable()) {
                            SocketChannel chan = serverCh.accept();
                            Socket s = chan.socket();
                            chan.configureBlocking(false);
                            Peer p = new Peer(s.getInetAddress().getHostAddress(), s.getPort(), null);
                            if (!item.removePeerOrder(p)) {
                                Peer pp = item.findPeer(p);
                                if (pp != null) {
                                    s.close();
                                    continue;
                                } else if (pp == null) {
                                    item.addPeer(p);
                                }
                            }
                            PeerConnection pc = new PeerConnection(p);
                            pc.setChannel(chan);
                            chan.register(selector, SelectionKey.OP_READ, pc);
                            tobs.fireIncomingConnectionEvent(s.getInetAddress().getHostAddress());
                        } else if (key.isConnectable()) {
                            final Peer p = con.getPeer();
                            try {
                                if (con.getChannel().finishConnect()) {
                                    TimerTask tt = con.getConTimer();
                                    if (tt != null) {
                                        tt.cancel();
                                    }
                                    ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT, con);
                                    con.finishConnect();
                                    item.getRateTimer().schedule(
                                        new TimerTask() {
                                            public void run() {
                                                item.initiateConnections(1);
                                            }
                                        }, 0);
                                }
                            } catch (IOException e) {
                                tobs.fireConnectionFailedEvent(p, e);
                                con.close();
                                item.getRateTimer().schedule(
                                    new TimerTask() {
                                        public void run() {
                                            item.initiateConnections(1);
                                        }
                                    }, 0);
                            }
                        }
                    }
                } catch (CancelledKeyException e) {
                    logger.warning("A key was unexpectedly canceled");
                } catch (ClosedChannelException e) {
                    logger.warning("A channel was unexpectedly closed");
                } catch (IOException e) {
                    logger.warning("Error while multiplexing " + e.getMessage());
                }
            }
        }
    }
    
    private class RegisterOp {
        
        private PeerConnection pc;
        private int op;
        
        private RegisterOp(PeerConnection pc, int op) {
            this.pc = pc;
            this.op = op;
        }
        
        private PeerConnection getPeerConnection() {
            return pc;
        }
        
        private int getOp() {
            return op;
        }
    }
}
