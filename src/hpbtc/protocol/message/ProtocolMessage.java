/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public interface ProtocolMessage {
          
    void process(MessageProcessor processor);
    
    ByteBuffer send();
}
