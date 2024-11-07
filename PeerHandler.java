import java.io.*;
import java.net.*;
import java.io.IOException;

public class PeerHandler implements Runnable {
    private Socket clientSocket;
    private Node node;

    public PeerHandler(Socket socket, Node node) {
        this.clientSocket = socket;
        this.node = node;
    }

    @Override
    public void run() {
        try {
            // Handle communication with the peer
            // After successful connection, add the peer
            String peerIp = clientSocket.getInetAddress().getHostAddress();
            int peerPort = clientSocket.getPort();
            node.addPeer(new Node(peerIp, peerPort));

            // Handle HTTP request (assuming you are doing this in the same method)
            node.handleHttpRequest(clientSocket);
        } catch (IOException e) {
            System.err.println("Error handling peer: " + e.getMessage());
        }
    }
}
