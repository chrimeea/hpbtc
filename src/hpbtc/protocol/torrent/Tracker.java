package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.TrackerUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Tracker {

    public enum Event {

        started, stopped, completed
    };
    private static Logger logger = Logger.getLogger(Tracker.class.getName());
    private int complete;
    private int interval;
    private int incomplete;
    private String trackerId;
    private int minInterval;
    private byte[] infoHash;
    private byte[] pid;
    private int port;
    private List<LinkedList<String>> trackers;

    public Tracker(final byte[] infoHash, final byte[] pid, final int port,
            final List<LinkedList<String>> trackers) {
        this.infoHash = infoHash;
        this.pid = pid;
        this.port = port;
        this.trackers = trackers;
    }

    public Set<Peer> beginTracker(final long bytesLeft) {
        return updateTracker(Event.started, 0, 0, bytesLeft, true);
    }

    public void endTracker(final long uploaded, final long downloaded) {
        updateTracker(Event.completed, uploaded, downloaded, 0, true);
    }

    public Set<Peer> updateTracker(final Event event, final long uploaded,
            final long downloaded, final long bytesLeft, final boolean compact) {
        for (LinkedList<String> ul : trackers) {
            Iterator<String> i = ul.iterator();
            while (i.hasNext()) {
                String tracker = i.next();
                try {
                    Set<Peer> peers = connectToTracker(event, tracker, uploaded,
                            downloaded, bytesLeft, compact);
                    i.remove();
                    ul.addFirst(tracker);
                    return peers;
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }
        }
        return null;
    }

    private Set<Peer> connectToTracker(final Event event,
            final String tracker, final long uploaded, final long dloaded,
            final long bytesLeft, final boolean compact)
            throws IOException {
        StringBuilder req = new StringBuilder(tracker);
        req.append("?info_hash=");
        req.append(URLEncoder.encode(new String(infoHash, "ISO-8859-1"),
                "ISO-8859-1"));
        req.append("&peer_id=");
        req.append(
                URLEncoder.encode(new String(pid, "ISO-8859-1"), "ISO-8859-1"));
        req.append("&port=");
        req.append(port);
        req.append("&uploaded=");
        req.append(uploaded);
        req.append("&downloaded=");
        req.append(dloaded);
        req.append("&left=");
        req.append(bytesLeft);
        req.append("&compact=");
        req.append(compact ? "1" : "0");
        if (event != null) {
            req.append("&event=");
            req.append(event.name());
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
            return compact ? TrackerUtil.doCompactPeer(response, infoHash) :
                TrackerUtil.doLoosePeer(response, infoHash);
        }
        return new HashSet<Peer>();
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
