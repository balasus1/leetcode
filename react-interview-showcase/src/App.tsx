import React, { useState } from 'react';
import { Boxes, Droplets, Globe, Sparkles, CheckCircle } from 'lucide-react';
import { CounterApp } from './components/CounterApp';
import { WaterApp } from './components/WaterApp';
import { PaginatedListApp } from './components/PaginatedListApp';
import { React19Playground } from './components/React19Playground';

type ActiveTab = 'counter' | 'water' | 'paginated' | 'react19';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ActiveTab>('counter');

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Top Glassmorphic Navigation Bar */}
      <header style={{
        position: 'sticky',
        top: 0,
        zIndex: 100,
        background: 'rgba(9, 13, 22, 0.85)',
        backdropFilter: 'blur(20px)',
        borderBottom: '1px solid var(--border-color)',
        padding: '16px 24px'
      }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          
          {/* Logo & Title */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{
              width: '40px',
              height: '40px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #0284c7 0%, #38bdf8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 20px rgba(56, 189, 248, 0.4)'
            }}>
              <Boxes size={22} color="#ffffff" />
            </div>
            <div>
              <h1 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.02em', lineHeight: 1.2 }}>
                React 19 <span style={{ color: 'var(--primary)' }}>Interview & Production Lab</span>
              </h1>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Targeted for Principal / Senior Remote Frontend Roles
              </div>
            </div>
          </div>

          {/* Navigation Tabs */}
          <nav style={{ display: 'flex', gap: '8px', background: 'rgba(15, 23, 42, 0.8)', padding: '6px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.06)' }}>
            <button 
              className="btn"
              style={{
                background: activeTab === 'counter' ? 'var(--primary)' : 'transparent',
                color: activeTab === 'counter' ? '#000' : 'var(--text-muted)',
                padding: '8px 14px',
                fontSize: '0.8rem'
              }}
              onClick={() => setActiveTab('counter')}
            >
              <Boxes size={15} /> Time-Travel Counter
            </button>

            <button 
              className="btn"
              style={{
                background: activeTab === 'water' ? 'var(--primary)' : 'transparent',
                color: activeTab === 'water' ? '#000' : 'var(--text-muted)',
                padding: '8px 14px',
                fontSize: '0.8rem'
              }}
              onClick={() => setActiveTab('water')}
            >
              <Droplets size={15} /> Water Visualizer (LC 11 & 42)
            </button>

            <button 
              className="btn"
              style={{
                background: activeTab === 'paginated' ? 'var(--primary)' : 'transparent',
                color: activeTab === 'paginated' ? '#000' : 'var(--text-muted)',
                padding: '8px 14px',
                fontSize: '0.8rem'
              }}
              onClick={() => setActiveTab('paginated')}
            >
              <Globe size={15} /> Paginated API Explorer
            </button>

            <button 
              className="btn"
              style={{
                background: activeTab === 'react19' ? 'var(--primary)' : 'transparent',
                color: activeTab === 'react19' ? '#000' : 'var(--text-muted)',
                padding: '8px 14px',
                fontSize: '0.8rem'
              }}
              onClick={() => setActiveTab('react19')}
            >
              <Sparkles size={15} /> React 19 Actions
            </button>
          </nav>

          {/* Status Pills */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="badge badge-success">
              <CheckCircle size={12} /> React 19.x Ready
            </span>
          </div>

        </div>
      </header>

      {/* Main Content Area */}
      <main style={{ flex: 1, maxWidth: '1280px', width: '100%', margin: '0 auto', padding: '32px 24px' }}>
        {activeTab === 'counter' && <CounterApp />}
        {activeTab === 'water' && <WaterApp />}
        {activeTab === 'paginated' && <PaginatedListApp />}
        {activeTab === 'react19' && <React19Playground />}
      </main>

      {/* Footer */}
      <footer style={{
        marginTop: 'auto',
        borderTop: '1px solid var(--border-color)',
        padding: '24px',
        textAlign: 'center',
        color: 'var(--text-muted)',
        fontSize: '0.8rem',
        background: 'rgba(9, 13, 22, 0.6)'
      }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            Built with <strong>React 19 + TypeScript + Vite</strong> for high-bar remote interview assessments.
          </div>
          <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
            <span>Undo/Redo Hooks</span>
            <span>•</span>
            <span>AbortController Signals</span>
            <span>•</span>
            <span>Two-Pointer O(N)</span>
            <span>•</span>
            <span>SWR In-Memory Cache</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;
