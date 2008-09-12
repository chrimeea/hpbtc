package hpbtc.protocol.network;

import java.io.IOException;

/**
 *
 * @author Cristian Mocanu
 */
public interface Network {

    int connect() throws IOException;

    void disconnect();
}
