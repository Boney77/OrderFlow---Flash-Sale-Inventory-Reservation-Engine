interface StockBadgeProps {
  stock: number;
  totalStock: number;
}

export function StockBadge({ stock, totalStock }: StockBadgeProps) {
  const percentage = totalStock > 0 ? (stock / totalStock) * 100 : 0;

  let colorClasses: string;
  let label: string;

  if (stock <= 0) {
    colorClasses = 'bg-gray-100 text-gray-800 border-gray-300';
    label = 'SOLD OUT';
  } else if (percentage <= 20) {
    colorClasses = 'bg-red-100 text-red-800 border-red-300';
    label = `${stock} left`;
  } else if (percentage <= 50) {
    colorClasses = 'bg-yellow-100 text-yellow-800 border-yellow-300';
    label = `${stock} left`;
  } else {
    colorClasses = 'bg-green-100 text-green-800 border-green-300';
    label = `${stock} in stock`;
  }

  return (
    <span
      className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${colorClasses}`}
    >
      {stock > 0 && (
        <span
          className={`w-2 h-2 rounded-full mr-2 ${
            percentage <= 20
              ? 'bg-red-500'
              : percentage <= 50
              ? 'bg-yellow-500'
              : 'bg-green-500'
          }`}
        />
      )}
      {label}
    </span>
  );
}
