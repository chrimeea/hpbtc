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
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerTest {

    private String byteEncoding = "US-ASCII";

    @Test
    public void testConnectToTracker() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(8000),
                0);
        server.createContext("/test", new HttpHandler() {

            public void handle(HttpExchange t) throws IOException {
                try {
                    assert t.getRequestURI().equals(
                            new URI(
                            "/test?info_hash=INFOHASH&peer_id=PID&port=2000&uploaded=1&downloaded=2&left=3&compact=0&event=started"));
                } catch (URISyntaxException e) {
                    assert false;
                }
                final String response =
                        "d8:intervali10e12:min intervali5e10:tracker id3:foo8:completei20e10:incompletei9e5:peersld7:peer id2:1P2:ip9:localhost4:porti9000eed7:peer id2:2P2:ip9:localhost4:porti3003eeee";
                t.sendResponseHeaders(200, response.length());
                final OutputStream os = t.getResponseBody();
                os.write(response.getBytes(byteEncoding));
                os.close();
            }
        });
        server.setExecutor(null);
        server.start();
        final LinkedList<LinkedList<byte[]>> t =
                new LinkedList<LinkedList<byte[]>>();
        final LinkedList<byte[]> l = new LinkedList<byte[]>();
        l.add("http://localhost:8001/test".getBytes(byteEncoding));
        l.add("http://localhost:8000/test".getBytes(byteEncoding));
        t.add(l);
        final Tracker ti = new Tracker("INFOHASH".getBytes(byteEncoding),
                "PID".getBytes(byteEncoding), 2000, t, byteEncoding);
        final Iterable<Peer> peers = ti.updateTracker(Tracker.Event.started, 1,
                2, 3,
                false);
        server.stop(0);
        assert ti.getInterval() == 10;
        assert ti.getMinInterval() == 5;
        assert ti.getComplete() == 20;
        assert ti.getIncomplete() == 9;
        for (Peer p : peers) {
            assert Arrays.equals(p.getId(), "1P".getBytes(byteEncoding)) ||
                    Arrays.equals(p.getId(), "2P".getBytes(byteEncoding));
        }
    }
}
