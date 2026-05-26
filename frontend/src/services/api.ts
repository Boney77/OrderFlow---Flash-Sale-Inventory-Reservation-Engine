import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface InventoryItem {
  productId: string;
  productName: string;
  totalStock: number;
  availableStock: number;
}

export interface PurchaseResponse {
  reservationToken: string;
  expiresAt: string;
  message: string;
}

export interface ReservationDTO {
  reservationToken: string;
  userId: string;
  productId: string;
  quantity: number;
  status: 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED';
  expiresAt: string;
  createdAt: string;
}

export interface OrderDTO {
  orderId: string;
  reservationId: string;
  productId: string;
  userId: string;
  amount: number;
  status: 'CONFIRMED' | 'FAILED';
  createdAt: string;
}

export interface DashboardStats {
  stockRemaining: number;
  totalRequests: number;
  successfulOrders: number;
  failedRequests: number;
  ordersPerSecond: number;
}

export const getInventory = async (): Promise<InventoryItem[]> => {
  const response = await api.get<InventoryItem[]>('/inventory');
  return response.data;
};

export const purchase = async (
  productId: string,
  userId: string,
  quantity: number
): Promise<PurchaseResponse> => {
  const response = await api.post<PurchaseResponse>('/purchase', {
    productId,
    userId,
    quantity,
  });
  return response.data;
};

export const confirmOrder = async (
  reservationToken: string
): Promise<OrderDTO> => {
  const response = await api.post<OrderDTO>('/confirm-order', {
    reservationToken,
  });
  return response.data;
};

export const getReservation = async (
  token: string
): Promise<ReservationDTO> => {
  const response = await api.get<ReservationDTO>(`/reservation/${token}`);
  return response.data;
};

export const getDashboardStats = async (): Promise<DashboardStats> => {
  const response = await api.get<DashboardStats>('/dashboard/stats');
  return response.data;
};

export default api;
