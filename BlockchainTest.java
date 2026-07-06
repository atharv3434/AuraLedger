public class BlockchainTest {
    public static void main(String[] args) {
        testBlockHashValidation();
        testChainImmutabilityFaultDetect();
    }

    public static void testBlockHashValidation() {
        System.out.println("Running Chain Consensus Security Assertions...");
        String blockData = "00000000-GenesisBlock-NonceSol-4251";
        
        String hash1 = BlockchainNode.calculateSHA256(blockData);
        String hash2 = BlockchainNode.calculateSHA256(blockData);
        
        if (hash1.equals(hash2)) {
            System.out.println("✅ Test Passed: Block hashing is mathematically deterministic.");
        } else {
            throw new AssertionError("Fails block hash consistency verification checks.");
        }
    }

    public static void testChainImmutabilityFaultDetect() {
        String baseBlock = "Alice->Bob:5FT|Nonce:5042";
        String alteredBlock = "Alice->Bob:50FT|Nonce:5042"; // Unauthorized transaction injection modification

        String originalHash = BlockchainNode.calculateSHA256(baseBlock);
        String tamperedHash = BlockchainNode.calculateSHA256(alteredBlock);

        if (!originalHash.equals(tamperedHash)) {
            System.out.println("✅ Test Passed: Tamper detection active. Any modifications alter parent tree hashes completely.");
        } else {
            throw new AssertionError("Fails cryptographic chain link immutability tests.");
        }
    }
}