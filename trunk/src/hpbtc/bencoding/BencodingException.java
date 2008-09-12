/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc.bencoding;

import java.io.IOException;

/**
 * @author Cristian Mocanu
 *
 */
public class BencodingException extends IOException {

    private static final long serialVersionUID = -8478964499306440597L;

    /**
     * @param message
     */
    public BencodingException(String message) {
        super(message);
    }
}
