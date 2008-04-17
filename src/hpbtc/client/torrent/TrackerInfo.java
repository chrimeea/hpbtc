package hpbtc.client.torrent;

import hpbtc.bencoding.BencodingParser;
import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;
import hpbtc.client.Client;
import hpbtc.client.observer.TorrentObserver;
import hpbtc.client.peer.Peer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerInfo {

    public static final int TOTAL_PEERS = 50;
    public static final int DEFAULT_INTERVAL = 15;
    
    private List<LinkedList<String>> trackers;
    private long lastCheck;
    private int minInterval;
    private String trackerId;
    private int interval = DEFAULT_INTERVAL;
    private int complete;
    private int incomplete;
    
    public TrackerInfo(BencodedDictionary meta) {
        if (meta.containsKey("announce-list")) {
            BencodedList bl = (BencodedList) meta.get("announce-list");
            trackers = new ArrayList<LinkedList<String>>(bl.getSize());
            for (BencodedElement ul : bl) {
                BencodedList x = (BencodedList) ul;
                LinkedList<String> z = new LinkedList<String>();
                for (BencodedElement y : x) {
                    String u = ((BencodedString) y).getValue();
                    z.add(u);
                }
                Collections.shuffle(z);
                trackers.add(z);
            }
        } else {
            trackers = new ArrayList<LinkedList<String>>(1);
            LinkedList<String> ul = new LinkedList<String>();
            String u = ((BencodedString) meta.get("announce")).getValue();
            ul.add(u);
            trackers.add(ul);
        }
    }

    public List<LinkedList<String>> getTrackers() {
        return trackers;
    }
    
    public Set<Peer> tryGetTrackerPeers(String event, int uploaded, int downloaded,
            byte[] infoHash, int bytesLeft) {
        for (LinkedList<String> ul : trackers) {
            Iterator<String> i = ul.iterator();
            while (i.hasNext()) {
                String tracker = i.next();
                try {
                    Set<Peer> lastPeers = getTrackerPeers(event, tracker, uploaded, downloaded, infoHash, bytesLeft);
                    i.remove();
                    ul.addFirst(tracker);
                    return lastPeers;
                } catch (IOException e) {
                    Client.getInstance().getObserver().fireTrackerNotAvailableEvent(tracker);
                }
            }
        }
        return new HashSet<Peer>();
    }

    private Set<Peer> getTrackerPeers(String event, String tracker, int uploaded,
            int downloaded, byte[] infoHash, int bytesLeft) throws IOException {
        Client client = Client.getInstance();
        TorrentObserver to = client.getObserver();
        long l = System.currentTimeMillis();
        long h = l - lastCheck;
        long w = minInterval * 1000;
        while (h < w) {
            to.fireWaitTrackerEvent(w - h);
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
        req.append(URLEncoder.encode(client.getPID(), "ISO-8859-1"));
        req.append("&port=");
        req.append(client.getPort());
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
                trackerId = ((BencodedString) response.get("tracker id")).getValue();
            }
            BencodedList prs = (BencodedList) response.get("peers");
            for (BencodedElement e : prs) {
                BencodedDictionary d = (BencodedDictionary) e;
                BencodedString beid = (BencodedString) d.get("peer id");
                String id = beid.getValue();
                if (!Arrays.equals(client.getPIDBytes(), beid.getBytes())) {
                    Peer p = new Peer(((BencodedString) d.get("ip")).getValue(),
                            ((BencodedInteger) d.get("port")).getValue(), id);
                    peers.add(p);
                }
            }
        }
        lastCheck = l;
        return peers;
    }

    public int getInterval() {
        return interval;
    }
}
