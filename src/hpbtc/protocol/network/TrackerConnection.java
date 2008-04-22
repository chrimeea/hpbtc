package hpbtc.protocol.network;

import hpbtc.bencoding.BencodingParser;
import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;
import hpbtc.protocol.network.Peer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerConnection {

    public static final int TOTAL_PEERS = 50;
    public static final int DEFAULT_INTERVAL = 15;
    private static Logger logger = Logger.getLogger(TrackerConnection.class.getName());
    
    private int complete;
    private int interval;
    private int incomplete;
    private String trackerId;
    private int minInterval;
    private long lastCheck;
    private byte[] infoHash;
    private byte[] pid;
    private int port;
    private List<LinkedList<String>> trackers;
    
    public TrackerConnection(byte[] infoHash, byte[] pid, int port, List<LinkedList<String>> trackers) {
        this.infoHash = infoHash;
        this.pid = pid;
        this.port = port;
        this.trackers = trackers;
    }

    public Set<Peer> getTrackerPeers(String event, int uploaded, int downloaded, int bytesLeft) {
        for (LinkedList<String> ul : trackers) {
            Iterator<String> i = ul.iterator();
            while (i.hasNext()) {
                String tracker = i.next();
                try {
                    Set<Peer> lastPeers = getTrackerPeers(event, tracker, uploaded, downloaded, bytesLeft);
                    i.remove();
                    ul.addFirst(tracker);
                    return lastPeers;
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
            }
        }
        return new HashSet<Peer>();
    }

    private Set<Peer> getTrackerPeers(String event, String tracker, int uploaded,
            int downloaded, int bytesLeft) throws IOException {
        long l = System.currentTimeMillis();
        long h = l - lastCheck;
        long w = minInterval * 1000;
        while (h < w) {
            logger.info("Wait tracker " + (w - h));
            try {
                wait(w - h);
            } catch (InterruptedException e) {
            }
            l = System.currentTimeMillis();
            h = l - lastCheck;
        }
        StringBuilder req = new StringBuilder(tracker);
        req.append("?info_hash=");
        req.append(URLEncoder.encode(new String(infoHash, "ISO-8859-1"), "ISO-8859-1"));
        req.append("&peer_id=");
        req.append(URLEncoder.encode(getPID(pid), "ISO-8859-1"));
        req.append("&port=");
        req.append(port);
        req.append("&uploaded=");
        req.append(uploaded);
        req.append("&downloaded=");
        req.append(downloaded);
        req.append("&left=");
        req.append(bytesLeft);
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
        URL track = new URL(req.toString());
        HttpURLConnection con = (HttpURLConnection) track.openConnection();
        con.setInstanceFollowRedirects(true);
        con.setDoInput(true);
        con.setDoOutput(false);
        con.connect();
        BencodingParser parser = new BencodingParser(con.getInputStream());
        BencodedDictionary response = parser.readNextDictionary();
        con.disconnect();
        Set<Peer> peers = new HashSet<Peer>();
        if (response.containsKey("failure reason")) {
            logger.info("tracker failure " + ((BencodedString) response.get("failure reason")).getValue());
        } else {
            if (response.containsKey("warning message")) {
                logger.info("tracker warning " + ((BencodedString) response.get("warning message")).getValue());
            }
            interval = ((BencodedInteger) response.get("interval")).getValue();
            logger.info("tracker interval " + interval);
            if (response.containsKey("min interval")) {
                minInterval = ((BencodedInteger) response.get("min interval")).getValue();
                logger.info("set tracker min interval " + minInterval);
            }
            if (response.containsKey("complete")) {
                complete = ((BencodedInteger) response.get("complete")).getValue();
                logger.info("set seeders " + complete);
            }
            if (response.containsKey("incomplete")) {
                incomplete = ((BencodedInteger) response.get("incomplete")).getValue();
                logger.info("set leechers " + incomplete);
            }
            if (response.containsKey("tracker id")) {
                trackerId = ((BencodedString) response.get("tracker id")).getValue();
            }
            BencodedList prs = (BencodedList) response.get("peers");
            for (BencodedElement e : prs) {
                BencodedDictionary d = (BencodedDictionary) e;
                BencodedString beid = (BencodedString) d.get("peer id");
                String id = beid.getValue();
                if (!Arrays.equals(pid, beid.getBytes())) {
                    Peer p = new Peer(((BencodedString) d.get("ip")).getValue(),
                            ((BencodedInteger) d.get("port")).getValue(), id);
                    peers.add(p);
                }
            }
        }
        lastCheck = l;
        return peers;
    }

    /**
     * @return
     */
    private static String getPID(byte[] pid) {
        String s;
        try {
            s = new String(pid, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            s = null;
            logger.severe("ISO-8859-1 is not available");
        }
        return s;
    }
}
