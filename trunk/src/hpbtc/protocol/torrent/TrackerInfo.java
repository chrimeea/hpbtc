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
public class TrackerInfo {

    private static Logger logger = Logger.getLogger(TrackerInfo.class.getName());
    private long complete;
    private long interval;
    private long incomplete;
    private String trackerId;
    private long minInterval;
    private byte[] infoHash;
    private byte[] pid;
    private int port;
    private List<LinkedList<String>> trackers;
    private String encoding;
    private Set<Peer> peers;

    public TrackerInfo(byte[] infoHash, byte[] pid, int port, List<LinkedList<String>> trackers) {
        this.infoHash = infoHash;
        this.pid = pid;
        this.port = port;
        this.trackers = trackers;
        this.encoding = "ISO-8859-1";
    }

    public void updateTracker(String event, int uploaded, int downloaded, int bytesLeft, int totalPeers) {
        for (LinkedList<String> ul : trackers) {
            Iterator<String> i = ul.iterator();
            while (i.hasNext()) {
                String tracker = i.next();
                try {
                    connectToTracker(event, tracker, uploaded, downloaded, bytesLeft, totalPeers);
                    i.remove();
                    ul.addFirst(tracker);
                    break;
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
                }
            }
        }
    }

    private void connectToTracker(String event, String tracker, int uploaded,
            int downloaded, int bytesLeft, int totalPeers) throws IOException {
        StringBuilder req = new StringBuilder(tracker);
        req.append("?info_hash=");
        req.append(infoHash);
        req.append("&peer_id=");
        req.append(pid);
        req.append("&port=");
        req.append(port);
        req.append("&uploaded=");
        req.append(uploaded);
        req.append("&downloaded=");
        req.append(downloaded);
        req.append("&left=");
        req.append(bytesLeft);
        req.append("&numwant=");
        req.append(totalPeers);
        if (event != null) {
            req.append("&event=");
            req.append(event);
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
        peers = new HashSet<Peer>();
        if (response.containsKey("failure reason")) {
            logger.warning((String) response.get("failure reason"));
        } else {
            if (response.containsKey("warning message")) {
                logger.warning((String) response.get("warning message"));
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
                peers.add(new Peer(new InetSocketAddress((String) d.get("ip"),
                        ((Long) d.get("port")).intValue()), (String) d.get("peer id")));
            }
        }
    }

    public long getComplete() {
        return complete;
    }

    public long getIncomplete() {
        return incomplete;
    }

    public long getInterval() {
        return interval;
    }

    public long getMinInterval() {
        return minInterval;
    }

    public Set<Peer> getPeers() {
        return peers;
    }
}
