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
import hpbtc.util.TrackerUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Tracker {

    public enum Event {

        started, stopped, completed
    };
    private static Logger logger = Logger.getLogger(Tracker.class.getName());
    private String trackerEncoding = "ISO-8859-1";
    private String byteEncoding;
    private int complete;
    private int interval;
    private int incomplete;
    private byte[] trackerId;
    private int minInterval;
    private byte[] infoHash;
    private byte[] pid;
    private int port;
    private List<LinkedList<byte[]>> trackers;
    private long lastTrackerContact;

    public Tracker(final byte[] infoHash, final byte[] pid, final int port,
            final List<LinkedList<byte[]>> trackers, String byteEncoding) {
        this.infoHash = infoHash;
        this.pid = pid;
        this.port = port;
        this.trackers = trackers;
        this.byteEncoding = byteEncoding;
    }

    public Set<Peer> beginTracker(final long bytesLeft) {
        return updateTracker(Event.started, 0, 0, bytesLeft, true);
    }

    public void endTracker(final long uploaded, final long downloaded) {
        updateTracker(Event.completed, uploaded, downloaded, 0, true);
    }

    public Set<Peer> updateTracker(final Event event, final long uploaded,
            final long downloaded, final long bytesLeft, final boolean compact) {
        for (LinkedList<byte[]> ul : trackers) {
            final Iterator<byte[]> i = ul.iterator();
            while (i.hasNext()) {
                byte[] tracker = i.next();
                try {
                    final Set<Peer> peers = connectToTracker(event, tracker,
                            uploaded, downloaded, bytesLeft, compact);
                    lastTrackerContact = System.currentTimeMillis();
                    i.remove();
                    ul.addFirst(tracker);
                    logger.info("Received " + peers.size() + " peers");
                    return peers;
                } catch (IOException e) {
                    logger.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        }
        logger.warning("Could not connect to any tracker");
        return new HashSet<Peer>(0);
    }

    public long getLastTrackerContact() {
        return lastTrackerContact;
    }
    
    private Set<Peer> connectToTracker(final Event event,
            final byte[] tracker, final long uploaded, final long dloaded,
            final long bytesLeft, final boolean compact)
            throws IOException {
        final StringBuilder req = new StringBuilder(new String(tracker,
                byteEncoding));
        req.append("?info_hash=");
        req.append(URLEncoder.encode(new String(infoHash, trackerEncoding),
                trackerEncoding));
        req.append("&peer_id=");
        req.append(URLEncoder.encode(new String(pid, trackerEncoding),
                trackerEncoding));
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
            req.append(URLEncoder.encode(new String(trackerId, trackerEncoding),
                    trackerEncoding));
        }
        final URL track = new URL(req.toString());
        final HttpURLConnection con = (HttpURLConnection) track.openConnection();
        con.setInstanceFollowRedirects(true);
        con.setDoInput(true);
        con.setDoOutput(false);
        con.connect();
        final BencodingReader parser = new BencodingReader(con.getInputStream());
        final Map<byte[], Object> response = parser.readNextDictionary();
        con.disconnect();
        if (response.containsKey("failure reason".getBytes(byteEncoding))) {
            logger.warning(new String((byte[]) response.get("failure reason".
                    getBytes(byteEncoding)), byteEncoding));
        } else {
            if (response.containsKey("warning message".getBytes(byteEncoding))) {
                logger.warning(new String((byte[]) response.get("warning message".
                        getBytes(byteEncoding)), byteEncoding));
            }
            interval = ((Long) response.get("interval".getBytes(byteEncoding))).
                    intValue();
            if (response.containsKey("min interval".getBytes(byteEncoding))) {
                minInterval = ((Long) response.get("min interval".getBytes(
                        byteEncoding))).intValue();
            }
            if (response.containsKey("complete".getBytes(byteEncoding))) {
                complete = ((Long) response.get("complete".getBytes(
                        byteEncoding))).intValue();
            }
            if (response.containsKey("incomplete".getBytes(byteEncoding))) {
                incomplete = ((Long) response.get("incomplete".getBytes(
                        byteEncoding))).intValue();
            }
            if (response.containsKey("tracker id".getBytes(byteEncoding))) {
                trackerId = (byte[]) response.get("tracker id".getBytes(
                        byteEncoding));
            }
            final Object o = response.get("peers".getBytes(byteEncoding));
            return compact ? TrackerUtil.doCompactPeer((byte[]) o) : TrackerUtil.
                    doLoosePeer((List<Map<byte[], Object>>) o, byteEncoding);
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
