/*
 * Created on 26.07.2009
 */
package hpbtc.protocol.dht;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DHTMessage {

    private String byteEncoding = "US-ASCII";
    private byte[] mid;
    private char mtype;
    private byte[] mquery;
    private byte[] remoteId;
    private byte[] targetId;
    private byte[] infohash;
    private int port;
    private byte[] token;

    public DHTMessage(final Map<byte[], Object> message)
            throws UnsupportedEncodingException {
        mid = (byte[]) message.get("t".getBytes(byteEncoding));
        mtype = (char) ((byte[]) message.get("y".getBytes(byteEncoding)))[0];
        Map<byte[], Object> margs;
        if (isQuery()) {
            mquery = (byte[]) message.get("q".getBytes(byteEncoding));
            margs = (Map<byte[], Object>) message.get("a".getBytes(byteEncoding));
            remoteId = (byte[]) margs.get("id".getBytes(byteEncoding));
            if (isFindNodeQuery()) {
                targetId = (byte[]) margs.get("target".getBytes(byteEncoding));
            } else if (isGetPeersQuery()) {
                infohash = (byte[]) margs.get("info_hash".getBytes(byteEncoding));
            } else if (isAnnouncePeerQuery()) {
                port = ((Long) margs.get("port".getBytes(byteEncoding))).intValue();
                token = (byte[]) margs.get("token".getBytes(byteEncoding));
            }
        } else if (isResponse()) {
            margs = (Map<byte[], Object>) message.get("r".getBytes(byteEncoding));
            remoteId = (byte[]) margs.get("id".getBytes(byteEncoding));
        } else if (isError()) {
            final List<Object> merr =
                    (List<Object>) message.get("e".getBytes(byteEncoding));
        }
    }

    public boolean isPingQuery() throws UnsupportedEncodingException {
        return Arrays.equals(mquery, "ping".getBytes(byteEncoding));
    }

    public boolean isFindNodeQuery() throws UnsupportedEncodingException {
        return Arrays.equals(mquery, "find_node".getBytes(byteEncoding));
    }

    public boolean isGetPeersQuery() throws UnsupportedEncodingException {
        return Arrays.equals(mquery, "get_peers".getBytes(byteEncoding));
    }

    public boolean isAnnouncePeerQuery() throws UnsupportedEncodingException {
        return Arrays.equals(mquery, "announce_peer".getBytes(byteEncoding));
    }

    public byte[] getTargetID() {
        return targetId;
    }

    public int getPort() {
        return port;
    }

    public byte[] getInfohash() {
        return infohash;
    }

    public byte[] getToken() {
        return token;
    }

    public byte[] getRemoteID() {
        return remoteId;
    }

    public byte[] getTransactionID() {
        return mid;
    }

    public boolean isQuery() {
        return mtype == 'q';
    }

    public boolean isResponse() {
        return mtype == 'r';
    }

    public boolean isError() {
        return mtype == 'e';
    }
}
