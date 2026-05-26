import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts';

interface StockDataPoint {
  time: string;
  stock: number;
}

interface StockChartProps {
  data: StockDataPoint[];
  maxStock?: number;
}

export function StockChart({ data, maxStock = 100 }: StockChartProps) {
  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg border border-gray-200">
        <p className="text-gray-500">Waiting for data...</p>
      </div>
    );
  }

  const latestStock = data[data.length - 1]?.stock ?? 0;
  const stockPercentage = maxStock > 0 ? (latestStock / maxStock) * 100 : 0;

  let strokeColor: string;
  let fillColor: string;

  if (stockPercentage <= 20) {
    strokeColor = '#ef4444';
    fillColor = 'url(#stockGradientRed)';
  } else if (stockPercentage <= 50) {
    strokeColor = '#eab308';
    fillColor = 'url(#stockGradientYellow)';
  } else {
    strokeColor = '#22c55e';
    fillColor = 'url(#stockGradientGreen)';
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Stock Level Over Time
      </h3>
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart
            data={data}
            margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
          >
            <defs>
              <linearGradient id="stockGradientGreen" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#22c55e" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="stockGradientYellow" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#eab308" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#eab308" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="stockGradientRed" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis
              dataKey="time"
              tick={{ fontSize: 12, fill: '#6b7280' }}
              tickLine={{ stroke: '#e5e7eb' }}
            />
            <YAxis
              domain={[0, maxStock]}
              tick={{ fontSize: 12, fill: '#6b7280' }}
              tickLine={{ stroke: '#e5e7eb' }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'white',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
              }}
              labelStyle={{ color: '#374151', fontWeight: 600 }}
            />
            <Area
              type="monotone"
              dataKey="stock"
              stroke={strokeColor}
              strokeWidth={2}
              fill={fillColor}
              dot={false}
              activeDot={{ r: 6, strokeWidth: 2, fill: 'white' }}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
