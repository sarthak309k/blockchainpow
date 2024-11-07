import java.security.MessageDigest;
import java.util.Date;

public class Block {
    public String hash;           // The hash of the current block
    public String previousHash;   // The hash of the previous block
    public String data;           // The data stored in the block
    public long timestamp;        // The time when the block was created
    public int nonce;             // A number used for mining the block
    public String mediaFilePath;  // Path to the media file
    public String mediaType;      // Type of the media (e.g., video, audio)
    public long mediaSize;        // Size of the media file in bytes

    // Constructor to create a new block with the provided data, previous hash, and media info
    public Block(String data, String previousHash, String mediaFilePath, String mediaType, long mediaSize) {
        this.data = data;
        this.previousHash = previousHash;
        this.timestamp = new Date().getTime();
        this.nonce = 0; // Initialize nonce to 0
        this.hash = calculateHash(); // Calculate the hash upon creation
        this.mediaFilePath = mediaFilePath; // Set media file path
        this.mediaType = mediaType; // Set media type
        this.mediaSize = mediaSize; // Set media size
    }

    // Method to calculate the hash of the block
    public String calculateHash() {
        String input = previousHash + Long.toString(timestamp) + Integer.toString(nonce) + data + mediaFilePath + mediaType + mediaSize;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // SHA-256 hashing algorithm
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b)); // Convert byte to hex
            }
            return hexString.toString(); // Return the hash as a hex string
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method to mine the block by finding a hash that starts with a specific number of zeros (difficulty)
    public void mineBlock(int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0'); // Create target string
        while (!hash.substring(0, difficulty).equals(target)) { // Keep hashing until the target is met
            nonce++; // Increment nonce
            hash = calculateHash(); // Recalculate the hash
        }
        System.out.println("Block mined: " + hash); // Log mined block hash
    }

    // Method to get the data of the block
    public String getData() {
        return data; // Return the data stored in the block
    }

    // Convert block details to a string representation
    @Override
    public String toString() {
        return previousHash + ";" + data + ";" + timestamp + ";" + nonce + ";" + hash + ";" + mediaFilePath + ";" + mediaType + ";" + mediaSize;
    }

    // Create a Block object from its string representation
    public static Block fromString(String data) {
        try {
            String[] parts = data.split(";"); // Adjust delimiter to match the toString representation
            if (parts.length != 8) {  // Ensure we have all expected parts
                throw new IllegalArgumentException("Invalid block format.");
            }
    
            String previousHash = parts[0];
            String blockData = parts[1];
            long timestamp = Long.parseLong(parts[2]);
            int nonce = Integer.parseInt(parts[3]);
            String hash = parts[4];
            String mediaFilePath = parts[5];
            String mediaType = parts[6];
            long mediaSize = Long.parseLong(parts[7]);
    
            Block block = new Block(blockData, previousHash, mediaFilePath, mediaType, mediaSize);
            block.timestamp = timestamp; // Set the parsed timestamp
            block.nonce = nonce; // Set the parsed nonce
            block.hash = hash; // Set the parsed hash
            return block; // Return the newly created block
        } catch (Exception e) {
            System.err.println("Error parsing block data: " + e.getMessage());
            return null;
        }
    }
}
