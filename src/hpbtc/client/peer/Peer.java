/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.client.peer;

import hpbtc.client.Client;
import hpbtc.client.DownloadItem;
import hpbtc.client.message.CancelMessage;
import hpbtc.client.message.ChokeMessage;
import hpbtc.client.message.InterestedMessage;
import hpbtc.client.message.NotInterestedMessage;
import hpbtc.client.message.ProtocolMessage;
import hpbtc.client.message.RequestMessage;
import hpbtc.client.message.UnchokeMessage;

import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class Peer {

    private static Logger logger = Logger.getLogger(Peer.class.getName());

    private String peerId;
    private String ip;
    private int port;
    private AtomicReference<PeerConnection> con;
    private AtomicBoolean chokedHere;
    private AtomicBoolean interestedHere;
    private AtomicBoolean chokedThere;
    private AtomicBoolean interestedThere;
    private BitSet pieces;
    private AtomicInteger requestsSent;
    private AtomicBoolean snub;
    
    /**
     * @param i
     * @param p
     * @param id
     */
    public Peer(String i, int p, String id) {
        snub = new AtomicBoolean(false);
        con = new AtomicReference<PeerConnection>(null);
        pieces = new BitSet();
        peerId = id;
        ip = i;
        port = p;
        chokedHere = new AtomicBoolean(true);
        interestedHere = new AtomicBoolean(false);
        chokedThere = new AtomicBoolean(true);
        interestedThere = new AtomicBoolean(false);
        requestsSent = new AtomicInteger(0);
    }
    
    public synchronized void removePieces() {
        pieces.clear();
    }

    public void requestDone() {
        logger.fine("Peer " + ip + " has " + requestsSent.decrementAndGet() + " requests");
        setSnubbed(false);
    }

    /**
     * @return
     */
    public synchronized int getTotalPieces() {
        return pieces.cardinality();
    }
    
    /**
     * @return
     */
    public synchronized BitSet getAllPieces() {
        return (BitSet) pieces.clone();
    }
    
    /**
     * @param index
     */
    public synchronized void addPiece(int index) {
        pieces.set(index);
        if (isConnected() && !interestedHere.get() &&
            !Client.getInstance().getDownloadItem().getPiece(index).isComplete()) {
                setInterestedHere(true);
        }
    }
    
    /**
     * @param index
     * @return
     */
    public synchronized boolean hasPiece(int index) {
        return pieces.get(index);
    }
    
    /**
     * @return
     */
    public boolean isFree() {
        return requestsSent.get() == 0;
    }
    
    /**
     * @return
     */
    public int totalRequestsSent() {
        return requestsSent.get();
    }
    
    public boolean sendRequest(final RequestMessage rm) {
        if (isConnected() && !chokedThere.get()) {
            PeerConnection pc = con.get();
            pc.addUploadMessage(rm);
            logger.fine("Peer " + ip + " has " + requestsSent.incrementAndGet() + " requests");
            return true;
        }
        return false;
    }

    public void setSnubbed(boolean s) {
        boolean old = snub.getAndSet(s);
        if (old != s) {
            logger.info("snub " + this);
        }
    }

    /**
     * @param rm
     */
    public void cancelRequestSent(RequestMessage rm) {
        PeerConnection pc = con.get();
        if (pc != null) {
            CancelMessage cm = new CancelMessage(rm.getBegin(), rm.getIndex(), rm.getLength());
            pc.addUploadMessage(cm);
            logger.fine("Peer " + ip + " has " + requestsSent.decrementAndGet() + " requests");
        } else {
            logger.fine("Peer " + ip + " has lost connection, requests " + requestsSent.decrementAndGet());
        }
    }
    
    /**
     * @param c
     */
    public void setChokedThere(boolean c) {
        boolean d = chokedThere.getAndSet(c);
        if (!d && c) {
            cancelAll();
        }
    }
    
    private synchronized void cancelAll() {
        DownloadItem item = Client.getInstance().getDownloadItem();
        for (int i = pieces.nextSetBit(0); i >= 0; i = pieces.nextSetBit(i + 1)) {
            item.getPiece(i).cancelAll(this);
        }
        requestsSent.getAndSet(0);
    }
    
    /**
     * @return
     */
    public boolean isInterestedHere() {
        return interestedHere.get();
    }
    
    /**
     * @param i
     */
    public void setInterestedThere(boolean i) {
        interestedThere.set(i);
    }

    /**
     * @param c
     */
    public void setChokedHere(boolean c) {
        if (chokedHere.getAndSet(c) != c) {
            PeerConnection pc = con.get();
            if (pc != null) {
                ProtocolMessage cm = c ? new ChokeMessage() : new UnchokeMessage();
                pc.addUploadMessage(cm);
            }
        }
    }
    
    /**
     * @param i
     */
    public void setInterestedHere(boolean i) {
        if (interestedHere.getAndSet(i) != i) {
            PeerConnection pc = con.get();
            if (pc != null) {
                ProtocolMessage pm = i ? new InterestedMessage() : new NotInterestedMessage();
                con.get().addUploadMessage(pm);
            }
        }
    }

    /**
     * @return
     */
    public boolean isChokedHere() {
        return chokedHere.get();
    }
    
    /**
     * @return
     */
    public boolean isChokedThere() {
        return chokedThere.get();
    }

    /**
     * @return
     */
    public boolean isInterestedThere() {
        return interestedThere.get();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        Peer p;
        if (o instanceof LightPeer) {
            p = ((LightPeer) o).getPeer();
        } else if (o instanceof Peer) {
            p = (Peer) o;
        } else {
            return false;
        }
        if (ip.equals(p.ip) && port == p.port) {
            return true;
        }
        return false;
    }
    
    /**
     * @return
     */
    public PeerConnection getConnection() {
        return con.get();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return (ip + "#" + port).hashCode();
    }
    
    /**
     * @param p
     */
    public void clearConnection(PeerConnection p) {
        if (con.compareAndSet(p, null)) {
            logger.fine("Reseting connection for peer " + ip);
            setInterestedHere(false);
            setInterestedThere(false);
            setChokedHere(true);
            setChokedThere(true);
        }
    }
    
    /**
     * @param p
     * @return
     */
    public boolean setConnection(PeerConnection p) {
        if (p == null) {
            logger.fine("Connection silently dropped for peer " + ip);
        }
        return (con.compareAndSet(null, p));
    }
    
    /**
     * @return
     */
    public boolean isConnected() {
        PeerConnection x = con.get();
        return (x != null && x.getChannel().isConnected());
    }
    
    public boolean isConnecting() {
        PeerConnection x = con.get();
        if (x == null) {
            return false;
        }
        SocketChannel c = x.getChannel();
        return c.isConnected() || c.isConnectionPending();
    }
    
    public boolean isSnubbed() {
        return snub.get();
    }
    
    /**
     * @return Returns the ip.
     */
    public String getIp() {
        return ip;
    }
    
    public void setId(String id) {
        peerId = id;
    }
    
    /**
     * @return Returns the peerId.
     */
    public String getId() {
        return peerId;
    }
    
    /**
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ip;
    }
}
