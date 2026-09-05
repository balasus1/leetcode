import React, { useState, useTransition, useOptimistic, useActionState, useRef } from 'react';
import { 
  Sparkles, RefreshCw, CheckCircle2, 
  Cpu, ShieldAlert, Plus, Code2 
} from 'lucide-react';
import confetti from 'canvas-confetti';

interface Task {
  id: string;
  title: string;
  isPending?: boolean;
}

const INITIAL_TASKS: Task[] = [
  { id: '1', title: 'Prepare React 19 Fiber & Concurrent Mode presentation' },
  { id: '2', title: 'Review System Design microfrontends & module federation' },
  { id: '3', title: 'Practice 2-Pointer Container with Most Water simulation' }
];

export const React19Playground: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>(INITIAL_TASKS);
  const [failSimulated, setFailSimulated] = useState<boolean>(false);
  const [showCode, setShowCode] = useState<boolean>(false);

  // 1. React 19 useOptimistic State for Tasks
  const [optimisticTasks, setOptimisticTasks] = useOptimistic(
    tasks,
    (current, newTask: Task) => [...current, { ...newTask, isPending: true }]
  );

  // 2. React 19 useActionState for Form Submissions
  const [formState, formAction, isSubmitting] = useActionState(
    async (_prevState: { error?: string } | null, formData: FormData) => {
      const title = formData.get('taskTitle') as string;
      if (!title || !title.trim()) return { error: 'Task title cannot be empty' };

      const newTask: Task = {
        id: Date.now().toString(),
        title: title.trim()
      };

      // Apply optimistic update immediately
      setOptimisticTasks(newTask);

      // Simulate remote network latency
      await new Promise(r => setTimeout(r, 900));

      if (failSimulated) {
        return { error: 'Simulated 500 Server Error! Rolled back optimistic state.' };
      }

      setTasks(prev => [...prev, newTask]);
      confetti({ particleCount: 50, spread: 60, origin: { y: 0.8 } });
      return null;
    },
    null
  );

  // 3. useTransition Benchmark (Heavy 10,000 items filtering)
  const [filterInput, setFilterInput] = useState<string>('');
  const [deferredFilter, setDeferredFilter] = useState<string>('');
  const [isPendingTransition, startTransition] = useTransition();

  // Synthetic 10,000 dataset
  const heavyDataset = useRef(
    Array.from({ length: 8000 }, (_, i) => `Enterprise Microfrontend Node #${i + 1} - Cluster Zone ${(i % 8) + 1}`)
  ).current;

  const handleHeavySearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setFilterInput(val); // High-priority immediate typing responsiveness

    startTransition(() => {
      // Non-blocking low priority transition
      setDeferredFilter(val);
    });
  };

  const filteredHeavyData = deferredFilter
    ? heavyDataset.filter(item => item.toLowerCase().includes(deferredFilter.toLowerCase()))
    : heavyDataset.slice(0, 100);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span>⚡ React 19 Actions & Concurrency Playground</span>
            <span className="badge badge-purple">useActionState + useOptimistic</span>
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Interactive lab for Server Actions, Optimistic Rollbacks, and <code>useTransition</code> non-blocking UI rendering.
          </p>
        </div>

        <button className="btn btn-secondary" onClick={() => setShowCode(!showCode)}>
          <Code2 size={16} /> {showCode ? 'Hide Code' : 'Inspect React 19 Patterns'}
        </button>
      </div>

      {/* Code Modal */}
      {showCode && (
        <div className="glass-panel" style={{ padding: '20px', background: 'rgba(10, 15, 29, 0.95)', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
          <pre style={{ overflowX: 'auto', padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.5)', color: '#a5f3fc' }}>
            <code>{`// 1. React 19 useActionState + useOptimistic Pattern:
const [optimisticTasks, setOptimisticTasks] = useOptimistic(tasks, (cur, next) => [...cur, next]);

const [state, formAction, isPending] = useActionState(async (prev, formData) => {
  const item = { id: Date.now(), title: formData.get('title') };
  setOptimisticTasks(item); // 0ms Instant UI update
  await serverAction(item);  // Automatic rollback if this rejects!
  setTasks(prev => [...prev, item]);
}, null);

// 2. React 19 useTransition Priority Splitting:
setImmediateText(val); // Urgency: High (Input keystrokes)
startTransition(() => {
  setDeferredList(val); // Urgency: Low (Concurrent non-blocking render)
});`}</code>
          </pre>
        </div>
      )}

      {/* Grid: Task Manager with Rollback + Transition Benchmark */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
        
        {/* Lab 1: Optimistic Action Task Manager */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Sparkles size={18} color="var(--primary)" /> Optimistic Actions with Rollback
            </h3>
            <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', cursor: 'pointer', color: failSimulated ? '#f87171' : 'var(--text-muted)' }}>
              <input 
                type="checkbox" 
                checked={failSimulated} 
                onChange={e => setFailSimulated(e.target.checked)} 
              />
              Simulate Server Failure
            </label>
          </div>

          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Tasks render immediately with a glowing status. If "Simulate Server Failure" is checked, the server rejects and React 19 rolls back state automatically.
          </p>

          <form action={formAction} style={{ display: 'flex', gap: '10px' }}>
            <input 
              type="text" 
              name="taskTitle" 
              className="input-control" 
              placeholder="Enter new task..." 
              disabled={isSubmitting}
            />
            <button type="submit" className="btn btn-primary" disabled={isSubmitting} style={{ whiteSpace: 'nowrap' }}>
              {isSubmitting ? <RefreshCw size={16} className="animate-spin" /> : <Plus size={16} />} Add Task
            </button>
          </form>

          {formState?.error && (
            <div className="badge badge-warning" style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#f87171', border: '1px solid rgba(239, 68, 68, 0.3)', padding: '8px 12px' }}>
              <ShieldAlert size={16} /> {formState.error}
            </div>
          )}

          {/* Task List */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '280px', overflowY: 'auto' }}>
            {optimisticTasks.map(task => (
              <div 
                key={task.id} 
                style={{ 
                  padding: '12px 14px', 
                  borderRadius: '8px', 
                  background: task.isPending ? 'rgba(56, 189, 248, 0.1)' : 'rgba(255,255,255,0.03)',
                  border: task.isPending ? '1px dashed var(--primary)' : '1px solid rgba(255,255,255,0.06)',
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'space-between',
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  {task.isPending ? (
                    <RefreshCw size={14} className="animate-spin" color="var(--primary)" />
                  ) : (
                    <CheckCircle2 size={16} color="#34d399" />
                  )}
                  <span style={{ fontSize: '0.85rem', color: task.isPending ? 'var(--primary)' : '#f8fafc' }}>
                    {task.title}
                  </span>
                </div>
                {task.isPending && (
                  <span className="badge badge-primary" style={{ fontSize: '0.65rem' }}>Optimistic</span>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Lab 2: useTransition Concurrency Benchmark */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Cpu size={18} color="var(--secondary)" /> Non-Blocking <code>useTransition</code>
            </h3>
            {isPendingTransition && (
              <span className="badge badge-warning animate-pulse-subtle">
                <RefreshCw size={12} className="animate-spin" /> Concurrently Rendering
              </span>
            )}
          </div>

          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Filtering through <strong>8,000 items</strong> in real-time. Type rapidly into the input: typing never freezes because list calculation is scheduled on concurrent low-priority lanes.
          </p>

          <input 
            type="text" 
            className="input-control" 
            placeholder="Search 8,000 cluster nodes..." 
            value={filterInput}
            onChange={handleHeavySearch}
          />

          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', justifyContent: 'space-between' }}>
            <span>Matches: {filteredHeavyData.length} records</span>
            <span>Transition Priority: Low Lane</span>
          </div>

          {/* Virtual List Items */}
          <div style={{ 
            maxHeight: '220px', 
            overflowY: 'auto', 
            background: 'rgba(0,0,0,0.3)', 
            padding: '10px', 
            borderRadius: '8px', 
            border: '1px solid rgba(255,255,255,0.06)',
            display: 'flex',
            flexDirection: 'column',
            gap: '4px'
          }}>
            {filteredHeavyData.slice(0, 40).map((item, idx) => (
              <div key={idx} style={{ fontSize: '0.775rem', padding: '6px 8px', borderRadius: '4px', background: 'rgba(255,255,255,0.02)', color: '#cbd5e1' }}>
                {item}
              </div>
            ))}
            {filteredHeavyData.length > 40 && (
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textAlign: 'center', padding: '6px' }}>
                ... and {filteredHeavyData.length - 40} more items
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};
