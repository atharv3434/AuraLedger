-- Extract every mined block sequence committed to long-term storage
SELECT block_index, block_hash, nonce FROM blocks;

-- Calculate mining operations output speed using aggregate nonce parameters
SELECT AVG(nonce) as average_proof_of_work_compute_cycles FROM blocks;