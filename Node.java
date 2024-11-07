import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Node implements NetworkDiscovery {
    private String ipAddress;
    private int port;
    private List<Node> peers;
    private List<Block> blockchain;
    private List<File> mediaFiles;

    // Constructor that initializes the Node with a dynamically detected IP address and a random port
    public Node(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port; // Find an open port for the server
        this.peers = new ArrayList<>();
        this.blockchain = new ArrayList<>();
        this.mediaFiles = new ArrayList<>();
        initializeGenesisBlock();
    }

    // Method to get the local IP address
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    // Check if the address is not a loopback address and is IPv4
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress(); // Return the first valid IP found
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error getting local IP address: " + e.getMessage());
        }
        return "127.0.0.1"; // Fallback to localhost if no IP is found
    }

    // Method to find an open port dynamically
    private int findOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort(); // Get the dynamically allocated port
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 8080; // Fallback to a default port
    }

    // Initialize a genesis block
    private void initializeGenesisBlock() {
        Block genesisBlock = new Block("Genesis Block", "0", "", "", 0);
        blockchain.add(genesisBlock);
    }

    @Override
    public void discoverAndConnect() {
        final int DISCOVERY_PORT = 9876; // Port for discovery
        final String MULTICAST_ADDRESS = "230.0.0.0"; // Multicast address for discovery
        final int DISCOVERY_TIMEOUT = 5000; // Timeout for discovery response in milliseconds

        try {
            // Create a MulticastSocket for sending discovery requests
            MulticastSocket multicastSocket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket.joinGroup(group);

            // Broadcast a discovery message
            String discoveryMessage = "DISCOVER_NODE_REQUEST:" + ipAddress + ":" + port; // Custom message format
            byte[] buffer = discoveryMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, DISCOVERY_PORT);
            multicastSocket.send(packet); // Send the discovery request

            // Prepare to receive responses
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            multicastSocket.setSoTimeout(DISCOVERY_TIMEOUT); // Set timeout for receiving responses

            System.out.println("Listening for peer responses...");

            // Wait for responses
            while (true) {
                try {
                    multicastSocket.receive(responsePacket); // Receive response
                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    System.out.println("Received response from peer: " + response);

                    // Parse the response to get peer details
                    String[] responseParts = response.split(":");
                    if (responseParts.length == 3) {
                        String peerIp = responseParts[1];
                        int peerPort = Integer.parseInt(responseParts[2]);

                        // Connect to the discovered peer
                        connectToPeer(peerIp, peerPort);
                    }
                } catch (IOException e) {
                    // If the timeout is reached, break out of the loop
                    break;
                }
            }

            multicastSocket.leaveGroup(group); // Leave the multicast group
            multicastSocket.close(); // Close the multicast socket
        } catch (Exception e) {
            System.err.println("Error during peer discovery: " + e.getMessage());
        }
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on " + ipAddress + ":" + port);
            while (true) {
                // Accept incoming connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection accepted from " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread to manage communication with the new peer
                new Thread(new PeerHandler(clientSocket, this)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    public List<Node> getPeers() {
        return peers; // Return the list of connected peers
    }

    public void addPeer(Node peer) {
        peers.add(peer); // Add the new peer to the list
        System.out.println("Peer added: " + peer.getIpAddress() + ":" + peer.getPort());
        System.out.println("Total peers connected: " + peers.size()); // Log the peer count
    
        // Automatically start mining when a new peer connects
        String data = "New block data"; // Replace with actual data
        String mediaFilePath = "/path/to/media/file"; // Replace with actual media file path
        String mediaType = "image/jpeg"; // Replace with actual media type
        long mediaSize = 1024; // Replace with actual media size
    
        // Call the mining method with gathered data
        startMining(data, mediaFilePath, mediaType, mediaSize);
    }
    

    private boolean connectToPeer(String ip, int port) {
        try {
            Node newPeer = new Node(ip, port); // Create a new Node instance for the peer
            newPeer.setIpAddress(ip);
            newPeer.setPort(port);
            peers.add(newPeer);
            System.out.println("Connected to peer: " + ip + ":" + port);
            return true;
        } catch (Exception e) {
            System.out.println("Failed to connect to peer: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onPeerConnected() {
        startMining("Peer connection block", null, null, 0); // Start mining with no media
        System.out.println("Mining started after peer connection...");
    }

    public void startMining(String data, String mediaFilePath, String mediaType, long mediaSize) {
        Block previousBlock = blockchain.get(blockchain.size() - 1);
        Block newBlock = new Block(data, previousBlock.hash, mediaFilePath, mediaType, mediaSize);
        newBlock.mineBlock(4); // Example difficulty, adjust as necessary
        blockchain.add(newBlock);
        System.out.println("Mined new block: " + newBlock);
    }

    public List<Block> getBlockchain() {
        return blockchain;
    }

    public void uploadMedia(File mediaFile) {
        mediaFiles.add(mediaFile);
        System.out.println("Media file uploaded: " + mediaFile.getName());

        // Optionally, mine a block for the uploaded media
        startMining("Uploaded media: " + mediaFile.getName(), mediaFile.getAbsolutePath(), "media", mediaFile.length());
    }

    public List<File> getMediaFiles() {
        return mediaFiles;
    }

    public void shutdown() {
        System.out.println("Shutting down node...");
    }

    public String getIpAddress() {
        return ipAddress; // Getter for the IP address
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress; // Setter for the IP address
    }

    public int getPort() {
        return port; // Getter for the port
    }

    public void setPort(int port) {
        this.port = port; // Setter for the port
    }

    // New method to handle HTTP requests for node status and connected peers
    public void handleHttpRequest(Socket clientSocket) throws IOException {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
    
            String inputLine;
            StringBuilder request = new StringBuilder();
            while (!(inputLine = in.readLine()).isEmpty()) {
                request.append(inputLine).append("\n");
            }
            System.out.println("Received request: " + request.toString()); // Debugging line
    
            // Simple request handling
            if (request.toString().startsWith("GET /status")) {
                StringBuilder response = new StringBuilder();
                response.append("HTTP/1.1 200 OK\r\n");
                response.append("Content-Type: text/plain\r\n");
                response.append("\r\n");
                response.append("Node IP: ").append(ipAddress).append("\n");
                response.append("Node Port: ").append(port).append("\n");
                response.append("Connected Peers: ").append(peers.size()).append("\n");
                
                for (Node peer : peers) {
                    response.append(peer.getIpAddress()).append(":").append(peer.getPort()).append("\n");
                }
                List<Block> getBl=getBlockchain();
                response.append("Blockchain Data"+getBl);
                out.print(response.toString());
                out.flush(); // Ensure data is sent immediately
            } else {
                out.print("HTTP/1.1 404 Not Found\r\n");
                out.print("Content-Type: text/plain\r\n");
                out.print("\r\n");
                out.print("404 Not Found\n");
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Error handling HTTP request: " + e.getMessage());
        } finally {
            clientSocket.close(); // Close the client socket
        }
    }
    

    // Method to zip media files
    public void zipMediaFiles(String zipFilePath) {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : mediaFiles) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
            System.out.println("Media files zipped successfully: " + zipFilePath);
        } catch (IOException e) {
            System.err.println("Error zipping media files: " + e.getMessage());
        }
    }

    // Inner class for handling peer connections


    
  }

