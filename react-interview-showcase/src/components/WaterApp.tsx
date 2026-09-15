import React, { useState, useEffect, useMemo } from 'react';
import { 
  Play, Pause, SkipForward, RotateCcw, Shuffle, Droplets, 
  TrendingUp, Code2, CheckCircle2 
} from 'lucide-react';
import confetti from 'canvas-confetti';

type WaterProblemType = 'container' | 'trapping';

interface StepState {
  left: number;
  right: number;
  currentArea?: number;
  maxArea?: number;
  bestLeft?: number;
  bestRight?: number;
  trappedSoFar?: number;
  trappedWaterGrid?: number[]; // for trapping rainwater
  explanation: string;
}

const PRESET_CONTAINER = [1, 8, 6, 2, 5, 4, 8, 3, 7];
const PRESET_TRAPPING = [0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1];

export const WaterApp: React.FC = () => {
  const [problemType, setProblemType] = useState<WaterProblemType>('container');
  const [heights, setHeights] = useState<number[]>(PRESET_CONTAINER);
  const [customInput, setCustomInput] = useState<string>(PRESET_CONTAINER.join(', '));
  const [stepIndex, setStepIndex] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(800);
  const [showCode, setShowCode] = useState<boolean>(false);

  // Pre-compute execution step trace for Container With Most Water (LeetCode 11)
  const containerSteps = useMemo<StepState[]>(() => {
    const steps: StepState[] = [];
    let l = 0;
    let r = heights.length - 1;
    let maxA = 0;
    let bestL = 0;
    let bestR = heights.length - 1;

    while (l < r) {
      const hL = heights[l];
      const hR = heights[r];
      const width = r - l;
      const effectiveH = Math.min(hL, hR);
      const area = effectiveH * width;

      let isNewMax = false;
      if (area > maxA) {
        maxA = area;
        bestL = l;
        bestR = r;
        isNewMax = true;
      }

      steps.push({
        left: l,
        right: r,
        currentArea: area,
        maxArea: maxA,
        bestLeft: bestL,
        bestRight: bestR,
        explanation: `L=${l} (h=${hL}), R=${r} (h=${hR}) | Width=${width}, Height=min(${hL},${hR})=${effectiveH} | Area=${area}${isNewMax ? ' 🚀 NEW MAX FOUND!' : ''}. Moving ${hL < hR ? 'Left pointer inward (since hL < hR)' : 'Right pointer inward (since hR <= hL)'}.`
      });

      if (hL < hR) {
        l++;
      } else {
        r--;
      }
    }
    return steps;
  }, [heights]);

  // Pre-compute execution step trace for Trapping Rain Water (LeetCode 42)
  const trappingSteps = useMemo<StepState[]>(() => {
    const steps: StepState[] = [];
    const n = heights.length;
    if (n === 0) return steps;

    let l = 0;
    let r = n - 1;
    let leftMax = 0;
    let rightMax = 0;
    let totalTrapped = 0;
    const waterGrid = new Array(n).fill(0);

    while (l <= r) {
      if (heights[l] <= heights[r]) {
        if (heights[l] >= leftMax) {
          leftMax = heights[l];
          steps.push({
            left: l,
            right: r,
            trappedSoFar: totalTrapped,
            trappedWaterGrid: [...waterGrid],
            explanation: `L=${l}, R=${r} | heights[L] (${heights[l]}) >= leftMax (${leftMax}). Updating leftMax=${heights[l]}. No water trapped at index ${l}.`
          });
        } else {
          const trappedAtIdx = leftMax - heights[l];
          totalTrapped += trappedAtIdx;
          waterGrid[l] = trappedAtIdx;
          steps.push({
            left: l,
            right: r,
            trappedSoFar: totalTrapped,
            trappedWaterGrid: [...waterGrid],
            explanation: `L=${l}, R=${r} | leftMax (${leftMax}) > heights[L] (${heights[l]}). Trapping ${trappedAtIdx} unit(s) of water at index ${l}. Total: ${totalTrapped}.`
          });
        }
        l++;
      } else {
        if (heights[r] >= rightMax) {
          rightMax = heights[r];
          steps.push({
            left: l,
            right: r,
            trappedSoFar: totalTrapped,
            trappedWaterGrid: [...waterGrid],
            explanation: `L=${l}, R=${r} | heights[R] (${heights[r]}) >= rightMax (${rightMax}). Updating rightMax=${heights[r]}. No water trapped at index ${r}.`
          });
        } else {
          const trappedAtIdx = rightMax - heights[r];
          totalTrapped += trappedAtIdx;
          waterGrid[r] = trappedAtIdx;
          steps.push({
            left: l,
            right: r,
            trappedSoFar: totalTrapped,
            trappedWaterGrid: [...waterGrid],
            explanation: `L=${l}, R=${r} | rightMax (${rightMax}) > heights[R] (${heights[r]}). Trapping ${trappedAtIdx} unit(s) of water at index ${r}. Total: ${totalTrapped}.`
          });
        }
        r--;
      }
    }
    return steps;
  }, [heights]);

  const activeSteps = problemType === 'container' ? containerSteps : trappingSteps;
  const currentStep = activeSteps[Math.min(stepIndex, activeSteps.length - 1)] || {
    left: 0,
    right: heights.length - 1,
    explanation: 'Initial state ready.'
  };

  const maxHeightVal = Math.max(...heights, 1);

  // Playback Timer
  useEffect(() => {
    if (!isPlaying) return;
    const interval = setInterval(() => {
      setStepIndex(prev => {
        if (prev >= activeSteps.length - 1) {
          setIsPlaying(false);
          confetti({ particleCount: 80, spread: 80, origin: { y: 0.6 } });
          return prev;
        }
        return prev + 1;
      });
    }, playbackSpeed);

    return () => clearInterval(interval);
  }, [isPlaying, playbackSpeed, activeSteps.length]);

  const handleApplyInput = () => {
    try {
      const parsed = customInput
        .split(',')
        .map(s => parseInt(s.trim(), 10))
        .filter(n => !isNaN(n) && n >= 0);
      if (parsed.length < 2) {
        alert('Please provide at least 2 non-negative numbers.');
        return;
      }
      setHeights(parsed);
      setStepIndex(0);
      setIsPlaying(false);
    } catch {
      alert('Invalid input. Format: 1, 8, 6, 2, 5, 4, 8, 3, 7');
    }
  };

  const handleRandomize = () => {
    const size = Math.floor(Math.random() * 6) + 7; // 7-12 items
    const generated = Array.from({ length: size }, () => Math.floor(Math.random() * 10));
    setHeights(generated);
    setCustomInput(generated.join(', '));
    setStepIndex(0);
    setIsPlaying(false);
  };

  const switchProblem = (type: WaterProblemType) => {
    setProblemType(type);
    const defaultData = type === 'container' ? PRESET_CONTAINER : PRESET_TRAPPING;
    setHeights(defaultData);
    setCustomInput(defaultData.join(', '));
    setStepIndex(0);
    setIsPlaying(false);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header & Modes */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span>🌊 Min-Max & Trapping Water Algorithm Visualizer</span>
            <span className="badge badge-primary">Two-Pointer O(N)</span>
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Interactive step-by-step visualizer for Container With Most Water (LC 11) & Trapping Rain Water (LC 42).
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button 
            className={`btn ${problemType === 'container' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => switchProblem('container')}
          >
            <TrendingUp size={16} /> Container With Most Water
          </button>
          <button 
            className={`btn ${problemType === 'trapping' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => switchProblem('trapping')}
          >
            <Droplets size={16} /> Trapping Rain Water
          </button>
          <button className="btn btn-secondary" onClick={() => setShowCode(!showCode)}>
            <Code2 size={16} /> {showCode ? 'Hide Code' : 'Inspect Algorithm'}
          </button>
        </div>
      </div>

      {/* Code Architecture View */}
      {showCode && (
        <div className="glass-panel" style={{ padding: '20px', background: 'rgba(10, 15, 29, 0.95)', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--primary)' }}>
              Algorithm: {problemType === 'container' ? 'Two-Pointer Area Maximization (LeetCode 11)' : 'Two-Pointer Elevation Trapping (LeetCode 42)'}
            </span>
            <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: '0.75rem' }} onClick={() => setShowCode(false)}>Close</button>
          </div>
          <pre style={{ overflowX: 'auto', padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.5)', color: '#a5f3fc' }}>
            <code>{problemType === 'container' ? `// Time: O(N), Space: O(1)
function maxArea(height: number[]): number {
  let left = 0, right = height.length - 1, maxA = 0;
  while (left < right) {
    const width = right - left;
    const currentA = Math.min(height[left], height[right]) * width;
    maxA = Math.max(maxA, currentA);
    // Crucial greedy rule: Always advance pointer with smaller height
    if (height[left] < height[right]) left++;
    else right--;
  }
  return maxA;
}` : `// Time: O(N), Space: O(1)
function trap(height: number[]): number {
  let l = 0, r = height.length - 1;
  let leftMax = 0, rightMax = 0, trapped = 0;
  while (l <= r) {
    if (height[l] <= height[r]) {
      if (height[l] >= leftMax) leftMax = height[l];
      else trapped += leftMax - height[l];
      l++;
    } else {
      if (height[r] >= rightMax) rightMax = height[r];
      else trapped += rightMax - height[r];
      r--;
    }
  }
  return trapped;
}`}</code>
          </pre>
        </div>
      )}

      {/* Main Visualizer Stage */}
      <div className="glass-panel" style={{ padding: '28px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Metric Bar */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
          <div style={{ background: 'rgba(255,255,255,0.03)', padding: '14px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Active Pointers</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--primary)' }}>
              L = {currentStep.left} | R = {currentStep.right}
            </div>
          </div>

          {problemType === 'container' ? (
            <>
              <div style={{ background: 'rgba(255,255,255,0.03)', padding: '14px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.06)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Current Area</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f8fafc' }}>
                  {currentStep.currentArea ?? 0} units²
                </div>
              </div>
              <div style={{ background: 'rgba(2, 132, 199, 0.15)', padding: '14px', borderRadius: '10px', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
                <div style={{ fontSize: '0.75rem', color: '#38bdf8' }}>Max Area Found</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#38bdf8' }}>
                  🏆 {currentStep.maxArea ?? 0} units²
                </div>
              </div>
            </>
          ) : (
            <div style={{ background: 'rgba(2, 132, 199, 0.15)', padding: '14px', borderRadius: '10px', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
              <div style={{ fontSize: '0.75rem', color: '#38bdf8' }}>Total Trapped Water</div>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#38bdf8' }}>
                💧 {currentStep.trappedSoFar ?? 0} units
              </div>
            </div>
          )}

          <div style={{ background: 'rgba(255,255,255,0.03)', padding: '14px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Execution Progress</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--secondary)' }}>
              Step {stepIndex + 1} / {activeSteps.length}
            </div>
          </div>
        </div>

        {/* Dynamic Water Bars Graph */}
        <div style={{ 
          minHeight: '260px', 
          background: 'rgba(0, 0, 0, 0.35)', 
          border: '1px solid rgba(255,255,255,0.08)', 
          borderRadius: '12px', 
          padding: '24px 20px 10px 20px',
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'center',
          gap: '12px',
          position: 'relative'
        }}>
          {heights.map((h, idx) => {
            const isL = idx === currentStep.left;
            const isR = idx === currentStep.right;
            const isBestL = problemType === 'container' && idx === currentStep.bestLeft;
            const isBestR = problemType === 'container' && idx === currentStep.bestRight;
            const trappedUnits = currentStep.trappedWaterGrid ? currentStep.trappedWaterGrid[idx] || 0 : 0;
            const heightPercent = (h / maxHeightVal) * 160;
            const waterPercent = (trappedUnits / maxHeightVal) * 160;

            return (
              <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1, maxWidth: '60px', position: 'relative' }}>
                
                {/* Pointer Tag Labels */}
                <div style={{ position: 'absolute', top: '-28px', display: 'flex', gap: '2px', fontWeight: 800, fontSize: '0.75rem' }}>
                  {isL && <span style={{ background: '#38bdf8', color: '#000', padding: '2px 6px', borderRadius: '4px' }}>L</span>}
                  {isR && <span style={{ background: '#ec4899', color: '#fff', padding: '2px 6px', borderRadius: '4px' }}>R</span>}
                </div>

                {/* Trapped Water Layer (for Trapping Rain Water) */}
                {problemType === 'trapping' && trappedUnits > 0 && (
                  <div style={{
                    width: '100%',
                    height: `${waterPercent}px`,
                    background: 'rgba(56, 189, 248, 0.65)',
                    border: '1px dashed #38bdf8',
                    borderRadius: '4px 4px 0 0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.65rem',
                    color: '#fff',
                    fontWeight: 700
                  }}>
                    +{trappedUnits}
                  </div>
                )}

                {/* Solid Elevation Pillar Bar */}
                <div style={{
                  width: '100%',
                  height: `${Math.max(12, heightPercent)}px`,
                  background: isL 
                    ? 'linear-gradient(180deg, #38bdf8, #0284c7)' 
                    : isR 
                    ? 'linear-gradient(180deg, #ec4899, #be185d)' 
                    : isBestL || isBestR
                    ? 'linear-gradient(180deg, #10b981, #059669)'
                    : 'linear-gradient(180deg, #475569, #334155)',
                  borderRadius: problemType === 'trapping' && trappedUnits > 0 ? '0' : '6px 6px 0 0',
                  boxShadow: (isL || isR) ? '0 0 16px rgba(56, 189, 248, 0.4)' : 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#fff',
                  fontWeight: 700,
                  fontSize: '0.8rem',
                  transition: 'all 0.3s ease'
                }}>
                  {h}
                </div>

                {/* Column Index Label */}
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '6px' }}>[{idx}]</span>
              </div>
            );
          })}
        </div>

        {/* Algorithm Step Explanation Console */}
        <div style={{ 
          background: 'rgba(15, 23, 42, 0.8)', 
          border: '1px solid rgba(56, 189, 248, 0.2)', 
          borderRadius: '10px', 
          padding: '14px 18px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <CheckCircle2 size={20} color="var(--primary)" />
          <span style={{ fontSize: '0.875rem', color: '#f8fafc', fontWeight: 500 }}>
            {currentStep.explanation}
          </span>
        </div>

        {/* Playback Controls & Inputs */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center', justifyContent: 'space-between' }}>
          
          <div style={{ display: 'flex', gap: '8px' }}>
            <button 
              className={`btn ${isPlaying ? 'btn-danger' : 'btn-primary'}`} 
              onClick={() => setIsPlaying(!isPlaying)}
            >
              {isPlaying ? <><Pause size={16} /> Pause</> : <><Play size={16} /> Auto Play</>}
            </button>
            <button 
              className="btn btn-secondary" 
              onClick={() => setStepIndex(prev => Math.min(activeSteps.length - 1, prev + 1))}
              disabled={stepIndex >= activeSteps.length - 1}
            >
              <SkipForward size={16} /> Step Next
            </button>
            <button 
              className="btn btn-secondary" 
              onClick={() => { setStepIndex(0); setIsPlaying(false); }}
            >
              <RotateCcw size={16} /> Reset
            </button>
            <button 
              className="btn btn-secondary" 
              onClick={handleRandomize}
            >
              <Shuffle size={16} /> Random Array
            </button>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Speed:</span>
            <select 
              className="input-control" 
              style={{ width: '110px' }}
              value={playbackSpeed} 
              onChange={e => setPlaybackSpeed(Number(e.target.value))}
            >
              <option value="1200">0.5x Slow</option>
              <option value="800">1.0x Normal</option>
              <option value="400">2.0x Fast</option>
              <option value="150">5.0x Ultra</option>
            </select>
          </div>

        </div>

        {/* Custom Input Array Bar */}
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginTop: '4px' }}>
          <input 
            type="text" 
            className="input-control" 
            placeholder="Custom heights e.g., 1, 8, 6, 2, 5, 4, 8, 3, 7" 
            value={customInput}
            onChange={e => setCustomInput(e.target.value)}
          />
          <button className="btn btn-secondary" style={{ whiteSpace: 'nowrap' }} onClick={handleApplyInput}>
            Apply Array
          </button>
        </div>

      </div>
    </div>
  );
};
