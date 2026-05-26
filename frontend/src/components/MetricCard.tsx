interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: React.ReactNode;
  trend?: 'up' | 'down' | 'neutral';
}

export function MetricCard({ title, value, subtitle, icon, trend }: MetricCardProps) {
  const trendColor =
    trend === 'up'
      ? 'text-green-600'
      : trend === 'down'
      ? 'text-red-600'
      : 'text-gray-500';

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-500 uppercase tracking-wide">
            {title}
          </p>
          <p className="mt-2 text-3xl font-bold text-gray-900">{value}</p>
          {subtitle && (
            <p className={`mt-1 text-sm ${trendColor}`}>{subtitle}</p>
          )}
        </div>
        {icon && (
          <div className="ml-4 p-3 bg-indigo-50 rounded-lg text-indigo-600">
            {icon}
          </div>
        )}
      </div>
    </div>
  );
}
