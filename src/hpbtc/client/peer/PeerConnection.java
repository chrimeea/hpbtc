package hpbtc.client.peer;

import hpbtc.client.Client;
import hpbtc.client.DownloadItem;
import hpbtc.client.message.CancelMessage;
import hpbtc.client.message.HandshakeMessage;
import hpbtc.client.message.IdleMessage;
import hpbtc.client.message.MessageFactory;
import hpbtc.client.message.PIDMessage;
import hpbtc.client.message.ProtocolMessage;
import hpbtc.util.IOUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class PeerConnection {
    
    public static final int AVERAGE_PERIOD = 20000;
    public static final int IDLE_DELAY = 120000;
    public static final int CON_TIMEOUT = 30000;
    public static final int MAX_UPLOAD = 10;

    private static Logger logger = Logger.getLogger(PeerConnection.class.getName());

    private Peer peer = null;
    private SocketChannel ch;
    private AtomicInteger uploaded;
    private AtomicInteger downloaded;
    private int lastUploaded;
    private int lastDownloaded;
    private AtomicInteger uploadRate;
    private AtomicInteger downloadRate;
    private TimerTask contimer;
    private int length;
    private int read;
    private ByteBuffer head;
    private ByteBuffer current;
    private boolean handshakeSent;
    private boolean handshakeRead;
    private boolean pidRead;
    private BlockingQueue<ProtocolMessage> messagesUpload;
    private ByteBuffer cUpload;
    
    /**
     * @param here
     * @param p
     */
    public PeerConnection(Peer p) {
        cUpload = null;
        messagesUpload = new LinkedBlockingQueue<ProtocolMessage>(MAX_UPLOAD);
        handshakeSent = false;
        handshakeRead = false;
        pidRead = false;
        head = ByteBuffer.allocate(4);
        length = 0;
        read = 0;
        uploaded = new AtomicInteger(0);
        downloaded = new AtomicInteger(0);
        uploadRate = new AtomicInteger(0);
        downloadRate = new AtomicInteger(0);
        if (p != null) {
            peer = p;
            p.setConnection(this);
        }
    }
    
    public void writeNext() {
        try {
            if (cUpload != null && cUpload.remaining() > 0) {
                upload(cUpload);
            } else {
                ProtocolMessage pm = messagesUpload.poll();
                if (pm != null) {
                    cUpload = pm.send();
                    upload(cUpload);
                }
            }
        } catch (IOException e) {
            logger.warning("Error while sending message to peer " + peer.getIp() + " " + e.getMessage());
            close();
        }
    }
    
    public void cancelRequestReceived(CancelMessage rm) {
        messagesUpload.remove(rm);
    }
    
    public boolean addUploadMessage(ProtocolMessage rm) {
        boolean r;
        if (messagesUpload.isEmpty()) {
            r = messagesUpload.offer(rm);
            if (r) {
                Client.getInstance().registerNow(this, SelectionKey.OP_WRITE);
            }
        } else {
            r = messagesUpload.offer(rm);
        }
        return r;
    }
    
    public boolean isUploadEmpty() {
        return messagesUpload.isEmpty();
    }

    /**
     * @return
     */
    public int getUploadRate() {
        return uploadRate.get();
    }
    
    /**
     * @return
     */
    public int getDownloadRate() {
        return downloadRate.get();
    }
    
    /**
     * @return
     */
    public int getUploaded() {
        return uploaded.get();
    }
    
    /**
     * @return
     */
    public int getDownloaded() {
        return downloaded.get();
    }
    
    /**
     * @param s
     */
    public void setChannel(SocketChannel s) {
        ch = s;
    }
    
    /**
     * @return
     */
    public SocketChannel getChannel() {
        return ch;
    }
    
    /**
     * @throws IOException
     */
    public void connect() throws IOException {
        ch = SocketChannel.open();
        ch.configureBlocking(false);
        InetSocketAddress addr = new InetSocketAddress(peer.getIp(), peer.getPort());
        final Client cl = Client.getInstance();
        cl.getObserver().fireConnectEvent(peer);
        final DownloadItem item = cl.getDownloadItem();
        try {
            if (ch.connect(addr)) {
                cl.registerNow(this, SelectionKey.OP_READ);
                finishConnect();
                item.initiateConnections(1);
            } else {
                cl.registerNow(this, SelectionKey.OP_CONNECT);
                contimer = new TimerTask() {
        
                    /* (non-Javadoc)
                     * @see java.lang.Runnable#run()
                     */
                    public void run() {
                        cl.getObserver().fireConnectFailedEvent(peer);
                        item.removePeer(peer);
                        close();
                        item.initiateConnections(1);
                    }
                };
                if (!ch.isConnected()) {
                    item.getRateTimer().schedule(contimer, CON_TIMEOUT);
                }
            }
        } catch (ClosedChannelException e) {
            cl.getObserver().fireConnectFailedEvent(peer);
            close();
            Client.getInstance().getDownloadItem().initiateConnections(1);
        }
    }
    
    public TimerTask getConTimer() {
        return contimer;
    }
    
    public void finishConnect() {
        ProtocolMessage hm = new HandshakeMessage();
        hm.setPeer(peer);
        peer.getConnection().addUploadMessage(hm);
        hm = new PIDMessage();
        hm.setPeer(peer);
        peer.getConnection().addUploadMessage(hm);
        setHandshakeSent(true);
    }

    /**
     * @return
     */
    public Peer getPeer() {
        return peer;
    }
    
    public int download(ByteBuffer bb) throws IOException {
        int r = IOUtil.readFromSocket(ch, bb);
        downloaded.addAndGet(r);
        return r;
    }

    public void upload(ByteBuffer bb) throws IOException {
        bb.rewind();
        uploaded.addAndGet(IOUtil.writeToSocket(ch, bb));
    }

    public void startAllTimers() {
        startIdleTimer();
        final Client client = Client.getInstance();
        DownloadItem item = client.getDownloadItem();
        item.getRateTimer().schedule(new TimerTask() {
            /* (non-Javadoc)
             * @see java.util.TimerTask#run()
             */
            @Override
            public void run() {
                int up = uploaded.get();
                int down = downloaded.get();
                int upl = up - lastUploaded;
                int downl = down - lastDownloaded;
                uploadRate.set(upl);
                downloadRate.set(downl);
                lastUploaded = up;
                lastDownloaded = down;
                client.getObserver().fireRateChangeEvent(peer, upl, downl);
            }
        }, 0, AVERAGE_PERIOD);
    }
    
    private void startIdleTimer() {
        DownloadItem item = Client.getInstance().getDownloadItem();
        item.getRateTimer().schedule(new TimerTask() {
            /* (non-Javadoc)
             * @see java.util.TimerTask#run()
             */
            @Override
            public void run() {
                if (isUploadEmpty()) {
                    ProtocolMessage pm = new IdleMessage();
                    pm.setPeer(peer);
                    addUploadMessage(pm);
                }
            }
        }, IDLE_DELAY, IDLE_DELAY);
    }
    
    /**
     * 
     */
    public synchronized void close() {
        if (!ch.isOpen()) {
            return;
        }
        if (contimer != null) {
            contimer.cancel();
        }
        try {
            ch.socket().close();
        } catch (IOException e) {
            logger.warning("Error while closing peer socket");
        }
        if (peer != null) {
            peer.clearConnection(this);
        }
    }
    
    public boolean isHandshakeSent() {
        return handshakeSent;
    }
    
    public void setHandshakeSent(boolean h) {
        handshakeSent = h;
    }
    
    private void startReading() throws IOException {
        read += download(current);
        if (read == length) {
            current.rewind();
            final ProtocolMessage pm;
            if (!handshakeRead) {
                pm = new HandshakeMessage();
                handshakeRead = true;
            } else if (!pidRead) {
                pm = new PIDMessage();
                pidRead = true;
            } else {
                pm = MessageFactory.createMessage(current.get());
            }
            Client.getInstance().getDownloadItem().getRateTimer().schedule(
                new TimerTask() {
                    public void run() {
                        pm.process(current);
                    }
                }, 0);
            read = 0;
            length = 0;
        }
    }
    
    /**
     * 
     */
    public synchronized void readMessage() {
        try {
            if (length == 0) {
                if (!handshakeRead) {
                    length = 48;
                } else if (!pidRead) {
                    length = 20;
                } else {
                    read += download(head);
                    if (read == 4) {
                        length = head.getInt(0);
                        head.rewind();
                        read = 0;
                        if (length == 0) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                current = ByteBuffer.allocate(length);
            }
            startReading();
        } catch (IOException e) {
            InetAddress s = ch.socket().getInetAddress();
            String addr = s == null ? "unknown" : s.getHostAddress();
            logger.warning("Error while reading message from peer " + addr + " " + e.getMessage());
            close();
        }
    }
}
