package hpbtc.protocol.network;

import hpbtc.bencoding.BencodingReader;
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
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerConnection {

    public static final int TOTAL_PEERS = 50;
    private static Logger logger = Logger.getLogger(TrackerConnection.class.getName());
    
    private long complete;
    private long interval;
    private long incomplete;
    private String trackerId;
    private long minInterval;
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
                    logger.warning(e.getMessage());
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
        BencodingReader parser = new BencodingReader(con.getInputStream());
        Map<String, Object> response = parser.readNextDictionary();
        con.disconnect();
        Set<Peer> peers = new HashSet<Peer>();
        if (response.containsKey("failure reason")) {
            logger.warning("tracker failure " + (String) response.get("failure reason"));
        } else {
            if (response.containsKey("warning message")) {
                logger.warning("tracker warning " + (String) response.get("warning message"));
            }
            interval = (Long) response.get("interval");
            if (response.containsKey("min interval")) {
                minInterval = (Long) response.get("min interval");
            }
            if (response.containsKey("complete")) {
                complete = (Long) response.get("complete");
            }
            if (response.containsKey("incomplete")) {
                incomplete = (Long) response.get("incomplete");
            }
            if (response.containsKey("tracker id")) {
                trackerId = (String) response.get("tracker id");
            }
            List<Map<String, Object>> prs = (List<Map<String, Object>>) response.get("peers");
            for (Map<String, Object> d : prs) {
                String id = (String) d.get("peer id");
                if (!Arrays.equals(pid, id.getBytes("ISO-8859-1"))) {
                    Peer p = new Peer((String) d.get("ip"),
                            ((Long) d.get("port")).intValue(), id);
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
