import React, { useState, useEffect } from 'react';
import { Share2, Cpu, Database, PlusCircle, Hammer, Layers, ShieldCheck } from 'lucide-react';

const API_ROOT = 'http://localhost:8000';

export default function App() {
  const [explorerData, setExplorerData] = useState({ mempool_depth: 0, blocks: [] });
  const [sender, setSender] = useState('0xAlice_Wallet');
  const [recipient, setRecipient] = useState('0xBob_Wallet');
  const [amount, setAmount] = useState('10.5');
  const [terminalFeed, setTerminalFeed] = useState(["AuraLedger Operations Suite: ONLINE. P2P consensus structures synchronized."]);

  const fetchLedgerState = async () => {
    try {
      const res = await fetch(`${API_ROOT}/blockchain/explorer`);
      const data = await res.json();
      setExplorerData(data);
    } catch (err) {
      console.error("Ledger Node Network drop:", err);
    }
  };

  useEffect(() => {
    fetchLedgerState();
    const interval = setInterval(fetchLedgerState, 2000);
    return () => clearInterval(interval);
  }, []);

  const injectTransaction = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_ROOT}/blockchain/transaction`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sender, recipient, amount })
      });
      const data = await res.json();
      setTerminalFeed(prev => [`[MEMPOOL INGEST] Broadcast Tx successfully staged: ${data.tx}`, ...prev]);
      fetchLedgerState();
    } catch (err) {
      setTerminalFeed(prev => ["[ERROR] Failed to broadcast transaction payload to network nodes.", ...prev]);
    }
  };

  const triggerMiningRoutine = async () => {
    setTerminalFeed(prev => ["[MINER INIT] Commencing validation loops against difficulty constraints...", ...prev]);
    try {
      const res = await fetch(`${API_ROOT}/blockchain/mine`, { method: 'POST' });
      if (res.status === 400) {
        setTerminalFeed(prev => ["[MINER ABORT] Countermeasures blocked mining. Mempool block payload contains zero transactions.", ...prev]);
        return;
      }
      const data = await res.json();
      setTerminalFeed(prev => [`[🎉 SUCCESS] Target Proof verified! Block signature: ${data.hash}. Nonce: ${data.nonce}`, ...prev]);
      fetchLedgerState();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-mono p-6 selection:bg-slate-800">
      {/* HUD Header Area */}
      <div className="max-w-6xl mx-auto border-b border-slate-800 pb-4 flex justify-between items-center">
        <div className="flex items-center gap-3">
          <Share2 className="text-emerald-500 w-8 h-8 animate-pulse" />
          <div>
            <h1 className="text-xl font-black tracking-wider text-slate-100">AURA_LEDGER.NODE1</h1>
            <p className="text-xs text-slate-500">Sovereign Proof-of-Work Node & Persistent Relational Ledger</p>
          </div>
        </div>
        <div className="text-xs border border-slate-800 rounded-lg px-3 py-1.5 bg-slate-900/40 flex items-center gap-2">
          <Layers className="text-emerald-400 w-4 h-4" /> Active Mempool Load: <span className="text-white font-bold">{explorerData.mempool_depth} pending tx</span>
        </div>
      </div>

      <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        
        {/* Transaction Injection & Mining Work Control Panel */}
        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-5 space-y-4">
          <h2 className="text-sm font-bold tracking-widest text-slate-400 uppercase flex items-center gap-2">
            <PlusCircle className="w-4 h-4 text-emerald-500" /> Transaction Broadcast Engine
          </h2>
          <form onSubmit={injectTransaction} className="space-y-3 text-xs">
            <div>
              <label className="block text-slate-500 font-bold mb-1">SOURCE ORIGIN WALLET PUBLIC KEY</label>
              <input type="text" value={sender} onChange={(e) => setSender(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded p-2 focus:outline-none focus:border-emerald-500 text-slate-300" />
            </div>
            <div>
              <label className="block text-slate-500 font-bold mb-1">DESTINATION WALLET TARGET KEY</label>
              <input type="text" value={recipient} onChange={(e) => setRecipient(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded p-2 focus:outline-none focus:border-emerald-500 text-slate-300" />
            </div>
            <div>
              <label className="block text-slate-500 font-bold mb-1">ASSET TRANSFER VOLUME (FT)</label>
              <input type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded p-2 focus:outline-none focus:border-emerald-500 text-slate-300" />
            </div>
            <button type="submit" className="w-full bg-emerald-600 hover:bg-emerald-500 text-white py-2 rounded font-black transition tracking-wide text-xs">
              SIGN & BROADCAST TRANSACTION
            </button>
          </form>

          {/* Mining Infrastructure Control Block */}
          <div className="pt-4 border-t border-slate-800 space-y-2">
            <h3 className="text-xs font-bold tracking-widest text-slate-400 uppercase flex items-center gap-2">
              <Cpu className="w-4 h-4 text-cyan-500" /> Hardware Consensus Suite
            </h3>
            <button onClick={triggerMiningRoutine} className="w-full bg-slate-100 hover:bg-white text-black py-2.5 rounded font-black transition text-xs flex items-center justify-center gap-2">
              <Hammer className="w-4 h-4 animate-bounce" /> PROCESS MEMPOOL & MINE BLOCK
            </button>
          </div>
        </div>

        {/* Distributed Ledger Block Explorer Fact Sheet */}
        <div className="lg:col-span-2 bg-slate-900/20 border border-slate-800 rounded-xl p-5 space-y-3">
          <h2 className="text-sm font-bold tracking-widest text-slate-400 uppercase flex items-center gap-2">
            <Database className="w-4 h-4 text-emerald-500" /> Relational Block Ledger Explorer Index
          </h2>
          <div className="space-y-3 max-h-[360px] overflow-y-auto pr-1">
            {explorerData.blocks.map((block) => (
              <div key={block.index} className="p-3 border border-slate-800 bg-slate-950/60 rounded-lg text-[11px] space-y-2 hover:border-slate-700 transition">
                <div className="flex justify-between items-center border-b border-slate-900 pb-1.5 text-xs">
                  <span className="font-black text-emerald-400">BLOCK INDEX #{block.index}</span>
                  <span className="text-slate-500 font-medium">{block.time.slice(11, 19)} UTC</span>
                </div>
                <div className="grid grid-cols-1 gap-1 text-slate-400">
                  <p className="truncate"><span className="text-slate-600 font-bold uppercase">Hash Digest:</span> {block.hash}</p>
                  <p className="truncate"><span className="text-slate-600 font-bold uppercase">Parent Root:</span> {block.prev}</p>
                  <div className="p-1.5 mt-1 rounded bg-slate-900/40 text-[10px] text-slate-300 font-mono italic">
                    📦 Transactions: "{block.tx.replace(/\|/g, ', ')}"
                  </div>
                </div>
                <div className="flex justify-between items-center text-[10px] pt-1 text-slate-500 border-t border-slate-900/60">
                  <span className="flex items-center gap-1"><ShieldCheck className="w-3 h-3 text-emerald-500"/> Consensus State Verified</span>
                  <span>Nonce Met: <strong className="text-cyan-400 font-mono">{block.nonce}</strong></span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Terminal Engine Feedback Output Stream Console */}
      <div className="max-w-6xl mx-auto mt-6 bg-black border border-slate-800 rounded-xl p-4 h-32 overflow-y-auto text-xs space-y-1 shadow-inner">
        <span className="text-[10px] text-slate-600 uppercase tracking-widest block font-bold border-b border-slate-900 pb-1 mb-1">Live Distributed Cryptographic Ledger Console Logs</span>
        {terminalFeed.map((log, i) => (
          <p key={i} className={i === 0 ? "text-emerald-400 font-bold" : "text-slate-500"}>
            &gt; {log}
          </p>
        ))}
      </div>
    </div>
  );
}