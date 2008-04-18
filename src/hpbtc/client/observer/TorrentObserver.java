package hpbtc.client.observer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import hpbtc.client.torrent.BTFile;
import hpbtc.client.message.ProtocolMessage;
import hpbtc.client.peer.Peer;
import hpbtc.client.piece.Piece;

/**
 * @author chris
 *
 */
public interface TorrentObserver {
    
    public void fireServerStartedEvent(int port);
    
    public void fireIncomingConnectionEvent(String ip);
    
    public void fireConnectionFailedEvent(Peer p, IOException e);
    
    public void fireFileCreationEvent(String path, int length);
    
    public void fireSetTrackerURLEvent(List<LinkedList<String>> urls);
    
    public void fireSetPieceLengthEvent(int length);
    
    public void fireSetFilesEvent(List<BTFile> files);
    
    public void fireSetTotalPiecesEvent(int total);
    
    public void fireStartCheckSavedEvent();
    
    public void fireStartSeedingEvent();
    
    public void fireTrackerNotAvailableEvent(String url);
    
    public void fireWaitTrackerEvent(long delay);
    
    public void fireTrackerFailureEvent(String reason);
    
    public void fireTrackerWarningEvent(String warning);
    
    public void fireSetTrackerMinIntervalEvent(int interval);
    
    public void fireSetTrackerIntervalEvent(int interval);
    
    public void fireSetSeedersEvent(int complete);
    
    public void fireSetLeechersEvent(int incomplete);
    
    public void fireSetTotalPeersEvent(int peers);
    
    public void fireSendMessageEvent(ProtocolMessage pm);
    
    public void fireProcessMessageEvent(ProtocolMessage pm);
    
    public void fireHandshakeOKEvent(Peer peer);
    
    public void fireHandshakeErrorEvent(Peer peer);
    
    public void firePIDOKEvent(Peer peer);
    
    public void firePIDErrorEvent(Peer peer);
    
    public void fireSnubEvent(Peer peer, boolean state);
    
    public void fireConnectEvent(Peer peer);
    
    public void fireConnectFailedEvent(Peer peer);
    
    public void fireFinishedPieceEvent(Piece piece);
    
    public void firePieceDiscardedEvent(Piece piece);
    
    public void fireRecoveredPieceEvent(Piece piece);
    
    public void fireFlushPieceEvent(Piece piece);
    
    public void fireRateChangeEvent(Peer peer, int up, int down);
}
