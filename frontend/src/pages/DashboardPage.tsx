import { useState, useEffect, useRef } from 'react';
import { useDashboardSocket } from '../hooks/useDashboardSocket';
import { StockChart } from '../components/StockChart';
import { MetricCard } from '../components/MetricCard';

interface StockDataPoint {
  time: string;
  stock: number;
}

const MAX_DATA_POINTS = 60;

export function DashboardPage() {
  const { stats, connected, error } = useDashboardSocket();
  const [stockHistory, setStockHistory] = useState<StockDataPoint[]>([]);
  const maxStockRef = useRef<number>(100);

  useEffect(() => {
    if (stats) {
      if (stockHistory.length === 0 && stats.stockRemaining > maxStockRef.current) {
        maxStockRef.current = stats.stockRemaining;
      }

      const now = new Date();
      const timeString = now.toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });

      setStockHistory((prev) => {
        const newHistory = [...prev, { time: timeString, stock: stats.stockRemaining }];
        if (newHistory.length > MAX_DATA_POINTS) {
          return newHistory.slice(-MAX_DATA_POINTS);
        }
        return newHistory;
      });
    }
  }, [stats]);

  const formatNumber = (num: number): string => {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    }
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toLocaleString();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              Flash Sale Dashboard
            </h1>
            <p className="text-gray-600 mt-1">Real-time monitoring</p>
          </div>

          <div className="flex items-center gap-4">
            <a
              href="/"
              className="text-indigo-600 hover:text-indigo-700 font-medium"
            >
              ← Back to Sale
            </a>
            <div
              className={`flex items-center gap-2 px-4 py-2 rounded-full ${
                connected
                  ? 'bg-green-100 text-green-800'
                  : 'bg-red-100 text-red-800'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  connected ? 'bg-green-500 animate-pulse' : 'bg-red-500'
                }`}
              />
              {connected ? 'Live' : 'Disconnected'}
            </div>
          </div>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-red-700">{error}</p>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <MetricCard
            title="Stock Remaining"
            value={stats ? formatNumber(stats.stockRemaining) : '-'}
            subtitle={
              stats && maxStockRef.current > 0
                ? `${Math.round((stats.stockRemaining / maxStockRef.current) * 100)}% of initial stock`
                : undefined
            }
            icon={
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            }
          />

          <MetricCard
            title="Total Requests"
            value={stats ? formatNumber(stats.totalRequests) : '-'}
            subtitle="All purchase attempts"
            icon={
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            }
          />

          <MetricCard
            title="Successful Orders"
            value={stats ? formatNumber(stats.successfulOrders) : '-'}
            subtitle={
              stats && stats.totalRequests > 0
                ? `${((stats.successfulOrders / stats.totalRequests) * 100).toFixed(1)}% success rate`
                : undefined
            }
            trend="up"
            icon={
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            }
          />

          <MetricCard
            title="Failed Requests"
            value={stats ? formatNumber(stats.failedRequests) : '-'}
            subtitle="Sold out responses"
            trend="down"
            icon={
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            }
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
          <div className="lg:col-span-2">
            <StockChart data={stockHistory} maxStock={maxStockRef.current} />
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Performance Metrics
            </h3>

            <div className="space-y-6">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-gray-600">
                    Orders per Second
                  </span>
                  <span className="text-2xl font-bold text-indigo-600">
                    {stats ? stats.ordersPerSecond.toFixed(1) : '-'}
                  </span>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-2">
                  <div
                    className="bg-indigo-600 h-2 rounded-full transition-all duration-500"
                    style={{
                      width: `${Math.min((stats?.ordersPerSecond || 0) * 10, 100)}%`,
                    }}
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-gray-600">
                    Stock Depletion
                  </span>
                  <span className="text-sm text-gray-500">
                    {stats
                      ? `${maxStockRef.current - stats.stockRemaining} sold`
                      : '-'}
                  </span>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all duration-500 ${
                      stats && stats.stockRemaining <= maxStockRef.current * 0.2
                        ? 'bg-red-500'
                        : stats && stats.stockRemaining <= maxStockRef.current * 0.5
                        ? 'bg-yellow-500'
                        : 'bg-green-500'
                    }`}
                    style={{
                      width: `${
                        stats
                          ? ((maxStockRef.current - stats.stockRemaining) /
                              maxStockRef.current) *
                            100
                          : 0
                      }%`,
                    }}
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-gray-200">
                <h4 className="text-sm font-medium text-gray-600 mb-3">
                  Quick Stats
                </h4>
                <dl className="space-y-2">
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Initial Stock</dt>
                    <dd className="text-sm font-medium text-gray-900">
                      {maxStockRef.current}
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Units Sold</dt>
                    <dd className="text-sm font-medium text-gray-900">
                      {stats ? maxStockRef.current - stats.stockRemaining : '-'}
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Rejection Rate</dt>
                    <dd className="text-sm font-medium text-gray-900">
                      {stats && stats.totalRequests > 0
                        ? `${((stats.failedRequests / stats.totalRequests) * 100).toFixed(1)}%`
                        : '-'}
                    </dd>
                  </div>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            System Status
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="flex items-center gap-3">
              <div className={`w-3 h-3 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`} />
              <div>
                <p className="text-sm font-medium text-gray-900">WebSocket</p>
                <p className="text-xs text-gray-500">
                  {connected ? 'Connected' : 'Disconnected'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 rounded-full bg-green-500" />
              <div>
                <p className="text-sm font-medium text-gray-900">API</p>
                <p className="text-xs text-gray-500">Operational</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 rounded-full bg-green-500" />
              <div>
                <p className="text-sm font-medium text-gray-900">Redis</p>
                <p className="text-xs text-gray-500">Operational</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 rounded-full bg-green-500" />
              <div>
                <p className="text-sm font-medium text-gray-900">Database</p>
                <p className="text-xs text-gray-500">Operational</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
