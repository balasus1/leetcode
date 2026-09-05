import React, { useState, useEffect, useRef, useCallback } from 'react';
import { 
  Search, RefreshCw, ChevronLeft, ChevronRight, 
  Database, AlertCircle, Code2, Star
} from 'lucide-react';

interface ProductItem {
  id: number;
  title: string;
  description: string;
  category: string;
  price: number;
  rating: number;
  stock: number;
  thumbnail: string;
}

interface ApiResponse {
  products: ProductItem[];
  total: number;
  skip: number;
  limit: number;
}

// Global In-Memory SWR Promise & Response Cache
const memoryCache = new Map<string, { data: ApiResponse; timestamp: number }>();
const inFlightRequests = new Map<string, Promise<ApiResponse>>();
const CACHE_TTL_MS = 60_000; // 1 minute stale time

export const PaginatedListApp: React.FC = () => {
  // Query States
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [debouncedSearch, setDebouncedSearch] = useState<string>('');
  const [page, setPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(6);
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [viewMode, setViewMode] = useState<'paged' | 'infinite'>('paged');

  // Network & Data States
  const [items, setItems] = useState<ProductItem[]>([]);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [cacheHit, setCacheHit] = useState<boolean>(false);
  const [fetchLatency, setFetchLatency] = useState<number>(0);

  // Selected item detail modal
  const [activeItem, setActiveItem] = useState<ProductItem | null>(null);
  const [showCode, setShowCode] = useState<boolean>(false);

  // Infinite Scroll Sentinel Ref & Accumulated List
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const [infiniteItems, setInfiniteItems] = useState<ProductItem[]>([]);
  const [hasMoreInfinite, setHasMoreInfinite] = useState<boolean>(true);

  // AbortController Ref for active search/page switch cancellation
  const abortControllerRef = useRef<AbortController | null>(null);
  const renderCountRef = useRef(0);
  renderCountRef.current += 1;

  // 1. Debounce Search Input (300ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setPage(1); // Reset page on new search query
      setInfiniteItems([]);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  // Unified Fetcher with SWR Caching & Deduplication
  const fetchProducts = useCallback(async (
    query: string, 
    pageNum: number, 
    limit: number, 
    category: string,
    signal?: AbortSignal
  ): Promise<ApiResponse> => {
    const skip = (pageNum - 1) * limit;
    const cacheKey = `${query}_${pageNum}_${limit}_${category}`;

    // 1. Check in-memory cache
    const cached = memoryCache.get(cacheKey);
    if (cached && (Date.now() - cached.timestamp < CACHE_TTL_MS)) {
      setCacheHit(true);
      return cached.data;
    }

    // 2. Check in-flight promise deduplication
    if (inFlightRequests.has(cacheKey)) {
      return inFlightRequests.get(cacheKey)!;
    }

    setCacheHit(false);

    // 3. Construct URL (DummyJSON / fallback)
    let url = `https://dummyjson.com/products/search?q=${encodeURIComponent(query)}&limit=${limit}&skip=${skip}`;
    if (category !== 'all' && !query) {
      url = `https://dummyjson.com/products/category/${category}?limit=${limit}&skip=${skip}`;
    }

    const fetchPromise = (async () => {
      try {
        const res = await fetch(url, { signal });
        if (!res.ok) throw new Error(`HTTP Error ${res.status}`);
        const data: ApiResponse = await res.json();
        
        // Cache result
        memoryCache.set(cacheKey, { data, timestamp: Date.now() });
        return data;
      } finally {
        inFlightRequests.delete(cacheKey);
      }
    })();

    inFlightRequests.set(cacheKey, fetchPromise);
    return fetchPromise;
  }, []);

  // Main Data Fetch Trigger
  useEffect(() => {
    // Abort previous in-flight request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    const controller = new AbortController();
    abortControllerRef.current = controller;

    setIsLoading(true);
    setError(null);
    const start = performance.now();

    fetchProducts(debouncedSearch, page, pageSize, selectedCategory, controller.signal)
      .then(res => {
        setItems(res.products);
        setTotalCount(res.total);
        setFetchLatency(Math.round(performance.now() - start));
        setIsLoading(false);

        if (viewMode === 'infinite') {
          setInfiniteItems(prev => page === 1 ? res.products : [...prev, ...res.products]);
          setHasMoreInfinite(res.products.length > 0 && (page * pageSize < res.total));
        }
      })
      .catch(err => {
        if (err.name !== 'AbortError') {
          setError(err.message || 'Failed to fetch data from remote API');
          setIsLoading(false);
        }
      });

    return () => controller.abort();
  }, [debouncedSearch, page, pageSize, selectedCategory, fetchProducts, viewMode]);

  // Hover Prefetch Next Page (Instant Zero-Latency UI)
  const handlePrefetchNextPage = () => {
    const totalPages = Math.ceil(totalCount / pageSize);
    if (page < totalPages) {
      fetchProducts(debouncedSearch, page + 1, pageSize, selectedCategory).catch(() => {});
    }
  };

  // Infinite Scroll IntersectionObserver Sentinel
  useEffect(() => {
    if (viewMode !== 'infinite' || isLoading || !hasMoreInfinite) return;

    const observer = new IntersectionObserver(
      entries => {
        if (entries[0].isIntersecting && !isLoading && hasMoreInfinite) {
          setPage(prev => prev + 1);
        }
      },
      { rootMargin: '200px' }
    );

    if (sentinelRef.current) {
      observer.observe(sentinelRef.current);
    }

    return () => observer.disconnect();
  }, [viewMode, isLoading, hasMoreInfinite]);

  const totalPages = Math.ceil(totalCount / pageSize) || 1;
  const displayItems = viewMode === 'infinite' ? infiniteItems : items;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header & Telemetry */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span>🌐 Enterprise Paginated External API Explorer</span>
            <span className="badge badge-primary">SWR + AbortController</span>
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Live remote data fetching, 300ms debouncing, in-flight request cancellation, hover prefetching, and infinite scrolling.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className={`badge ${cacheHit ? 'badge-success' : 'badge-primary'}`}>
            <Database size={12} /> {cacheHit ? 'SWR Cache HIT' : 'Network Call'} ({fetchLatency}ms)
          </span>
          <button className="btn btn-secondary" onClick={() => setShowCode(!showCode)}>
            <Code2 size={16} /> {showCode ? 'Hide Code' : 'Inspect API Pipeline'}
          </button>
        </div>
      </div>

      {/* Code Architecture View */}
      {showCode && (
        <div className="glass-panel" style={{ padding: '20px', background: 'rgba(10, 15, 29, 0.95)', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--primary)' }}>
              Architecture: In-flight Deduplication + Map Cache + AbortSignal Cleanups + IntersectionObserver
            </span>
            <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: '0.75rem' }} onClick={() => setShowCode(false)}>Close</button>
          </div>
          <pre style={{ overflowX: 'auto', padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.5)', color: '#a5f3fc' }}>
            <code>{`// Core Interview Techniques:
1. Debounce (300ms): Prevents keystroke flooding to backend servers.
2. AbortController: Automatically aborts obsolete network requests on query changes.
3. In-Flight Request Deduplication: inFlightRequests.has(key) returns existing Promise to prevent duplicate parallel fetches.
4. Hover Prefetch: Pre-fetches page N+1 when hovering over the "Next" button for zero perceived latency.
5. Dual Mode: Easily toggles between Classic URL-Friendly Pagination and Infinite Scrolling with IntersectionObserver.`}</code>
          </pre>
        </div>
      )}

      {/* Filter, Search & View Controls Bar */}
      <div className="glass-panel" style={{ padding: '18px 24px', display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'center', justifyContent: 'space-between' }}>
        
        {/* Search Input */}
        <div style={{ position: 'relative', flex: '1 1 260px' }}>
          <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input 
            type="text" 
            className="input-control" 
            style={{ paddingLeft: '38px' }}
            placeholder="Live search products (e.g., phone, laptop, perfume)..." 
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
          {isLoading && (
            <RefreshCw size={16} className="animate-spin" style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--primary)' }} />
          )}
        </div>

        {/* Category Filters */}
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <select 
            className="input-control" 
            style={{ width: '150px' }}
            value={selectedCategory} 
            onChange={e => { setSelectedCategory(e.target.value); setPage(1); }}
          >
            <option value="all">All Categories</option>
            <option value="smartphones">Smartphones</option>
            <option value="laptops">Laptops</option>
            <option value="fragrances">Fragrances</option>
            <option value="groceries">Groceries</option>
          </select>

          {/* View Mode Toggle */}
          <div style={{ display: 'flex', background: 'rgba(15, 23, 42, 0.8)', padding: '4px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.08)' }}>
            <button 
              className="btn" 
              style={{ 
                padding: '6px 12px', 
                fontSize: '0.75rem', 
                background: viewMode === 'paged' ? 'var(--primary)' : 'transparent',
                color: viewMode === 'paged' ? '#000' : 'var(--text-muted)' 
              }}
              onClick={() => { setViewMode('paged'); setPage(1); }}
            >
              Classic Paging
            </button>
            <button 
              className="btn" 
              style={{ 
                padding: '6px 12px', 
                fontSize: '0.75rem', 
                background: viewMode === 'infinite' ? 'var(--primary)' : 'transparent',
                color: viewMode === 'infinite' ? '#000' : 'var(--text-muted)' 
              }}
              onClick={() => { setViewMode('infinite'); setPage(1); setInfiniteItems([]); }}
            >
              Infinite Scroll
            </button>
          </div>
        </div>

      </div>

      {/* Error Boundary / Failure Message */}
      {error && (
        <div className="glass-panel" style={{ padding: '16px', borderColor: 'rgba(239, 68, 68, 0.4)', background: 'rgba(239, 68, 68, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#f87171' }}>
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
          <button className="btn btn-secondary" onClick={() => setPage(p => p)}>
            <RefreshCw size={14} /> Retry Request
          </button>
        </div>
      )}

      {/* Items Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }}>
        {isLoading && displayItems.length === 0 ? (
          // Shimmer Loading Skeletons
          Array.from({ length: pageSize }).map((_, i) => (
            <div key={i} className="glass-panel" style={{ padding: '20px', minHeight: '220px', animation: 'pulse-subtle 1.5s infinite' }}>
              <div style={{ height: '140px', background: 'rgba(255,255,255,0.04)', borderRadius: '10px', marginBottom: '12px' }} />
              <div style={{ height: '18px', width: '70%', background: 'rgba(255,255,255,0.06)', borderRadius: '4px', marginBottom: '8px' }} />
              <div style={{ height: '14px', width: '40%', background: 'rgba(255,255,255,0.04)', borderRadius: '4px' }} />
            </div>
          ))
        ) : displayItems.length === 0 ? (
          <div className="glass-panel" style={{ gridColumn: '1 / -1', padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
            No products found matching "{searchTerm}". Try a different keyword or category.
          </div>
        ) : (
          displayItems.map(item => (
            <div 
              key={item.id} 
              className="glass-panel" 
              style={{ 
                padding: '20px', 
                display: 'flex', 
                flexDirection: 'column', 
                justifyContent: 'space-between',
                transition: 'transform 0.2s ease, border-color 0.2s ease',
                cursor: 'pointer'
              }}
              onClick={() => setActiveItem(item)}
            >
              <div>
                <div style={{ 
                  height: '140px', 
                  background: 'rgba(0,0,0,0.3)', 
                  borderRadius: '10px', 
                  marginBottom: '14px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  overflow: 'hidden'
                }}>
                  <img 
                    src={item.thumbnail} 
                    alt={item.title} 
                    style={{ maxHeight: '100%', maxWidth: '100%', objectFit: 'contain' }}
                    loading="lazy"
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px', marginBottom: '6px' }}>
                  <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc', lineHeight: 1.3 }}>{item.title}</h4>
                  <span className="badge badge-purple" style={{ fontSize: '0.7rem' }}>{item.category}</span>
                </div>

                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '12px', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                  {item.description}
                </p>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '12px', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
                <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--primary)' }}>${item.price}</span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8rem', color: '#fbbf24' }}>
                  <Star size={14} fill="#fbbf24" /> {item.rating}
                </span>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Infinite Scroll Sentinel */}
      {viewMode === 'infinite' && (
        <div ref={sentinelRef} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
          {isLoading && <span className="badge badge-primary"><RefreshCw size={14} className="animate-spin" /> Loading more products...</span>}
          {!hasMoreInfinite && <span>✓ All {totalCount} products loaded</span>}
        </div>
      )}

      {/* Classic Pagination Bar */}
      {viewMode === 'paged' && (
        <div className="glass-panel" style={{ padding: '16px 24px', display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: '14px' }}>
          
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Showing <strong>{(page - 1) * pageSize + 1} - {Math.min(page * pageSize, totalCount)}</strong> of <strong>{totalCount}</strong> items
          </div>

          {/* Page Buttons with Prefetch on Hover */}
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <button 
              className="btn btn-secondary" 
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1}
            >
              <ChevronLeft size={16} /> Prev
            </button>

            <span style={{ padding: '6px 14px', fontSize: '0.875rem', fontWeight: 700, color: 'var(--primary)', background: 'rgba(56, 189, 248, 0.1)', borderRadius: '8px' }}>
              Page {page} of {totalPages}
            </span>

            <button 
              className="btn btn-secondary" 
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              onMouseEnter={handlePrefetchNextPage}
              disabled={page >= totalPages}
            >
              Next <ChevronRight size={16} />
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            <span>Items/page:</span>
            <select 
              className="input-control" 
              style={{ width: '70px', padding: '6px 8px' }}
              value={pageSize} 
              onChange={e => { setPageSize(Number(e.target.value)); setPage(1); }}
            >
              <option value="4">4</option>
              <option value="6">6</option>
              <option value="12">12</option>
            </select>
          </div>

        </div>
      )}

      {/* Detail Modal */}
      {activeItem && (
        <div style={{ 
          position: 'fixed', 
          top: 0, 
          left: 0, 
          right: 0, 
          bottom: 0, 
          background: 'rgba(0,0,0,0.7)', 
          backdropFilter: 'blur(8px)',
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          padding: '20px',
          zIndex: 999 
        }}>
          <div className="glass-panel glow-border" style={{ maxWidth: '480px', width: '100%', padding: '28px', background: '#0f172a' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#f8fafc' }}>{activeItem.title}</h3>
              <button className="btn btn-secondary" style={{ padding: '4px 10px' }} onClick={() => setActiveItem(null)}>✕</button>
            </div>
            <img src={activeItem.thumbnail} alt={activeItem.title} style={{ width: '100%', maxHeight: '200px', objectFit: 'contain', borderRadius: '10px', marginBottom: '16px', background: '#000' }} />
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '16px' }}>{activeItem.description}</p>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)' }}>${activeItem.price}</span>
              <span className="badge badge-success">In Stock: {activeItem.stock}</span>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
