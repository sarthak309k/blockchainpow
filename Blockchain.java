import java.util.LinkedList;

public class Blockchain {
    private LinkedList<Block> chain;
    private BlockchainManager blockchainManager;

    public Blockchain(BlockchainManager blockchainManager) {
        this.blockchainManager = blockchainManager;
        this.chain = new LinkedList<>();
        
        // Load existing blockchain from the manager
        this.chain.addAll(blockchainManager.getBlockchain());

        // If the chain is empty, create and add a genesis block
        if (chain.isEmpty()) {
            Block genesisBlock = new Block("Genesis Block", "0", "", "", 0);
            genesisBlock.mineBlock(blockchainManager.getDifficulty());
            chain.add(genesisBlock);
            blockchainManager.addBlock(genesisBlock); // Save genesis block to the database
        }
    }

    public synchronized boolean addBlock(Block block) {
        Block lastBlock = getLastBlock();
        
        // Validate the new block before adding it to the chain
        if (lastBlock != null && lastBlock.hash.equals(block.previousHash) && block.hash.equals(block.calculateHash())) {
            chain.add(block);
            blockchainManager.addBlock(block); // Save the new block to the database
            return true;
        }
        System.out.println("Invalid block: Previous hash does not match or hash is incorrect.");
        return false;
    }

    // Method to get the last block in the chain
    public Block getLastBlock() {
        return chain.isEmpty() ? null : chain.getLast();
    }

    public String getLastHash() {
        Block lastBlock = getLastBlock();
        return lastBlock != null ? lastBlock.hash : null;
    }

    // Method to validate the entire blockchain
    public boolean isChainValid() {
        return blockchainManager.isBlockchainValid();
    }

    // Method to synchronize chains with other nodes
    public synchronized void synchronizeChain(LinkedList<Block> newChain) {
        if (newChain.size() > chain.size() && blockchainManager.isBlockchainValid()) {
            // Replace the current chain with the new chain
            this.chain = newChain;
            // Save the updated blockchain to the manager
            blockchainManager.saveBlockchainToFile();
            System.out.println("Blockchain synchronized successfully.");
        } else {
            System.out.println("Received chain is not valid or shorter than the current chain.");
        }
    }

    // Method to retrieve the entire blockchain as a list
    public LinkedList<Block> getChain() {
        return chain;
    }
}
