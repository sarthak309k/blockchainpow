public interface NetworkDiscovery {
    void discoverAndConnect();    // Method for discovering peers and connecting
    void onPeerConnected();       // Event handler for when a new peer connects
}
