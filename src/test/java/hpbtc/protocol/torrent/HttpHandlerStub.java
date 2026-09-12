/*
 * Created on 27.09.2008
 */
package hpbtc.protocol.torrent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class HttpHandlerStub implements HttpHandler {

    private URI[] requests;
    private byte[][] responses;
    private int index;
    private int last;
    private String byteEncoding;

    public HttpHandlerStub(final int n, final String byteEncoding) {
        requests = new URI[n];
        responses = new byte[n][];
        this.byteEncoding = byteEncoding;
    }

    public HttpHandlerStub(final String request, final String response,
            final String byteEncoding) throws URISyntaxException,
            UnsupportedEncodingException {
        this(1, byteEncoding);
        addExpectation(request, response);
    }
    
    public void addExpectation(final String request, final String response)
            throws URISyntaxException, UnsupportedEncodingException {
        requests[last] = new URI(request);
        responses[last++] = response.getBytes(byteEncoding);
    }

    public void handle(final HttpExchange t) throws IOException {
        assert t.getRequestURI().equals(requests[index]);
        final byte[] resp = responses[index++];
        t.sendResponseHeaders(200, resp.length);
        final OutputStream os = t.getResponseBody();
        os.write(resp);
        os.close();
    }
}
