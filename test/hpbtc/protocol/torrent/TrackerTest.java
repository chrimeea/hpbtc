package hpbtc.protocol.torrent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerTest {

    @Test
    public void testConnectToTracker() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new HttpHandler() {

            public void handle(HttpExchange t) throws IOException {
                try {
                    assert t.getRequestURI().equals(
                            new URI(
                            "/test?info_hash=INFOHASH&peer_id=PID&port=2000&uploaded=1&downloaded=2&left=3&numwant=4&compact=0&event=started"));
                } catch (URISyntaxException e) {
                    assert false;
                }
                String response =
                        "d8:intervali10e12:min intervali5e10:tracker id3:foo8:completei20e10:incompletei9e5:peersld7:peer id2:1P2:ip9:localhost4:porti9000eed7:peer id2:2P2:ip9:localhost4:porti3003eeee";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes("ISO-8859-1"));
                os.close();
            }
        });
        server.setExecutor(null);
        server.start();
        LinkedList<LinkedList<String>> t = new LinkedList<LinkedList<String>>();
        LinkedList<String> l = new LinkedList<String>();
        l.add("http://localhost:8001/test");
        l.add("http://localhost:8000/test");
        t.add(l);
        Tracker ti = new Tracker("INFOHASH".getBytes("ISO-8859-1"),
                "PID".getBytes("ISO-8859-1"), 2000, t);
        Set<Peer> peers = ti.updateTracker(Tracker.Event.started, 1, 2, 3, 4,
                false);
        server.stop(0);
        assert ti.getInterval() == 10;
        assert ti.getMinInterval() == 5;
        assert ti.getComplete() == 20;
        assert ti.getIncomplete() == 9;
        assert peers.size() == 2;
        for (Peer p : peers) {
            assert Arrays.equals(p.getId(), "1P".getBytes("ISO-8859-1")) ||
                    Arrays.equals(p.getId(), "2P".getBytes("ISO-8859-1"));
        }
    }
}
