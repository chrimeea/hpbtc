package hpbtc.piece;

import hpbtc.Client;
import hpbtc.download.BTFile;
import hpbtc.download.DownloadItem;
import hpbtc.message.CancelMessage;
import hpbtc.message.PieceMessage;
import hpbtc.message.RequestMessage;
import hpbtc.peer.LightPeer;
import hpbtc.peer.Peer;
import hpbtc.util.ComparatorUtil;
import hpbtc.util.IOUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class Piece {

    private static Logger logger = Logger.getLogger(Piece.class.getName());
    
    public static final int DEFAULT_CHUNK = 16384;

    private int chunkSize;
    private int index;
    private Queue<Peer> peers;
    private BitSet chunks;
    private BitSet req;
    private int totalChunks;
    private ByteBuffer bb;
    private AtomicBoolean ok;
    private int size;
    private Set<BTFile> files;
    private byte[] hash;
    private AtomicBoolean started;
    private int lastChunk;
    private ConcurrentMap<RequestMessage, TimerTask> requestsSent;

    /**
     * @param i
     * @param s
     * @param h
     */
    public Piece(int i, int s, byte[] h) {
        chunkSize = s > DEFAULT_CHUNK ? DEFAULT_CHUNK : s;
        hash = h;
        ok = new AtomicBoolean(false);
        size = s;
        index = i;
        int k = s / chunkSize;
        if (s % chunkSize > 0) {
            k++;
        }
        totalChunks = k;
        chunks = new BitSet(k);
        req = new BitSet(k);
        peers = new ConcurrentLinkedQueue<Peer>();
        files = new LinkedHashSet<BTFile>();
        started = new AtomicBoolean(false);
        lastChunk = 0;
        requestsSent = new ConcurrentHashMap<RequestMessage, TimerTask>();
    }

    public boolean sendRequest(final RequestMessage rm, final List<LightPeer> peerSel) {
        boolean sent = false;
        TimerTask tt = new TimerTask() {
            /* (non-Javadoc)
             * @see java.util.TimerTask#run()
             */
            @Override
            public void run() {
                if (!haveAll(rm.getBegin(), rm.getLength())) {
                    logger.info("Did not receive piece " + (index + 1) + " begin " + rm.getBegin() + " length " + rm.getLength());
                    if (cancelRequestSent(rm, peerSel)) {
                        discardMessage(rm);
                    }
                }
            }
        };
        try {
            for (LightPeer p : peerSel) {
                RequestMessage r = (RequestMessage) rm.clone();
                r.setPeer(p.getPeer());
                if (p.getPeer().sendRequest(r)) {
                    sent = true;
                    requestsSent.put(r, tt);
                }
            }
        } catch (CloneNotSupportedException e) {
            logger.severe("The request message can not be copied " + e.getMessage());
        }
        if (sent) {
            setStarted(true);
            DownloadItem item = Client.getInstance().getDownloadItem();
            item.getRateTimer().schedule(tt, DownloadItem.REQUEST_TIMEOUT);
        }
        return sent;
    }
    
    public int cancelAll(Peer p) {
        int i = 0;
        for (RequestMessage rm : requestsSent.keySet()) {
            if (p.equals(rm.getPeer())) {
                logger.info("Request for piece " + (index + 1) + " begin " + rm.getBegin() + " length " + rm.getLength() + " to peer " + p.getIp() + " dropped");
                TimerTask tt = requestsSent.remove(rm);
                if (tt != null) {
                    tt.cancel();
                    discardMessage(rm);
                }
                i++;
            }
        }
        return i;
    }
    
    private void discardMessage(RequestMessage rm) {
        if (cancelAll(rm.getBegin(), rm.getLength())) {
            Client.getInstance().getDownloadItem().findNextPiece();
        }
    }
    
    public boolean cancelRequestSent(RequestMessage rm, List<LightPeer> peerSel) {
        boolean r = false;
        for (LightPeer p : peerSel) {
            Peer pp = p.getPeer();
            rm.setPeer(pp);
            TimerTask tt = requestsSent.remove(rm);
            if (tt != null) {
                CancelMessage cm = new CancelMessage();
                cm.setPeer(pp);
                cm.setIndex(rm.getIndex());
                cm.setBegin(rm.getBegin());
                cm.setLength(rm.getLength());
                pp.getConnection().addUploadMessage(cm);
                r = true;
                pp.setSnubbed(true);
            }
        }
        return r;
    }

    public boolean requestDone(PieceMessage rm) {
        boolean r = false;
        TimerTask tt = requestsSent.remove(rm);
        if (tt != null) {
            tt.cancel();
            r = true;
        }
        for (RequestMessage m : requestsSent.keySet()) {
            if (m.getIndex() == rm.getIndex() &&
                m.getBegin() == rm.getBegin() &&
                m.getLength() == rm.getLength()) {
                tt = requestsSent.remove(m);
                if (tt != null) {
                    tt.cancel();
                }
                m.getPeer().cancelRequestSent(m);
            }
        }
        return r;
    }

    /**
     * @return
     */
    public int getSize() {
        return size;
    }
    
    public boolean hasNextChunk() {
        return nextBit() != -1;
    }
    
    private synchronized int nextBit() {
        if (lastChunk == -1) {
            return -1;
        }        
        int i = req.nextClearBit(lastChunk);
        if (i == -1 || i >= totalChunks) {
            i = -1;
        }
        return i;
    }

    private int[] getAll(int o, int l) {
        int rem = o % chunkSize;
        if (rem != 0) {
            o += chunkSize - o % chunkSize;
        }
        l += o - 1;
        int max = l == size ? totalChunks - 1 : l / chunkSize;
        int y = o / chunkSize;
        int[] x = new int[max - y + 1];
        for (int i = y; i <= max; i++) {
            x[i - y] = i;
        }
        return x;
    }

    public synchronized boolean cancelAll(int o, int l) {
        int[] x = getAll(o, l);
        boolean y = false;
        if (x.length > 0) {
            for (int i : x) {
                if (req.get(i)) {
                    req.clear(i);
                    y = true;
                }
            }
            lastChunk = x[0];
        }
        return y;
    }
    
    /**
     * @return
     */
    public synchronized RequestMessage getNextChunk() {
        lastChunk = nextBit();
        if (lastChunk == -1) {
            return null;
        }
        req.set(lastChunk);
        RequestMessage rm = new RequestMessage();
        rm.setIndex(index);
        rm.setBegin(lastChunk * chunkSize);
        rm.setLength(getChunkSize(lastChunk));
        if (lastChunk == totalChunks - 1) {
            lastChunk = -1;
        } else {
            lastChunk++;
        }
        return rm;
    }
    
    /**
     * @return
     */
    public List<LightPeer> getAvailablePeers() {
        List<LightPeer> prs = new LinkedList<LightPeer>();
        for (Peer p : peers) {
            if (!p.isChokedThere() && p.totalRequestsSent() < DownloadItem.PEER_MAX_REQUESTS) {
                LightPeer lp = new LightPeer(p);
                lp.setTotalPieces(p.getTotalPieces());
                prs.add(lp);
            }
        }
        return prs;
    }

    /**
     * @param p
     */
    public void addPeer(Peer p) {
        peers.add(p);
    }

    private List<BTFile> getFiles(int begin, int length) {
        List<BTFile> fls = new LinkedList<BTFile>();
        for (BTFile f : files) {
            if (f.getPieceIndex() < index &&
                f.getLength() - f.getFileOffset(this.index) > begin) {
                    fls.add(f);
            } else if (f.getPieceIndex() == index &&
                    f.getOffset() < begin + length &&
                    f.getOffset() + f.getLength() > begin) {
                fls.add(f);
            } else if (f.getPieceIndex() == index &&
                f.getOffset() > begin + length) {
                break;
            }
        }
        return fls;
    }
    
    /**
     * @param begin
     * @param length
     * @return
     * @throws IOException
     */
    public ByteBuffer getPiece(int begin, int length) throws IOException {
        List<BTFile> f = getFiles(begin, length);
        ByteBuffer b = null;
        BTFile fl = f.get(0);
        FileChannel ch = new FileInputStream(fl.getPath()).getChannel();
        b = ByteBuffer.allocate(length);
        int o;
        if (f.size() == 1) {
            if (fl.getPieceIndex() < index) {
                o = begin + fl.getFileOffset(index);
            } else {
                o = begin - fl.getOffset();
            }
            ch.position(o);
            IOUtil.readFromFile(ch, b, length);
            ch.close();
        } else {
            if (fl.getPieceIndex() < index) {
                o = begin + fl.getFileOffset(index);
                ch.position(o);
                int max = fl.getLength() - o;
                IOUtil.readFromFile(ch, b, length < max ? length : max);
            } else {
                o =  begin - fl.getOffset();
                ch.position(o);
                IOUtil.readFromFile(ch, b, fl.getLength() - o);
            }
            for (int k = 1; k < f.size() - 1; k++) {
                fl = f.get(k);
                ch = new FileInputStream(fl.getPath()).getChannel();
                IOUtil.readFromFile(ch, b, fl.getLength());
            }
            fl = f.get(f.size() - 1);
            ch = new FileInputStream(fl.getPath()).getChannel();
            IOUtil.readFromFile(ch, b, length - fl.getOffset());
        }
        return b;
    }

    public boolean addFile(BTFile f) {
        return files.add(f);
    }
    
    private int getChunkSize(int c) {
        if (c < totalChunks - 1) {
            return chunkSize;
        }
        return size - c * chunkSize;
    }
    
    private synchronized void freeBuffer() {
        bb = null;
    }
    
    /**
     * @param o
     * @param b
     * @return
     */
    public boolean savePiece(int o, ByteBuffer b) {
        int d = o % chunkSize;
        if (d != 0) {
            d = chunkSize - d;
            o += d;
            b.position(b.position() + d);
        }
        int l = b.remaining() + o - 1;
        int max = l == size ? totalChunks - 1 : l / chunkSize;
        synchronized(this) {
            if (bb == null) {
                return false;
            }
            bb.position(o);
            for (int i = o / chunkSize; i <= max; i++) {
                if (!chunks.get(i)) {
                    int j = getChunkSize(i);
                    b.limit(b.position() + j);
                    bb.put(b);
                    chunks.set(i);
                } else {
                    b.position(b.position() + chunkSize);
                    bb.position(bb.position() + chunkSize);
                }
            }
        }
        if (getCardinality() == totalChunks) {
            Client client = Client.getInstance();
            if (checkHash()) {
                flush();
                ok.set(true);
                freeBuffer();
                for (Peer p : peers) {
                    if (p.isInterestedHere()) {
                        BitSet bs = p.getAllPieces();
                        boolean pass = true;
                        DownloadItem item = client.getDownloadItem();
                        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                            if (!item.getPiece(i).isComplete()) {
                                pass = false;
                                break;
                            }
                        }
                        if (pass) {
                            logger.info("Have all pieces from peer " + p.getIp() + " no longer interested");
                            p.setInterestedHere(false);
                        }
                    }
                }
                client.getObserver().fireFinishedPieceEvent(this);
                return true;
            } else {
                client.getObserver().firePieceDiscardedEvent(this);
                freeBuffer();
                reset();
            }
        }
        return false;
    }

    private synchronized int getCardinality() {
        return chunks.cardinality();
    }
    
    public boolean checkSaved() {
        try {
            bb = getPiece(0, size);
            if (checkHash()) {
                Client.getInstance().getObserver().fireRecoveredPieceEvent(this);
                ok.set(true);
                lastChunk = -1;
                bb = null;
                return true;
            } else {
                bb = null;
            }
        } catch (IOException e) {
            logger.warning("Error while recovering piece " + (index + 1));
        }
        return false;
    }
    
    private boolean checkHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            bb.rewind();
            md.update(bb);
            return Arrays.equals(md.digest(), hash);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("SHA1 is not available");
        }
        return false;
    }
    
    private void flush() {
        Client.getInstance().getObserver().fireFlushPieceEvent(this);
        Iterator<BTFile> it = files.iterator();
        BTFile fl = it.next();
        int k = 0;
        try {
            FileChannel ch = new RandomAccessFile(fl.getPath(), "rw").getChannel();
            if (fl.getPieceIndex() < index) {
                int o = fl.getFileOffset(index);
                int t = fl.getLength() - o;
                bb.rewind();
                ch.position(o);
                if (t >= size) {
                    IOUtil.writeToFile(ch, bb);
                    ch.close();
                    return;
                }
                bb.limit(t);
                IOUtil.writeToFile(ch, bb);
                ch.close();
                k = 1;
            } else {
                it = files.iterator();
            }
            for (; k < files.size() - 1; k++) {
                fl = it.next();
                ch = new RandomAccessFile(fl.getPath(), "rw").getChannel();
                bb.limit(fl.getOffset() + fl.getLength());
                bb.position(fl.getOffset());
                IOUtil.writeToFile(ch, bb);
                ch.close();
            }
            fl = it.next();
            ch = new RandomAccessFile(fl.getPath(), "rw").getChannel();
            bb.clear();
            bb.position(fl.getOffset());
            IOUtil.writeToFile(ch, bb);
            ch.close();
        } catch (IOException e) {
            logger.severe("Error while flushing piece " + (index + 1));
        }
    }
    
    /**
     * @param s
     */
    public void setStarted(boolean s) {
        if (s && !isStarted()) {
            bb = ByteBuffer.allocate(size);
            started.set(s);
        }
    }
    
    /**
     * @return
     */
    public boolean isStarted() {
        return started.get();
    }
    
    /**
     * @param o
     * @param l
     * @return
     */
    public synchronized boolean haveAll(int o, int l) {
        for (int i : getAll(o, l)) {
            if (!chunks.get(i)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isComplete() {
        return ok.get();
    }
    
    /**
     * @return
     */
    public boolean willComplete() {
        return ok.get() || (started.get() && !hasNextChunk());
    }
    
    /**
     * @return
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * 
     */
    public synchronized void reset() {
        chunks.clear();
        lastChunk = 0;
    }
    
    /**
     * @param c
     * @return
     */
    public synchronized boolean haveChunk(int c) {
        return chunks.get(c);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof Piece)) {
            return false;
        }
        Piece p = (Piece) o;
        if (ComparatorUtil.compare(this, p) == 0) {
            return p.getIndex() == index;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
       return new Integer(index).hashCode(); 
    }
}
