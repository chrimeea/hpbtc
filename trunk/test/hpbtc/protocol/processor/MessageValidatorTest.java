/*
 * Created on 25.09.2008
 */
package hpbtc.protocol.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageValidatorTest {

    private String encoding = "US-ASCII";
    private byte[] protocol;

    @Before
    public void before() {
        try {
            protocol = "PROT".getBytes(encoding);
        } catch (UnsupportedEncodingException ex) {
            assert false;
        }
    }

    @Test
    public void testValidateHandshakeMessage() throws
            UnsupportedEncodingException, IOException, NoSuchAlgorithmException {
        final Peer peer = new Peer(InetSocketAddress.createUnresolved(
                "localhost", 6000));
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(encoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        final List<Torrent> t = new ArrayList<Torrent>(1);
        final HandshakeMessage hm = new HandshakeMessage(null, protocol, peer,
                info.getInfoHash());
        final MessageValidator v = new MessageValidator(t, protocol);
        assert !v.validateHandshakeMessage(hm);
        t.add(info);
        assert v.validateHandshakeMessage(hm);
    }

    @Test
    public void testValidateBitfieldMessage() throws
            UnsupportedEncodingException, IOException, NoSuchAlgorithmException {
        final Peer peer = new Peer(InetSocketAddress.createUnresolved(
                "localhost", 6000));
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(encoding));
        Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        peer.setTorrent(info);
        final BitSet bs = new BitSet(info.getNrPieces());
        final MessageValidator v = new MessageValidator(null, protocol);
        BitfieldMessage bm = new BitfieldMessage(bs, info.getNrPieces(), peer);
        assert v.validateBitfieldMessage(bm);
        bm = new BitfieldMessage(bs, info.getNrPieces() - 1, peer);
        assert !v.validateBitfieldMessage(bm);
    }

    @Test
    public void testValidateCancelMessage() throws
            UnsupportedEncodingException, IOException, NoSuchAlgorithmException {
        final Peer peer = new Peer(InetSocketAddress.createUnresolved(
                "localhost", 6000));
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(encoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        peer.setTorrent(info);
        final MessageValidator v = new MessageValidator(null, protocol);
        BlockMessage bm = new BlockMessage(0, 0, info.getPieceLength(),
                SimpleMessage.TYPE_CANCEL, peer);
        assert v.validateCancelMessage(bm);
        bm = new BlockMessage(0, info.getNrPieces(), info.getPieceLength(),
                SimpleMessage.TYPE_CANCEL, peer);
        assert !v.validateCancelMessage(bm);
    }

    @Test
    public void testValidateHaveMessage() throws
            UnsupportedEncodingException, IOException, NoSuchAlgorithmException {
        final Peer peer = new Peer(InetSocketAddress.createUnresolved(
                "localhost", 6000));
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(encoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        peer.setTorrent(info);
        final MessageValidator v = new MessageValidator(null, protocol);
        HaveMessage hm = new HaveMessage(0, peer);
        assert v.validateHaveMessage(hm);
        hm = new HaveMessage(info.getNrPieces(), peer);
        assert !v.validateHaveMessage(hm);
    }

    @Test
    public void testValidatePieceMessage() throws
            UnsupportedEncodingException, IOException, NoSuchAlgorithmException {
        final Peer peer = new Peer(InetSocketAddress.createUnresolved(
                "localhost", 6000));
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(encoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        peer.setTorrent(info);
        final MessageValidator v = new MessageValidator(null, protocol);
        int l = info.getPieceLength();
        PieceMessage pm = new PieceMessage(0, 0, 85, peer);
        assert v.validatePieceMessage(pm);
        pm = new PieceMessage(l, 0, l, peer);
        assert !v.validatePieceMessage(pm);
    }

    @Test
    public void testValidateRequestMessage() throws
            UnsupportedEncodingException, IOException, NoSuchAlgorithmException {
        final Peer peer = new Peer(InetSocketAddress.createUnresolved(
                "localhost", 6000));
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(encoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        peer.setTorrent(info);
        final MessageValidator v = new MessageValidator(null, protocol);
        final BlockMessage bm = new BlockMessage(0, 0, info.getPieceLength(),
                SimpleMessage.TYPE_REQUEST, peer);
        assert !v.validateRequestMessage(bm);
    }
}
