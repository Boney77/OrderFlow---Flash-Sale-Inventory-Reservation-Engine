import { useEffect, useState, useCallback, useRef } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { DashboardStats, getDashboardStats, WS_URL } from '../services/api';

interface UseDashboardSocketReturn {
  stats: DashboardStats | null;
  connected: boolean;
  error: string | null;
}

const RECONNECT_DELAY = 5000;

export function useDashboardSocket(): UseDashboardSocketReturn {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);

  const connect = useCallback(() => {
    if (clientRef.current?.active) {
      return;
    }

    try {
      const client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        reconnectDelay: RECONNECT_DELAY,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          setConnected(true);
          setError(null);

          client.subscribe('/topic/dashboard', (message: IMessage) => {
            try {
              const dashboardStats: DashboardStats = JSON.parse(message.body);
              setStats(dashboardStats);
            } catch (e) {
              console.error('Failed to parse dashboard stats:', e);
            }
          });
        },
        onDisconnect: () => {
          setConnected(false);
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame.headers['message']);
          setError(`Connection error: ${frame.headers['message']}`);
          setConnected(false);
        },
        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          setError('Live updates unavailable (backend not reachable)');
          setConnected(false);
        },
        onWebSocketClose: () => {
          setConnected(false);
        },
      });

      clientRef.current = client;
      client.activate();
    } catch (e) {
      console.error('Failed to initialise WebSocket client:', e);
      setError('Live updates unavailable');
      setConnected(false);
    }
  }, []);

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch((e) => console.error('Failed to fetch initial stats:', e));

    connect();

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [connect]);

  return { stats, connected, error };
}
