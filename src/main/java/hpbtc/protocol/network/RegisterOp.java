/*
 * Created on 28.07.2009
 */
package hpbtc.protocol.network;

import hpbtc.protocol.network.Register.SELECTOR_TYPE;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class RegisterOp {

    private Map<SELECTOR_TYPE, Integer> operations;
    private Object peer;

    public Map<SELECTOR_TYPE, Integer> getOperations() {
        return operations;
    }

    public Object getPeer() {
        return peer;
    }

    public void setPeer(Object peer) {
        this.peer = peer;
    }

    public RegisterOp(final Object peer) {
        operations = new HashMap<SELECTOR_TYPE, Integer>();
        this.peer = peer;
    }
}
