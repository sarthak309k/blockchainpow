import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BlockchainManager {
    private List<Block> blockchain = new ArrayList<>();
    private static final int DIFFICULTY = 4;
    private Path filePath;

    public BlockchainManager(String filePath) {
        this.filePath = Paths.get(filePath);
        // Load blockchain from file
        loadBlockchainFromFile();

        // If blockchain is empty, create and add a genesis block
        if (blockchain.isEmpty()) {
            System.out.println("No blockchain file found or blockchain is empty. Starting new blockchain with genesis block.");
            Block genesisBlock = new Block("Genesis Block", "0", "", "", 0);
            genesisBlock.mineBlock(DIFFICULTY);
            blockchain.add(genesisBlock);
            saveBlockchainToFile();  // Save genesis block to file
        }
    }

    // Load blockchain from a file
    private void loadBlockchainFromFile() {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Block block = Block.fromString(line);
                if (block != null) { // Ensure block is valid before adding
                    blockchain.add(block);
                }
            }
            System.out.println("Blockchain loaded from file with " + blockchain.size() + " blocks.");
        } catch (IOException e) {
            System.out.println("No blockchain file found or could not read file. Starting new blockchain. Error: " + e.getMessage());
        }
    }

    // Save the blockchain to a file
    public void saveBlockchainToFile() {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) { // Start fresh on the first run
            for (Block block : blockchain) {
                writer.write(block.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save blockchain to file: " + e.getMessage());
        }
    }

    // Add block to blockchain and save to file
    public synchronized boolean addBlock(Block newBlock) {
        Block lastBlock = getLastBlock();
        if (lastBlock == null || (newBlock.previousHash.equals(lastBlock.hash) && newBlock.hash.equals(newBlock.calculateHash()))) {
            blockchain.add(newBlock);
            appendBlockToFile(newBlock); // Append new block to the file
            System.out.println("Block added to chain: " + newBlock.hash);
            return true;
        }
        System.out.println("Invalid block: Previous hash does not match.");
        return false;
    }

    // Append new block to the blockchain file
    private void appendBlockToFile(Block newBlock) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true))) { // Append to the file
            writer.write(newBlock.toString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to append block to file: " + e.getMessage());
        }
    }

    // Validate the blockchain
    public boolean isBlockchainValid() {
        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            Block previousBlock = blockchain.get(i - 1);

            // Check if the hash of the current block is correct
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("Invalid hash at block " + i);
                return false;
            }

            // Check if the previous hash is correct
            if (!currentBlock.previousHash.equals(previousBlock.hash)) {
                System.out.println("Invalid previous hash at block " + i);
                return false;
            }
        }
        return true;
    }

    public Block getLastBlock() {
        return blockchain.isEmpty() ? null : blockchain.get(blockchain.size() - 1);
    }

    public List<Block> getBlockchain() {
        return blockchain;
    }

    public int getDifficulty() {
        return DIFFICULTY;
    }

    // Add a new media block to the blockchain
    public synchronized boolean addMediaBlock(String data, String mediaFilePath, String mediaType, long mediaSize) {
        Block lastBlock = getLastBlock();
        Block newBlock = new Block(data, lastBlock != null ? lastBlock.hash : "0", mediaFilePath, mediaType, mediaSize);
        newBlock.mineBlock(DIFFICULTY); // Mine the new block
        return addBlock(newBlock); // Add the newly created media block to the blockchain
    }
}
