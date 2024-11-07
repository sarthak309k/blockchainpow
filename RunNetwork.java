import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunNetwork {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        final Node node; // Declare node but do not initialize it yet

        try {
            // Automatically detect the user's IP address
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            // Automatically find an available port
            int port = findFreePort();
            
            System.out.println("Node initialized at " + ipAddress + ":" + port);

            // Initialize the node
            node = new Node(ipAddress, port);

            // Start the server and discover peers
            Executors.newSingleThreadExecutor().execute(node::startServer);
            Executors.newSingleThreadExecutor().execute(node::discoverAndConnect);

            AtomicBoolean isMining = new AtomicBoolean(false);
            String uploadedFilePath = null; // Variable to store the uploaded file path

            while (true) {
                System.out.println("\nChoose an option:\n1. Upload Media\n2. Start Mining\n3. Stop Mining\n4. View Blockchain\n5. View Media Files\n6. Exit");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        System.out.print("Enter the path of the media file to upload: ");
                        String filePath = scanner.nextLine();
                        File mediaFile = new File(filePath);
                        if (mediaFile.exists()) {
                            node.uploadMedia(mediaFile); // Ensure this method is correctly implemented in Node
                            uploadedFilePath = mediaFile.getAbsolutePath(); // Store uploaded file path
                            System.out.println("Media file uploaded successfully.");
                        } else {
                            System.out.println("File does not exist. Please try again.");
                        }
                        break;

                    case 2:
                        // Start mining only if the connection is established
                        if (!isMining.get() && node.getPeers().size() > 0 && uploadedFilePath != null) {
                            isMining.set(true);

                            // Extract parameters from the uploaded media file
                            try {
                                MiningParameters params = extractParametersFromFile(uploadedFilePath);
                                final AtomicBoolean miningFlag = isMining; // Declare final for the mining flag
                                Executors.newSingleThreadExecutor().execute(() -> node.startMining(params.data, params.mediaFilePath, params.mediaType, params.mediaSize));
                                System.out.println("Mining started...");
                            } catch (IOException e) {
                                System.err.println("Error reading media file: " + e.getMessage());
                            }
                        } else if (isMining.get()) {
                            System.out.println("Mining is already running.");
                        } else {
                            System.out.println("No peers connected or no file uploaded. Cannot start mining.");
                        }
                        break;

                    case 3:
                        if (isMining.get()) {
                            isMining.set(false);
                            System.out.println("Mining stopped.");
                        } else {
                            System.out.println("Mining is not currently running.");
                        }
                        break;

                    case 4:
                        System.out.println("Current Blockchain:");
                        displayBlockchain(node);
                        break;

                    case 5:
                        System.out.println("Current Media Files:");
                        displayMediaFiles(node);
                        break;

                    case 6:
                        isMining.set(false);
                        node.shutdown(); // Ensure this shuts down server and connections properly
                        System.out.println("Exiting program.");
                        return; // Use return to exit the main method properly

                    default:
                        System.out.println("Invalid option, please try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing node: " + e.getMessage());
        } finally {
            scanner.close(); // Ensure the scanner is closed even in case of exceptions
        }
    }

    // Method to find a free port
    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Unable to find free port", e);
        }
    }

    private static void displayBlockchain(Node node) {
        List<Block> blockchain = node.getBlockchain();
        if (blockchain.isEmpty()) {
            System.out.println("Blockchain is empty.");
        } else {
            for (Block block : blockchain) {
                System.out.println(block);
            }
        }
    }

    private static void displayMediaFiles(Node node) {
        List<File> mediaFiles = node.getMediaFiles();
        if (mediaFiles.isEmpty()) {
            System.out.println("No media files uploaded.");
        } else {
            for (File mediaFile : mediaFiles) {
                System.out.println(mediaFile.getName());
            }
        }
    }

    // Method to extract parameters from the uploaded file
    private static MiningParameters extractParametersFromFile(String filePath) throws IOException {
        try (Scanner fileScanner = new Scanner(new File(filePath))) {
            // Assuming the file structure is predefined and each line contains relevant data
            String data = fileScanner.nextLine(); // First line: data for the block
            String mediaFilePath = fileScanner.nextLine(); // Second line: media file path
            String mediaType = fileScanner.nextLine(); // Third line: media type
            long mediaSize = Long.parseLong(fileScanner.nextLine()); // Fourth line: media size

            return new MiningParameters(data, mediaFilePath, mediaType, mediaSize);
        } catch (FileNotFoundException e) {
            throw new IOException("File not found: " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new IOException("Error parsing media size: " + e.getMessage());
        }
    }

    // Class to hold mining parameters
    private static class MiningParameters {
        String data;
        String mediaFilePath;
        String mediaType;
        long mediaSize;

        MiningParameters(String data, String mediaFilePath, String mediaType, long mediaSize) {
            this.data = data;
            this.mediaFilePath = mediaFilePath;
            this.mediaType = mediaType;
            this.mediaSize = mediaSize;
        }
    }
}
