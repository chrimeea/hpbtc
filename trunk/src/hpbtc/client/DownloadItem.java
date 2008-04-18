/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.client;

import hpbtc.client.torrent.BTFile;
import hpbtc.client.Client;
import hpbtc.client.message.HaveMessage;
import hpbtc.client.message.RequestMessage;
import hpbtc.client.observer.TorrentObserver;
import hpbtc.client.peer.LightPeer;
import hpbtc.client.peer.Peer;
import hpbtc.client.peer.PeerConnection;
import hpbtc.client.piece.LightPiece;
import hpbtc.client.piece.Piece;
import hpbtc.client.selection.choking.ChokingStrategy;
import hpbtc.client.selection.choking.DownloadStrategy;
import hpbtc.client.selection.choking.OptimisticStrategy;
import hpbtc.client.selection.choking.UploadStrategy;
import hpbtc.client.selection.peer.DistributedStrategy;
import hpbtc.client.selection.peer.EndGameStrategy;
import hpbtc.client.selection.peer.PeerSelectionStrategy;
import hpbtc.client.selection.piece.PieceSelectionStrategy;
import hpbtc.client.selection.piece.RandomStrategy;
import hpbtc.client.selection.piece.RarestFirstStrategy;

import hpbtc.client.torrent.TorrentInfo;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class DownloadItem {
    
    private static Logger logger = Logger.getLogger(DownloadItem.class.getName());

    public static final String DOWNLOAD_STARTED = "started";
    public static final String DOWNLOAD_COMPLETED = "completed";
    public static final String DOWNLOAD_STOPPED = "stopped";
    public static final int PEER_MAX_REQUESTS = 2;
    public static final int PIPELINE_SIZE = 10;
    public static final int RECALCULATION_DELAY = 10000;
    public static final int OPTIMISTIC_RATE = 3;
    public static final int OPTIMISTIC = 1;
    public static final int END_THRESHOLD = 2;
    public static final int REQUEST_TIMEOUT = 60000;
    public static final int MAX_CONNECTIONS = 2;
    
    private Set<Peer> peers = new HashSet<Peer>();
    private Queue<Peer> connectionOrder = new ConcurrentLinkedQueue<Peer>();
    private List<Piece> pieces;
    private PieceSelectionStrategy pieceStrat = new RandomStrategy();
    private PeerSelectionStrategy peerStrat = new DistributedStrategy();
    private ChokingStrategy chStrat = new DownloadStrategy();
    private ChokingStrategy optStrat = new OptimisticStrategy();
    private int pending;
    private int initiated;
    private Timer rate;
    private AtomicInteger piecesLeft;
    private TorrentInfo torrent;

    /**
     * @param ft
     */
    public DownloadItem(String ft) throws IOException {
        torrent = new TorrentInfo(ft);
        piecesLeft = new AtomicInteger(torrent.getNrPieces());
        createPieces(torrent.getPieceHash());
    }

    public void removePeer(Peer p) {
        synchronized (peers) {
            peers.remove(p);
        }
    }

    private void recoverPieces() {
        logger.info("start check saved");
        int n = torrent.getNrPieces();
        for (Piece p : pieces) {
            if (p.checkSaved()) {
                n--;
            }
        }
        if (n == 0) {
            chStrat = new UploadStrategy();
            logger.info("start seeding");
        } else if (n == END_THRESHOLD) {
            peerStrat = new EndGameStrategy();
            logger.fine("Switching to EndGameStrategy for peer selection");
        }
        piecesLeft.set(n);
    }

    public Peer findPeer(Peer p) {
        synchronized (peers) {
            for (Peer pp : peers) {
                if (pp.equals(p)) {
                    return pp;
                }
            }
        }
        return null;
    }

    /**
     * @return
     */
    public List<LightPeer> getPeers() {
        synchronized (peers) {
            List<LightPeer> lp = new LinkedList<LightPeer>();
            for (Peer p : peers) {
                PeerConnection pc = p.getConnection();
                if (p.isConnected() && p.isInterestedThere()) {
                    LightPeer l = new LightPeer(p);
                    l.setUploadRate(pc.getUploadRate());
                    l.setDownloadRate(pc.getDownloadRate());
                    l.setChoked(true);
                    lp.add(l);
                }
            }
            return lp;
        }
    }

    public int getPieceLength() {
        return torrent.getPieceLength();
    }

    private void createPieces(byte[] hash) {
        ByteBuffer bb = ByteBuffer.wrap(hash);
        pieces = new ArrayList<Piece>(torrent.getNrPieces());
        Piece pc;
        for (int i = 0; i < torrent.getNrPieces() - 1; i++) {
            byte[] x = new byte[20];
            bb.get(x);
            pc = new Piece(i, torrent.getPieceLength(), x);
            pieces.add(pc);
        }
        byte[] x = new byte[20];
        bb.get(x);
        pc = new Piece(torrent.getNrPieces() - 1, torrent.getFileLength() -
                (torrent.getNrPieces() - 1) * torrent.getPieceLength(), x);
        pieces.add(pc);
        addFilesToPieces();
    }

    private void addFilesToPieces() {
        for (BTFile f : torrent.getFiles()) {
            Piece pc = pieces.get(f.getPieceIndex());
            pc.addFile(f);
            for (int i = pc.getIndex() + 1; i <= f.getLastPieceIndex(torrent.getPieceLength()); i++) {
                pieces.get(i).addFile(f);
            }
        }
    }

    public void stopDownload() {
        tryGetTrackerPeers(DOWNLOAD_STOPPED);
    }

    /**
     * @param index
     * @return
     */
    public Piece getPiece(int index) {
        return pieces.get(index);
    }

    private void tryGetTrackerPeers(String event) {
        int uploaded = 0;
        int downloaded = 0;
        synchronized (peers) {
            for (Peer p : peers) {
                PeerConnection pc = p.getConnection();
                if (pc != null) {
                    uploaded += pc.getUploaded();
                    downloaded += pc.getDownloaded();
                }
            }
        }
        Set<Peer> lastPeers = torrent.tryGetTrackerPeers(event, uploaded, downloaded, getBytesLeft());
        if (connectionOrder.isEmpty()) {
            synchronized (peers) {
                connectionOrder.addAll(peers);
            }
        }
        for (Peer p : lastPeers) {
            if (addPeer(p)) {
                connectionOrder.add(p);
            }
        }
    }

    public boolean addPeer(Peer p) {
        synchronized (peers) {
            return peers.add(p);
        }
    }

    public void startDownload() {
        if (torrent.getTrackerURL() == null) {
            return;
        }
        if (torrent.isSaved()) {
            recoverPieces();
        }
        rate = new Timer("Worker", true);
        getNewPeers(DOWNLOAD_STARTED);
        startChokingTimer();
        findAllPieces();
    }

    public Timer getRateTimer() {
        return rate;
    }

    public synchronized void findAllPieces() {
        for (int i = pending; i < PIPELINE_SIZE; i++) {
            if (!findPiece()) {
                break;
            }
        }
    }

    private void startChokingTimer() {
        try {
            rate.schedule(new TimerTask() {

                private int rep = 0;

                /* (non-Javadoc)
                 * @see java.util.TimerTask#run()
                 */
                @Override
                public void run() {
                    List<LightPeer> lp = chStrat.select(Client.getInstance().getDownloadItem());
                    List<LightPeer> alt = new LinkedList<LightPeer>();
                    for (LightPeer l : lp) {
                        boolean i = l.isChoked();
                        l.getPeer().setChokedHere(i);
                        if (i) {
                            alt.add(l);
                        }
                    }
                    if ((++rep) % OPTIMISTIC_RATE == 0) {
                        rep = 0;
                        for (int j = 0; j < OPTIMISTIC; j++) {
                            LightPeer x = optStrat.select(alt);
                            if (x != null) {
                                x.getPeer().setChokedHere(x.isChoked());
                                alt.remove(x);
                            } else {
                                break;
                            }
                        }
                    }
                }
            }, RECALCULATION_DELAY, RECALCULATION_DELAY);
        } catch (IllegalStateException e) {
        }
    }

    /**
     * @return
     */
    private boolean findPiece() {
        int n = piecesLeft.get();
        if (n == END_THRESHOLD && !(peerStrat instanceof EndGameStrategy)) {
            peerStrat = new EndGameStrategy();
            logger.fine("Switching to EndGameStrategy for peer selection");
        } else if (n == 0 && !(chStrat instanceof UploadStrategy)) {
            chStrat = new UploadStrategy();
            logger.info("start seeding");
            tryGetTrackerPeers(DOWNLOAD_COMPLETED);
            return false;
        }
        LightPiece pieceSel = pieceStrat.select(this);
        if (pieceSel != null) {
            return processNext(pieceSel.getPiece());
        }
        return false;
    }

    public synchronized void findNextPiece() {
        pending--;
        findPiece();
    }

    /**
     * @param pi
     */
    private boolean processNext(Piece pi) {
        LightPiece pieceSel = new LightPiece(pi);
        pieceSel.setPeers(pi.getAvailablePeers());
        List<LightPeer> peerSel = peerStrat.select(pieceSel);
        RequestMessage rm = pieceSel.getPiece().getNextChunk();
        if (rm != null && pieceSel.getPiece().sendRequest(rm, peerSel)) {
            pending++;
            return true;
        }
        return false;
    }

    private void getNewPeers(String ev) {
        tryGetTrackerPeers(ev);
        if (piecesLeft.get() > 0) {
            initiateConnections(0);
        }
        startTrackerTimer();
    }

    private void startTrackerTimer() {
        try {
            rate.schedule(new TimerTask() {
                /* (non-Javadoc)
                 * @see java.util.TimerTask#run()
                 */

                @Override
                public void run() {
                    getNewPeers(null);
                }
            }, torrent.getInterval() * 1000);
        } catch (IllegalStateException e) {
            logger.severe(e.getMessage());
        }
    }

    /**
     * @param i
     */
    public void broadcastHave(int i) {
        piecesLeft.decrementAndGet();
        if (!(pieceStrat instanceof RarestFirstStrategy)) {
            pieceStrat = new RarestFirstStrategy();
        }
        BitSet a = getBitSet();
        synchronized (peers) {
            for (Peer p : peers) {
                if (p.isConnected() && !p.hasPiece(i)) {
                    HaveMessage hm = new HaveMessage(i);
                    p.getConnection().addUploadMessage(hm);
                }
                BitSet b = p.getAllPieces();
                b.andNot(a);
                if (p.isConnected() && b.isEmpty()) {
                    p.setInterestedHere(false);
                }
            }
        }
    }

    public boolean removePeerOrder(Peer p) {
        return connectionOrder.remove(p);
    }

    public synchronized void initiateConnections(int finished) {
        initiated -= finished;
        while (initiated < MAX_CONNECTIONS) {
            Peer p;
            do {
                p = connectionOrder.poll();
            } while (p != null && p.isConnecting());
            if (p != null) {
                initiated++;
                final Peer x = p;
                rate.schedule(new TimerTask() {

                    public void run() {
                        PeerConnection con = new PeerConnection(x);
                        try {
                            con.connect();
                        } catch (IOException e) {
                            logger.warning("Unable to connect to peer " + x.getIp() + " " + e.getMessage());
                            con.close();
                            removePeer(x);
                            initiateConnections(1);
                        }
                    }
                }, 0);
            } else {
                break;
            }
        }
    }

    /**
     * @return
     */
    public int getBytesLeft() {
        int n = piecesLeft.get();
        int s = 0;
        if (n > 0) {
            Piece p = pieces.get(pieces.size() - 1);
            if (p.isComplete()) {
                s += p.getSize();
                n--;
            }
            s += n * torrent.getPieceLength();
        }
        return s;
    }

    /**
     * @return
     */
    public byte[] getInfoHash() {
        return torrent.getInfoHash();
    }

    /**
     * @return
     */
    public int getTotalPieces() {
        return torrent.getNrPieces();
    }

    private BitSet getBitSet() {
        BitSet bs = new BitSet(torrent.getNrPieces());
        for (int i = 0; i < torrent.getNrPieces(); i++) {
            if (pieces.get(i).isComplete()) {
                bs.set(i);
            }
        }
        return bs;
    }
}
