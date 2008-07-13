package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
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
public class Tracker {

    public enum Event {started, stopped, completed};
    
    private static Logger logger = Logger.getLogger(Tracker.class.getName());
    private static final int TOTAL_PEERS = 50;
    private int complete;
    private int interval;
    private int incomplete;
    private String trackerId;
    private int minInterval;
    private byte[] infoHash;
    private byte[] pid;
    private int port;
    private List<LinkedList<String>> trackers;
    private String encoding;

    public Tracker(byte[] infoHash, byte[] pid, int port, List<LinkedList<String>> trackers) {
        this.infoHash = infoHash;
        this.pid = pid;
        this.port = port;
        this.trackers = trackers;
        this.encoding = "UTF-8";
    }

    public Set<Peer> beginTracker(int bytesLeft) {
        return updateTracker(Event.started, 0, 0, bytesLeft, TOTAL_PEERS);
    }
    
    public void endTracker(int uploaded, int downloaded) {
        updateTracker(Event.completed, uploaded, downloaded, 0, 0);
    }
    
    public Set<Peer> updateTracker(Event event, int uploaded, int downloaded, int bytesLeft, int totalPeers) {
        for (LinkedList<String> ul : trackers) {
            Iterator<String> i = ul.iterator();
            while (i.hasNext()) {
                String tracker = i.next();
                try {
                    Set<Peer> peers = connectToTracker(event, tracker, uploaded, downloaded, bytesLeft, totalPeers);
                    i.remove();
                    ul.addFirst(tracker);
                    return peers;
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
                }
            }
        }
        return null;
    }

    private Set<Peer> connectToTracker(Event event, String tracker, int uploaded,
            int dloaded, int bytesLeft, int totalPeers) throws IOException {
        StringBuilder req = new StringBuilder(tracker);
        req.append("?info_hash=");
        req.append(URLEncoder.encode(new String(infoHash, encoding), encoding));
        req.append("&peer_id=");
        req.append(URLEncoder.encode(new String(pid, encoding), encoding));
        req.append("&port=");
        req.append(port);
        req.append("&uploaded=");
        req.append(uploaded);
        req.append("&downloaded=");
        req.append(dloaded);
        req.append("&left=");
        req.append(bytesLeft);
        req.append("&numwant=");
        req.append(totalPeers);
        if (event != null) {
            req.append("&event=");
            req.append(event.name());
        }
        if (trackerId != null) {
            req.append("trackerid");
            req.append(URLEncoder.encode(trackerId, encoding));
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
            logger.warning((String) response.get("failure reason"));
        } else {
            if (response.containsKey("warning message")) {
                logger.warning((String) response.get("warning message"));
            }
            interval = (Integer) response.get("interval");   
            if (response.containsKey("min interval")) {
                minInterval = (Integer) response.get("min interval");
            }
            if (response.containsKey("complete")) {
                complete = (Integer) response.get("complete");
            }
            if (response.containsKey("incomplete")) {
                incomplete = (Integer) response.get("incomplete");
            }
            if (response.containsKey("tracker id")) {
                trackerId = (String) response.get("tracker id");
            }
            List<Map<String, Object>> prs = (List<Map<String, Object>>) response.get("peers");
            for (Map<String, Object> d : prs) {
                peers.add(new Peer(new InetSocketAddress((String) d.get("ip"),
                        ((Integer) d.get("port")).intValue()), ((String) d.get("peer id")).getBytes(encoding)));
            }
        }
        return peers;
    }

    public int getComplete() {
        return complete;
    }

    public int getIncomplete() {
        return incomplete;
    }

    public int getInterval() {
        return interval;
    }

    public int getMinInterval() {
        return minInterval;
    }
}