import { useState, useEffect, useCallback } from 'react';
import { StockBadge } from '../components/StockBadge';
import {
  getInventory,
  purchase,
  confirmOrder,
  InventoryItem,
} from '../services/api';

type SaleState = 'browsing' | 'reserved' | 'confirmed' | 'sold_out' | 'error';

function generateUserId(): string {
  return `user_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
}

export function FlashSalePage() {
  const [inventory, setInventory] = useState<InventoryItem | null>(null);
  const [saleState, setSaleState] = useState<SaleState>('browsing');
  const [reservationToken, setReservationToken] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<Date | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<number>(0);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [confirmationMessage, setConfirmationMessage] = useState<string | null>(null);

  const fetchInventory = useCallback(async () => {
    try {
      const items = await getInventory();
      if (items.length > 0) {
        setInventory(items[0]);
        if (items[0].availableStock <= 0) {
          setSaleState('sold_out');
        }
      }
    } catch (err) {
      console.error('Failed to fetch inventory:', err);
      setErrorMessage('Failed to load inventory');
    }
  }, []);

  useEffect(() => {
    fetchInventory();
    const interval = setInterval(fetchInventory, 5000);
    return () => clearInterval(interval);
  }, [fetchInventory]);

  useEffect(() => {
    if (!expiresAt || saleState !== 'reserved') return;

    const timer = setInterval(() => {
      const now = new Date();
      const remaining = Math.max(0, Math.floor((expiresAt.getTime() - now.getTime()) / 1000));
      setTimeRemaining(remaining);

      if (remaining <= 0) {
        setSaleState('browsing');
        setReservationToken(null);
        setExpiresAt(null);
        setErrorMessage('Reservation expired. Please try again.');
        fetchInventory();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [expiresAt, saleState, fetchInventory]);

  const handleBuyNow = async () => {
    if (!inventory) return;

    setLoading(true);
    setErrorMessage(null);

    try {
      const userId = generateUserId();
      const response = await purchase(inventory.productId, userId, 1);

      setReservationToken(response.reservationToken);
      setExpiresAt(new Date(response.expiresAt));
      setSaleState('reserved');
    } catch (err: any) {
      if (err.response?.status === 409) {
        setSaleState('sold_out');
        setErrorMessage('Sorry, this item is sold out!');
      } else {
        setErrorMessage(err.response?.data?.message || 'Purchase failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmOrder = async () => {
    if (!reservationToken) return;

    setLoading(true);
    setErrorMessage(null);

    try {
      await confirmOrder(reservationToken);
      setSaleState('confirmed');
      setConfirmationMessage('Order confirmed successfully!');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Confirmation failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const resetSale = () => {
    setSaleState('browsing');
    setReservationToken(null);
    setExpiresAt(null);
    setErrorMessage(null);
    setConfirmationMessage(null);
    fetchInventory();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50">
      <div className="max-w-4xl mx-auto px-4 py-12">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">
            ⚡ Flash Sale
          </h1>
          <p className="text-gray-600">Limited time offer - Don't miss out!</p>
        </div>

        {inventory && (
          <div className="bg-white rounded-2xl shadow-xl overflow-hidden">
            <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-8 py-6">
              <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold text-white">
                  {inventory.productName}
                </h2>
                <StockBadge
                  stock={inventory.availableStock}
                  totalStock={inventory.totalStock}
                />
              </div>
            </div>

            <div className="p-8">
              {errorMessage && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
                  <p className="text-red-700 text-center">{errorMessage}</p>
                </div>
              )}

              {saleState === 'browsing' && (
                <div className="text-center">
                  <div className="mb-6">
                    <p className="text-5xl font-bold text-gray-900 mb-2">$99.99</p>
                    <p className="text-gray-500 line-through">$199.99</p>
                    <span className="inline-block mt-2 px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">
                      50% OFF
                    </span>
                  </div>
                  <button
                    onClick={handleBuyNow}
                    disabled={loading || inventory.availableStock <= 0}
                    className="w-full max-w-xs mx-auto py-4 px-8 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-bold text-lg rounded-xl shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                  >
                    {loading ? (
                      <span className="flex items-center justify-center">
                        <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                        </svg>
                        Processing...
                      </span>
                    ) : (
                      'Buy Now'
                    )}
                  </button>
                </div>
              )}

              {saleState === 'reserved' && (
                <div className="text-center">
                  <div className="mb-6 p-6 bg-amber-50 border border-amber-200 rounded-xl">
                    <div className="text-amber-600 mb-2">
                      <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </div>
                    <h3 className="text-lg font-semibold text-amber-800 mb-1">
                      Item Reserved!
                    </h3>
                    <p className="text-amber-700 text-sm mb-4">
                      Complete your order before time runs out
                    </p>
                    <div className="text-4xl font-mono font-bold text-amber-900">
                      {formatTime(timeRemaining)}
                    </div>
                    <div className="w-full bg-amber-200 rounded-full h-2 mt-4">
                      <div
                        className="bg-amber-500 h-2 rounded-full transition-all duration-1000"
                        style={{ width: `${(timeRemaining / 300) * 100}%` }}
                      />
                    </div>
                  </div>

                  <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-600 mb-1">Reservation Token</p>
                    <p className="font-mono text-sm text-gray-800 break-all">
                      {reservationToken}
                    </p>
                  </div>

                  <button
                    onClick={handleConfirmOrder}
                    disabled={loading}
                    className="w-full max-w-xs mx-auto py-4 px-8 bg-green-600 text-white font-bold text-lg rounded-xl shadow-lg hover:bg-green-700 hover:shadow-xl transform hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {loading ? 'Confirming...' : 'Confirm Order'}
                  </button>
                </div>
              )}

              {saleState === 'confirmed' && (
                <div className="text-center">
                  <div className="mb-6 p-6 bg-green-50 border border-green-200 rounded-xl">
                    <div className="text-green-600 mb-2">
                      <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </div>
                    <h3 className="text-2xl font-bold text-green-800 mb-2">
                      {confirmationMessage}
                    </h3>
                    <p className="text-green-700">
                      Thank you for your purchase!
                    </p>
                  </div>
                  <button
                    onClick={resetSale}
                    className="py-3 px-6 bg-gray-100 text-gray-700 font-medium rounded-lg hover:bg-gray-200 transition-colors"
                  >
                    Back to Shop
                  </button>
                </div>
              )}

              {saleState === 'sold_out' && (
                <div className="text-center">
                  <div className="mb-6 p-6 bg-gray-100 rounded-xl">
                    <div className="text-gray-400 mb-2">
                      <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      </svg>
                    </div>
                    <h3 className="text-2xl font-bold text-gray-600 mb-2">
                      SOLD OUT
                    </h3>
                    <p className="text-gray-500">
                      This item is no longer available
                    </p>
                  </div>
                  <button
                    onClick={resetSale}
                    className="py-3 px-6 bg-gray-100 text-gray-700 font-medium rounded-lg hover:bg-gray-200 transition-colors"
                  >
                    Check Again
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        <div className="mt-8 text-center">
          <a
            href="/dashboard"
            className="text-indigo-600 hover:text-indigo-700 font-medium"
          >
            View Live Dashboard →
          </a>
        </div>
      </div>
    </div>
  );
}
