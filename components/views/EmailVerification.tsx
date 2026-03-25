import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { apiService } from '../../services/apiService';
import { API_CONFIG } from '../../config/api';

const EmailVerification: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState('');
  const [countdown, setCountdown] = useState(5);

  useEffect(() => {
    const token = searchParams.get('token');
    
    if (!token) {
      setStatus('error');
      setMessage('Invalid verification link. No token provided.');
      return;
    }

    verifyEmail(token);
  }, [searchParams]);

  useEffect(() => {
    if (status === 'success' && countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (status === 'success' && countdown === 0) {
      navigate('/login');
    }
  }, [status, countdown, navigate]);

  const verifyEmail = async (token: string) => {
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/auth/verify-email?token=${encodeURIComponent(token)}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json();

      if (response.ok && data.verified) {
        setStatus('success');
        setMessage(data.message || 'Email verified successfully!');
      } else {
        setStatus('error');
        setMessage(data.error || 'Email verification failed.');
      }
    } catch (error) {
      setStatus('error');
      setMessage('Network error. Please try again later.');
    }
  };

  const handleReturnToLogin = () => {
    navigate('/login');
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-gray-900 p-4">
      <div className="bg-gray-800 p-8 rounded-2xl shadow-lg w-full max-w-md border border-gray-700 text-center">
        <h1 className="text-3xl font-bold text-purple-400 mb-2">Campus Beats</h1>
        <p className="text-gray-400 mb-8">Email Verification</p>

        {status === 'loading' && (
          <div className="space-y-4">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-400 mx-auto"></div>
            <p className="text-gray-300">Verifying your email...</p>
          </div>
        )}

        {status === 'success' && (
          <div className="space-y-4">
            <div className="text-green-400 text-6xl mb-4">✓</div>
            <h2 className="text-xl font-semibold text-green-400 mb-4">Email Verified!</h2>
            <p className="text-gray-300 mb-6">{message}</p>
            <p className="text-gray-400 mb-4">
              Redirecting to login in {countdown} seconds...
            </p>
            <button
              onClick={handleReturnToLogin}
              className="w-full bg-purple-600 text-white py-3 px-4 rounded-lg hover:bg-purple-700 transition-colors font-medium"
            >
              Go to Login Now
            </button>
          </div>
        )}

        {status === 'error' && (
          <div className="space-y-4">
            <div className="text-red-400 text-6xl mb-4">✗</div>
            <h2 className="text-xl font-semibold text-red-400 mb-4">Verification Failed</h2>
            <p className="text-gray-300 mb-6">{message}</p>
            <div className="space-y-3">
              <button
                onClick={handleReturnToLogin}
                className="w-full bg-purple-600 text-white py-3 px-4 rounded-lg hover:bg-purple-700 transition-colors font-medium"
              >
                Return to Login
              </button>
              <p className="text-gray-400 text-sm">
                If you continue to have issues, please contact support or try registering again.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default EmailVerification;