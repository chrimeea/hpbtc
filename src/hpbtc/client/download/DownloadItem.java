/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.client.download;

import hpbtc.client.Client;
import hpbtc.bencoding.BencodingParser;
import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
    
    public static final String DOWNLOAD_STARTED = "started";
    public static final String DOWNLOAD_COMPLETED = "completed";
    public static final String DOWNLOAD_STOPPED = "stopped";
    public static final int PEER_MAX_REQUESTS = 2;
    public static final int PIPELINE_SIZE = 10;
    public static final int RECALCULATION_DELAY = 10000;
    public static final int OPTIMISTIC_RATE = 3;
    public static final int OPTIMISTIC = 1;
    public static final int END_THRESHOLD = 2;
    public static final int DEFAULT_INTERVAL = 15;
    public static final int TOTAL_PEERS = 50;
    public static final int REQUEST_TIMEOUT = 60000;
    public static final int MAX_CONNECTIONS = 2;

    private static Logger logger = Logger.getLogger(DownloadItem.class.getName());

    private List<LinkedList<String>> trackerURL;
    private boolean multiple;
    private String fileName;
    private Integer fileLength;
    private Integer pieceLength;
    private List<BTFile> files;
    private byte[] infoHash;
    private int interval;
    private int minInterval;
    private int complete;
    private int incomplete;
    private Set<Peer> peers;
    private Queue<Peer> connectionOrder;
    private List<Piece> pieces;
    private int nrPieces;
    private PieceSelectionStrategy pieceStrat;
    private PeerSelectionStrategy peerStrat;
    private ChokingStrategy chStrat;
    private ChokingStrategy optStrat;
    private String trackerId;
    private boolean saved;
    private long lastCheck;
    private int pending;
    private int initiated;
    private Timer rate;
    private AtomicInteger piecesLeft;
    
    /**
     * @param ft
     */
    public DownloadItem(String ft) {
        initiated = 0;
        connectionOrder = new ConcurrentLinkedQueue<Peer>();
        pending = 0;
        lastCheck = 0;
        trackerId = null;
        interval = DEFAULT_INTERVAL;
        minInterval = 0;
        pieceStrat = new RandomStrategy();
        peerStrat = new DistributedStrategy();
        chStrat = new DownloadStrategy();
        optStrat = new OptimisticStrategy();
        peers = new HashSet<Peer>();
        saved = false;
        readTorrent(ft);
    }
    
    private void readTorrent(String ft) {
        try {
            FileInputStream fis = new FileInputStream(ft);
            BencodingParser parser = new BencodingParser(fis);
            BencodedDictionary meta = parser.readNextDictionary();
            fis.close();
            BencodedDictionary info = (BencodedDictionary) meta.get("info");
            infoHash = info.getDigest();
            if (meta.containsKey("announce-list")) {
                BencodedList bl = (BencodedList) meta.get("announce-list");
                trackerURL = new ArrayList<LinkedList<String>>(bl.getSize());
                for (BencodedElement ul : bl) {
                    BencodedList x = (BencodedList) ul;
                    LinkedList<String> z = new LinkedList<String>();
                    for (BencodedElement y : x) {
                        String u = ((BencodedString) y).getValue();
                        z.add(u);
                    }
                    Collections.shuffle(z);
                    trackerURL.add(z);
                }
            } else {
                trackerURL = new ArrayList<LinkedList<String>>(1);
                LinkedList<String> ul = new LinkedList<String>();
                String u = ((BencodedString) meta.get("announce")).getValue();
                ul.add(u);
                trackerURL.add(ul);
            }
            TorrentObserver to = Client.getInstance().getObserver();
            to.fireSetTrackerURLEvent(trackerURL);
            multiple = info.containsKey("files");
            fileName = ((BencodedString) info.get("name")).getValue();
            pieceLength = ((BencodedInteger) info.get("piece length")).getValue();
            to.fireSetPieceLengthEvent(pieceLength);
            if (multiple) {
                BencodedList fls = (BencodedList) info.get("files");
                files = new ArrayList<BTFile>(fls.getSize());
                int index = 0;
                int i = 0;
                int off = 0;
                fileLength = 0;
                for (BencodedElement d : fls) {
                    BencodedDictionary fd = (BencodedDictionary) d;
                    BencodedList dirs = (BencodedList) fd.get("path");
                    StringBuilder sb = new StringBuilder(fileName);
                    sb.append(File.separator);
                    for (BencodedElement dir : dirs) {
                        sb.append(dir);
                        sb.append(File.separator);
                    }
                    Integer fl = ((BencodedInteger) fd.get("length")).getValue();
                    fileLength += fl;
                    BTFile f = new BTFile();
                    f.setPath(sb.substring(0, sb.length() - 1).toString());
                    f.setLength(fl);
                    f.setPieceIndex(index);
                    f.setIndex(i++);
                    f.setOffset(off);
                    if (!f.create()) {
                        saved = true;
                    }
                    files.add(f);
                    index = fileLength / pieceLength;
                    off = fileLength - index * pieceLength;
                }
            } else {
                files = new ArrayList<BTFile>(1);
                fileLength = ((BencodedInteger) info.get("length")).getValue();
                BTFile f = new BTFile();
                f.setPath(fileName);
                f.setLength(fileLength);
                f.setPieceIndex(0);
                f.setIndex(0);
                f.setOffset(0);
                if (!f.create()) {
                    saved = true;
                }
                files.add(f);
            }
            to.fireSetFilesEvent(files);
            nrPieces = fileLength / pieceLength;
            if (fileLength % pieceLength > 0) {
                nrPieces++;
            }
            to.fireSetTotalPiecesEvent(nrPieces);
            piecesLeft = new AtomicInteger(nrPieces);
            createPieces(((BencodedString) info.get("pieces")).getBytes());
        } catch (IOException e) {
            logger.severe("Error while reading from torrent file");
        }
    }
    
    public void removePeer(Peer p) {
        synchronized(peers) {
            peers.remove(p);
        }
    }
    
    private void recoverPieces() {
        TorrentObserver to = Client.getInstance().getObserver();
        to.fireStartCheckSavedEvent();
        int n = nrPieces;
        for (Piece p : pieces) {
            if (p.checkSaved()) {
                n--;
            }
        }
        if (n == 0) {
            chStrat = new UploadStrategy();
            to.fireStartSeedingEvent();
        } else if (n == END_THRESHOLD) {
            peerStrat = new EndGameStrategy();
            logger.fine("Switching to EndGameStrategy for peer selection");
        }
        piecesLeft.set(n);
    }
    
    public Peer findPeer(Peer p) {
        synchronized(peers) {
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
        synchronized(peers) {
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
        return pieceLength;
    }
    
    private void createPieces(byte[] hash) {
        ByteBuffer bb = ByteBuffer.wrap(hash);
        pieces = new ArrayList<Piece>(nrPieces);
        Piece pc;
        for (int i = 0; i < nrPieces - 1; i++) {
            byte[] x = new byte[20];
            bb.get(x);
            pc = new Piece(i, pieceLength, x);
            pieces.add(pc);
        }
        byte[] x = new byte[20];
        bb.get(x);
        pc = new Piece(nrPieces - 1, fileLength - (nrPieces - 1) * pieceLength, x);
        pieces.add(pc);
        addFilesToPieces();
    }
    
    private void addFilesToPieces() {
        for (BTFile f : files) {
            Piece pc = pieces.get(f.getPieceIndex());
            pc.addFile(f);
            for (int i = pc.getIndex() + 1; i <= f.getLastPieceIndex(pieceLength); i++) {
                pieces.get(i).addFile(f);
            }
        }
    }
    
    /**
     * 
     */
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
        for (LinkedList<String> ul : trackerURL) {
            Iterator<String> i = ul.iterator();
            while (i.hasNext()) {
                String tracker = i.next();
                try {
                    getTrackerPeers(event, tracker);
                    i.remove();
                    ul.addFirst(tracker);
                    return;
                } catch (IOException e) {
                    Client.getInstance().getObserver().fireTrackerNotAvailableEvent(tracker);
                }
            }
        }
        logger.warning("All trackers unnavailable");
    }

    private void getTrackerPeers(String event, String tracker) throws IOException {
        Client client = Client.getInstance();
        TorrentObserver to = client.getObserver();
        long l = System.currentTimeMillis();
        long h = l - lastCheck;
        long w = minInterval * 1000;
        while (h < w) {
            to.fireWaitTrackerEvent(w - h);
            try {
                wait(w - h);
            } catch (InterruptedException e) {}
            l = System.currentTimeMillis();
            h = l - lastCheck;
        }
        int uploaded = 0;
        int downloaded = 0;
        synchronized(peers) {
            for (Peer p : peers) {
                PeerConnection pc = p.getConnection();
                if (pc != null) {
                    uploaded += pc.getUploaded();
                    downloaded += pc.getDownloaded();
                }
            }
        }
        StringBuilder req = new StringBuilder(tracker);
        req.append("?info_hash=");
        req.append(URLEncoder.encode(new String(infoHash, "ISO-8859-1"), "ISO-8859-1"));
        req.append("&peer_id=");
        req.append(URLEncoder.encode(client.getPID(), "ISO-8859-1"));
        req.append("&port=");
        req.append(client.getPort());
        req.append("&uploaded=");
        req.append(uploaded);
        req.append("&downloaded=");
        req.append(downloaded);
        req.append("&left=");
        req.append(getBytesLeft());
        req.append("&numwant=");
        req.append(TOTAL_PEERS);
        if (event != null) {
            req.append("&event=");
            req.append(event);
        }
        if (trackerId != null) {
            req.append("trackerid");
            req.append(URLEncoder.encode(trackerId, "ISO-8859-1"));
        }
        logger.info("Connecting to tracker uploaded " + uploaded + " downloaded " + downloaded);
        URL track = new URL(req.toString());
        HttpURLConnection con = (HttpURLConnection) track.openConnection();
        con.setInstanceFollowRedirects(true);
        con.setDoInput(true);
        con.setDoOutput(false);
        con.connect();
        BencodingParser parser = new BencodingParser(con.getInputStream());
        BencodedDictionary response = parser.readNextDictionary();
        con.disconnect();
        if (response.containsKey("failure reason")) {
            to.fireTrackerFailureEvent(((BencodedString) response.get("failure reason")).getValue());
        } else {
            if (response.containsKey("warning message")) {
                to.fireTrackerWarningEvent(((BencodedString) response.get("warning message")).getValue());
            }
            interval = ((BencodedInteger) response.get("interval")).getValue();
            to.fireSetTrackerIntervalEvent(interval);
            if (response.containsKey("min interval")) {
                minInterval = ((BencodedInteger) response.get("min interval")).getValue();
                to.fireSetTrackerMinIntervalEvent(minInterval);
            }
            if (response.containsKey("complete")) {
                complete = ((BencodedInteger) response.get("complete")).getValue();
                to.fireSetSeedersEvent(complete);
            }
            if (response.containsKey("incomplete")) {
                incomplete = ((BencodedInteger) response.get("incomplete")).getValue();
                to.fireSetLeechersEvent(incomplete);
            }
            if (response.containsKey("tracker id")) {
                trackerId =  ((BencodedString) response.get("tracker id")).getValue();
            }
            BencodedList prs = (BencodedList) response.get("peers");
            if (connectionOrder.isEmpty()) {
                synchronized(peers) {
                    connectionOrder.addAll(peers);
                }
            }
            for (BencodedElement e : prs) {
               BencodedDictionary d = (BencodedDictionary) e;
               BencodedString beid = (BencodedString) d.get("peer id");
               String id = beid.getValue();
               if (!Arrays.equals(client.getPIDBytes(), beid.getBytes())) {
                    Peer p = new Peer(((BencodedString) d.get("ip")).getValue(),
                           ((BencodedInteger) d.get("port")).getValue(), id);
                    if (addPeer(p)) {
                        connectionOrder.add(p);
                   }
               }
            }
            to.fireSetTotalPeersEvent(getTotalPeers());
        }
        lastCheck = l;
    }
    
    private int getTotalPeers() {
        synchronized(peers) {
            return peers.size();
        }
    }

    public boolean addPeer(Peer p) {
        synchronized(peers) {
            return peers.add(p);
        }
    }

    public void startDownload() {
        if (trackerURL == null) {
            return;
        }
        if (saved) {
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
        } catch (IllegalStateException e) {}
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
            Client.getInstance().getObserver().fireStartSeedingEvent();
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
            }, interval * 1000);
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
        synchronized(peers) {
            for (Peer p : peers) {
                if (p.isConnected() && !p.hasPiece(i)) {
                    HaveMessage hm = new HaveMessage();
                    hm.setPeer(p);
                    hm.setIndex(i);
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
            s += n * pieceLength;
        }
        return s;
    }

    /**
     * @return
     */
    public byte[] getInfoHash() {
        return infoHash;
    }
    
    /**
     * @return
     */
    public int getTotalPieces() {
        return nrPieces;
    }
    
    private BitSet getBitSet() {
        BitSet bs = new BitSet(nrPieces);
        for (int i = 0; i < nrPieces; i++) {
            if (pieces.get(i).isComplete()) {
                bs.set(i);
            }
        }
        return bs;
    }
}
