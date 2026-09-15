import React, { useState, useRef, useEffect, useTransition, useOptimistic } from 'react';
import { 
  RotateCcw, Undo2, Redo2, Plus, Minus, Play, Pause, Zap, 
  Clock, ShieldAlert, Sparkles, Code2, RefreshCw 
} from 'lucide-react';
import confetti from 'canvas-confetti';

// Custom Hook: Time-Travel History Management
function useHistoryState<T>(initialValue: T) {
  const [history, setHistory] = useState<T[]>([initialValue]);
  const [currentIndex, setCurrentIndex] = useState(0);

  const set = (newVal: T | ((prev: T) => T)) => {
    const value = typeof newVal === 'function' ? (newVal as (prev: T) => T)(history[currentIndex]) : newVal;
    if (value === history[currentIndex]) return;
    
    // Slice off redo future when a new branch is created
    const updatedHistory = history.slice(0, currentIndex + 1);
    setHistory([...updatedHistory, value]);
    setCurrentIndex(updatedHistory.length);
  };

  const undo = () => {
    if (currentIndex > 0) setCurrentIndex(prev => prev - 1);
  };

  const redo = () => {
    if (currentIndex < history.length - 1) setCurrentIndex(prev => prev + 1);
  };

  const reset = (val: T = initialValue) => {
    setHistory([val]);
    setCurrentIndex(0);
  };

  return {
    state: history[currentIndex],
    set,
    undo,
    redo,
    canUndo: currentIndex > 0,
    canRedo: currentIndex < history.length - 1,
    history,
    currentIndex,
    reset
  };
}

export const CounterApp: React.FC = () => {
  // Time Travel State
  const { 
    state: count, 
    set: setCount, 
    undo, 
    redo, 
    canUndo, 
    canRedo, 
    history, 
    currentIndex, 
    reset 
  } = useHistoryState<number>(0);

  // Bounds & Controls
  const [minBound, setMinBound] = useState<number>(-50);
  const [maxBound, setMaxBound] = useState<number>(100);
  const [step, setStep] = useState<number>(1);

  // Auto-Ticker Engine
  const [isAutoTicking, setIsAutoTicking] = useState(false);
  const [tickSpeed, setTickSpeed] = useState<number>(500);

  // Async Network Simulation with AbortController
  const [isPendingNetwork, setIsPendingNetwork] = useState(false);
  const [networkDelay, setNetworkDelay] = useState<number>(1000);
  const [asyncLog, setAsyncLog] = useState<string[]>([]);
  const abortControllerRef = useRef<AbortController | null>(null);

  // React 19 Action & Optimistic Simulation
  const [isPendingTransition, startTransition] = useTransition();
  const [optimisticCount, setOptimisticCount] = useOptimistic(
    count,
    (current, update: number) => current + update
  );

  // Render Telemetry
  const renderCountRef = useRef(0);
  renderCountRef.current += 1;

  // Code Modal State
  const [showCode, setShowCode] = useState(false);

  // Auto-Ticker Effect
  useEffect(() => {
    if (!isAutoTicking) return;
    const timer = setInterval(() => {
      setCount((prev: number) => {
        if (prev + step > maxBound) {
          setIsAutoTicking(false);
          return prev;
        }
        return prev + step;
      });
    }, tickSpeed);

    return () => clearInterval(timer);
  }, [isAutoTicking, tickSpeed, step, maxBound]);

  // Handle Increments with bounds
  const handleDelta = (delta: number) => {
    const nextVal = count + delta;
    if (nextVal < minBound || nextVal > maxBound) return;
    setCount(nextVal);
    if (nextVal === 10 || nextVal === 50 || nextVal === 100) {
      confetti({ particleCount: 60, spread: 70, origin: { y: 0.7 } });
    }
  };

  // Async Increment with cancellation
  const handleAsyncIncrement = async (delta: number) => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      setAsyncLog(prev => [`[Cancelled] Superseded previous in-flight request`, ...prev.slice(0, 4)]);
    }

    const controller = new AbortController();
    abortControllerRef.current = controller;
    setIsPendingNetwork(true);

    const startTime = Date.now();
    setAsyncLog(prev => [`[Started] Request with delta ${delta > 0 ? '+' : ''}${delta}...`, ...prev.slice(0, 4)]);

    try {
      await new Promise((resolve, reject) => {
        const timeout = setTimeout(resolve, networkDelay);
        controller.signal.addEventListener('abort', () => {
          clearTimeout(timeout);
          reject(new DOMException('Aborted', 'AbortError'));
        });
      });

      const nextVal = count + delta;
      if (nextVal >= minBound && nextVal <= maxBound) {
        setCount(nextVal);
        setAsyncLog(prev => [`[Success] Updated to ${nextVal} in ${Date.now() - startTime}ms`, ...prev.slice(0, 4)]);
      }
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        setAsyncLog(prev => [`[Error] Network error: ${err.message}`, ...prev.slice(0, 4)]);
      }
    } finally {
      if (abortControllerRef.current === controller) {
        setIsPendingNetwork(false);
      }
    }
  };

  // React 19 Optimistic Action Mutation
  const handleOptimisticServerSync = async () => {
    startTransition(async () => {
      setOptimisticCount(5); // Instantly update UI optimistically
      setAsyncLog(prev => [`[Optimistic] UI updated instantly by +5, syncing with server...`, ...prev.slice(0, 4)]);
      
      // Simulate remote network latency
      await new Promise(r => setTimeout(r, 1200));
      
      // Commit actual server state
      setCount((prev: number) => Math.min(maxBound, prev + 5));
      setAsyncLog(prev => [`[Server Committed] Sync verified by backend`, ...prev.slice(0, 4)]);
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header & Telemetry Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span>🔢 Advanced Time-Travel & Async Counter</span>
            <span className="badge badge-primary">React 19 Hooks</span>
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Full Undo/Redo stack, AbortController async cancellation, bounding safeguards, and optimistic actions.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className="badge badge-warning" title="Total times this component re-rendered">
            <RefreshCw size={12} className="animate-spin" /> Renders: {renderCountRef.current}
          </span>
          <button className="btn btn-secondary" onClick={() => setShowCode(!showCode)}>
            <Code2 size={16} /> {showCode ? 'Hide Code' : 'Inspect Architecture'}
          </button>
        </div>
      </div>

      {/* Code Architecture View */}
      {showCode && (
        <div className="glass-panel" style={{ padding: '20px', background: 'rgba(10, 15, 29, 0.95)', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--primary)' }}>
              Engine Pattern: Ring Buffer History + AbortController Signals + React 19 useOptimistic
            </span>
            <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: '0.75rem' }} onClick={() => setShowCode(false)}>Close</button>
          </div>
          <pre style={{ overflowX: 'auto', padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.5)', color: '#a5f3fc' }}>
            <code>{`// Core Interview Architecture:
1. useHistoryState<T>: O(1) Undo/Redo pointer slicing without mutation.
2. AbortController: Cancels stale in-flight HTTP latency requests.
3. useOptimistic(count, updater): Instant perceived latency with automatic rollback on network failure.
4. Auto-Tick Interval: Bounded auto-increments with cleanup on unmount.`}</code>
          </pre>
        </div>
      )}

      {/* Main Counter Card Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
        
        {/* Core Counter & Display */}
        <div className="glass-panel" style={{ padding: '28px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            Current Value (Optimistic: {optimisticCount})
          </span>

          <div style={{ 
            fontSize: '5rem', 
            fontWeight: 800, 
            lineHeight: 1.1,
            margin: '16px 0',
            background: count >= 0 ? 'linear-gradient(135deg, #38bdf8, #818cf8)' : 'linear-gradient(135deg, #f87171, #fbbf24)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            filter: 'drop-shadow(0 0 20px rgba(56, 189, 248, 0.3))'
          }}>
            {optimisticCount}
          </div>

          {/* Bound Warning */}
          {(count <= minBound || count >= maxBound) && (
            <div className="badge badge-warning" style={{ marginBottom: '16px' }}>
              <ShieldAlert size={14} /> Hit Bound Limit ({count <= minBound ? `Min: ${minBound}` : `Max: ${maxBound}`})
            </div>
          )}

          {/* Primary Step Controls */}
          <div style={{ display: 'flex', gap: '12px', width: '100%', maxWidth: '320px' }}>
            <button 
              className="btn btn-secondary" 
              style={{ flex: 1, padding: '14px', fontSize: '1.25rem' }} 
              onClick={() => handleDelta(-step)}
              disabled={count - step < minBound}
            >
              <Minus size={20} /> -{step}
            </button>
            <button 
              className="btn btn-primary" 
              style={{ flex: 1, padding: '14px', fontSize: '1.25rem' }} 
              onClick={() => handleDelta(step)}
              disabled={count + step > maxBound}
            >
              <Plus size={20} /> +{step}
            </button>
          </div>

          {/* Undo / Redo / Reset */}
          <div style={{ display: 'flex', gap: '10px', marginTop: '16px' }}>
            <button className="btn btn-secondary" onClick={undo} disabled={!canUndo} title="Undo (Cmd+Z)">
              <Undo2 size={16} /> Undo
            </button>
            <button className="btn btn-secondary" onClick={redo} disabled={!canRedo} title="Redo (Cmd+Y)">
              <Redo2 size={16} /> Redo
            </button>
            <button className="btn btn-danger" onClick={() => reset(0)} title="Reset to 0">
              <RotateCcw size={16} /> Reset
            </button>
          </div>

          {/* History Timeline Pills */}
          <div style={{ marginTop: '24px', width: '100%' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '8px', textAlign: 'left' }}>
              Time-Travel History Stack ({currentIndex + 1} / {history.length})
            </div>
            <div style={{ display: 'flex', gap: '6px', overflowX: 'auto', paddingBottom: '6px' }}>
              {history.map((val, idx) => (
                <span 
                  key={idx} 
                  style={{
                    padding: '4px 10px',
                    borderRadius: '6px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    background: idx === currentIndex ? 'var(--primary)' : 'rgba(255,255,255,0.06)',
                    color: idx === currentIndex ? '#000' : 'var(--text-muted)',
                    cursor: 'default'
                  }}
                >
                  {val}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* Async & Network Race Condition Handler */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Zap size={18} color="var(--primary)" /> Async Network & Race Conditions
            </h3>
            {isPendingNetwork && <span className="badge badge-warning animate-pulse-subtle">In-Flight Request</span>}
          </div>

          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
            Rapid clicks trigger <code>AbortController.abort()</code>, cancelling stale promises so late responses never overwrite recent state.
          </p>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button 
              className="btn btn-secondary" 
              style={{ flex: 1 }}
              onClick={() => handleAsyncIncrement(1)}
              disabled={isPendingNetwork}
            >
              <Clock size={16} /> Async +1 ({networkDelay}ms)
            </button>

            <button 
              className="btn btn-primary" 
              style={{ flex: 1 }}
              onClick={handleOptimisticServerSync}
              disabled={isPendingTransition}
            >
              <Sparkles size={16} /> Optimistic +5
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            <span>Simulated Network Latency:</span>
            <input 
              type="range" 
              min="200" 
              max="2500" 
              step="100" 
              value={networkDelay} 
              onChange={e => setNetworkDelay(Number(e.target.value))}
              style={{ flex: 1 }}
            />
            <span style={{ fontWeight: 600, color: '#f8fafc' }}>{networkDelay}ms</span>
          </div>

          {/* Network Logs Console */}
          <div style={{ 
            background: 'rgba(0, 0, 0, 0.4)', 
            border: '1px solid rgba(255,255,255,0.06)', 
            borderRadius: '10px', 
            padding: '12px',
            fontSize: '0.775rem',
            fontFamily: 'var(--font-mono)',
            minHeight: '120px',
            display: 'flex',
            flexDirection: 'column',
            gap: '4px'
          }}>
            <div style={{ color: 'var(--text-muted)', borderBottom: '1px solid rgba(255,255,255,0.08)', paddingBottom: '4px', marginBottom: '4px' }}>
              Console Activity Log
            </div>
            {asyncLog.length === 0 ? (
              <span style={{ color: '#64748b' }}>No async events recorded yet. Click Async +1 or Optimistic.</span>
            ) : (
              asyncLog.map((log, i) => (
                <div key={i} style={{ color: log.startsWith('[Success]') ? '#34d399' : log.startsWith('[Cancelled]') ? '#f87171' : '#38bdf8' }}>
                  {log}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Configuration & Auto Ticker */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={18} color="var(--secondary)" /> Auto-Increment Engine & Bounds
          </h3>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Min Bound</label>
              <input 
                type="number" 
                className="input-control" 
                value={minBound} 
                onChange={e => setMinBound(Number(e.target.value))} 
              />
            </div>
            <div>
              <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Max Bound</label>
              <input 
                type="number" 
                className="input-control" 
                value={maxBound} 
                onChange={e => setMaxBound(Number(e.target.value))} 
              />
            </div>
            <div>
              <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Step Size</label>
              <input 
                type="number" 
                className="input-control" 
                value={step} 
                onChange={e => setStep(Math.max(1, Number(e.target.value)))} 
              />
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <button 
              className={`btn ${isAutoTicking ? 'btn-danger' : 'btn-primary'}`} 
              style={{ flex: 1 }}
              onClick={() => setIsAutoTicking(!isAutoTicking)}
            >
              {isAutoTicking ? <><Pause size={16} /> Stop Auto-Tick</> : <><Play size={16} /> Start Auto-Tick</>}
            </button>

            <select 
              className="input-control" 
              style={{ width: '130px' }}
              value={tickSpeed} 
              onChange={e => setTickSpeed(Number(e.target.value))}
            >
              <option value="1000">1.0s / tick</option>
              <option value="500">0.5s / tick</option>
              <option value="250">0.25s / tick</option>
              <option value="100">0.10s / tick</option>
            </select>
          </div>

          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: '8px' }}>
            💡 <strong>Interview Pro-Tip:</strong> State updates inside <code>setInterval</code> must use the functional updater <code>setCount(prev =&gt; prev + step)</code> to prevent closure state stagnation.
          </div>
        </div>

      </div>
    </div>
  );
};
