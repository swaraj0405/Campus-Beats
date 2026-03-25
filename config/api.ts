// API Configuration
export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:8081/api' : 'https://campus-beats.onrender.com/api'),
  WEBSOCKET_URL: import.meta.env.VITE_WS_URL || (import.meta.env.DEV ? 'http://localhost:8081/ws' : 'wss://campus-beats.onrender.com/ws'),
  TIMEOUT: 10000, // 10 seconds
};

// CORS configuration for development
export const CORS_CONFIG = {
  origin: ['http://localhost:3000', 'http://localhost:5173'],
  credentials: true,
};