import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlockchainNode {
    private static final String DB_URL = "jdbc:sqlite:blockchain_ledger.db";
    private static final int DIFFICULTY = 4; // Target leading zeros for mining proof
    
    // In-memory Mempool (Staging array for pending transaction records)
    private static final List<String> mempool = new ArrayList<>();
    private static String latestBlockHash = "0000000000000000000000000000000000000000000000000000000000000000";

    public static void main(String[] args) throws Exception {
        initDatabase();
        loadLatestHash();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        
        server.createContext("/blockchain/transaction", new TransactionHandler());
        server.createContext("/blockchain/mine", new MineBlockHandler());
        server.createContext("/blockchain/explorer", new ExplorerHandler());
        
        server.setExecutor(null);
        System.out.println("⚡ AuraLedger Sovereign Node actively mining on port 8000...");
        server.start();
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS blocks (
                    block_index INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT,
                    transactions TEXT,
                    previous_hash TEXT,
                    block_hash TEXT,
                    nonce INTEGER
                )
            """);
        } catch (SQLException e) {
            System.err.println("Database deployment fault: " + e.getMessage());
        }
    }

    private static void loadLatestHash() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT block_hash FROM blocks ORDER BY block_index DESC LIMIT 1")) {
            if (rs.next()) {
                latestBlockHash = rs.getString("block_hash");
            } else {
                // Generate Genesis Block if ledger state is empty
                mineAndCommitBlock("GENESIS_POOL", "0000000000000000000000000000000000000000000000000000000000000000");
            }
        } catch (Exception ignored) {}
    }

    // ── PROOF OF WORK & CRYPTOGRAPHIC MINING LIFECYCLE ──────────────────────
    private static synchronized String[] mineAndCommitBlock(String txData, String prevHash) {
        String timestamp = Instant.now().toString();
        int nonce = 0;
        String hash = "";
        String targetPrefix = "0".repeat(DIFFICULTY);

        System.out.println("🔨 Mining Block. Difficulty Target: " + DIFFICULTY + "...");
        
        // Proof of Work consensus loop computation
        while (true) {
            String dataToHash = prevHash + timestamp + txData + nonce;
            hash = calculateSHA256(dataToHash);
            if (hash.startsWith(targetPrefix)) {
                break;
            }
            nonce++;
        }

        System.out.println("🎉 Block Successfully Mined! Nonce solution found: " + nonce);

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO blocks (timestamp, transactions, previous_hash, block_hash, nonce) VALUES (?, ?, ?, ?, ?)")) {
            pstmt.setString(1, timestamp);
            pstmt.setString(2, txData);
            pstmt.setString(3, prevHash);
            pstmt.setString(4, hash);
            pstmt.setInt(5, nonce);
            pstmt.executeUpdate();
            latestBlockHash = hash;
        } catch (SQLException e) {
            System.err.println("Ledger synchronization fault: " + e.getMessage());
        }

        return new String[]{hash, String.valueOf(nonce)};
    }

    public static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ── ENDPOINT CONTROLLERS ────────────────────────────────────────────────
    static class TransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String from = parseJsonField(body, "sender");
                String to = parseJsonField(body, "recipient");
                String val = parseJsonField(body, "amount");

                String txRecord = String.format("%s->%s:%sFT", from, to, val);
                synchronized (mempool) {
                    mempool.add(txRecord);
                }

                sendResponse(exchange, 202, String.format("{\"mempool_status\":\"ACCEPTED\",\"tx\":\"%s\"}", txRecord));
            }
        }
    }

    static class MineBlockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String txData;
                synchronized (mempool) {
                    if (mempool.isEmpty()) {
                        sendResponse(exchange, 400, "{\"error\":\"Mempool empty. Refusing context validation loop.\"}");
                        return;
                    }
                    txData = String.join("|", mempool);
                    mempool.clear();
                }

                String[] mineStats = mineAndCommitBlock(txData, latestBlockHash);
                String resp = String.format("{\"status\":\"MINED\",\"hash\":\"%s\",\"nonce\":%s}", mineStats[0], mineStats[1]);
                sendResponse(exchange, 200, resp);
            }
        }
    }

    static class ExplorerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder sb = new StringBuilder("{\"mempool_depth\":" + mempool.size() + ",\"blocks\":[");
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM blocks ORDER BY block_index DESC LIMIT 10")) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        sb.append(String.format(
                            "{\"index\":%d,\"time\":\"%s\",\"tx\":\"%s\",\"prev\":\"%s\",\"hash\":\"%s\",\"nonce\":%d}",
                            rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6)
                        ));
                        first = false;
                    }
                } catch (SQLException ignored) {}
                sb.append("]}");
                sendResponse(exchange, 200, sb.toString());
            }
        }
    }

    private static void enableCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String parseJsonField(String json, String fieldName) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }
}