package hpbtc.protocol.torrent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerInfoTest {

    @Test
    public void testConnectToTracker() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new HttpHandler() {
            public void handle(HttpExchange t) throws IOException {
                try {
                    assert t.getRequestURI().equals(new URI("/test?info_hash=INFOHASH&peer_id=PID&port=2000&uploaded=1&downloaded=2&left=3&numwant=4&event=started"));
                } catch (URISyntaxException e) {
                    assert false;
                }
                String response = "X";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes("US-ASCII"));
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
        TrackerInfo ti = new TrackerInfo("INFOHASH".getBytes("US-ASCII"),
                "PID".getBytes("US-ASCII"), 2000, t);
        ti.updateTracker("started", 1, 2, 3, 4);
        server.stop(0);
    }
}
