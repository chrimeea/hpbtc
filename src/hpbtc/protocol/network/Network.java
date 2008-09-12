package hpbtc.protocol.network;

import java.io.IOException;

/**
 *
 * @author chris
 */
public interface Network {

    int connect() throws IOException;

    void disconnect();
}
