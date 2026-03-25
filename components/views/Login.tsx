import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../../services/apiService';
import { API_CONFIG } from '../../config/api';

interface LoginProps {
  onLogin: (user: any, token: string) => void;
}

const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const navigate = useNavigate();
  const [isLogin, setIsLogin] = useState(true);
  const [registrationStep, setRegistrationStep] = useState<'email' | 'verify' | 'complete'>('email');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    verificationCode: ''
  });
  const [passwordCriteria, setPasswordCriteria] = useState({
    length: false,
    uppercase: false,
    lowercase: false,
    number: false,
    special: false
  });
  const [passwordsMatch, setPasswordsMatch] = useState(null);
  const [emailAvailable, setEmailAvailable] = useState(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [verificationSent, setVerificationSent] = useState(false);
  const [countdown, setCountdown] = useState(0);

  // Countdown timer for resend verification code
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    let updatedFormData = { ...formData, [name]: value };
    
    // Check email availability for registration
    if (name === 'email' && !isLogin && value && value.includes('@')) {
      checkEmailAvailability(value);
    } else if (name === 'email') {
      setEmailAvailable(null);
    }
    
    setFormData(updatedFormData);
    setError('');
    setSuccessMessage(''); // Clear success message when user types
    
    // Real-time password validation
    if (name === 'password') {
      validatePasswordCriteria(value);
    }
    
    // Check password match
    if (name === 'confirmPassword' || (name === 'password' && formData.confirmPassword)) {
      const password = name === 'password' ? value : formData.password;
      const confirmPassword = name === 'confirmPassword' ? value : formData.confirmPassword;
      setPasswordsMatch(password === confirmPassword && password !== '');
    }
  };
  
  const validatePasswordCriteria = (password: string) => {
    setPasswordCriteria({
      length: password.length >= 8,
      uppercase: /[A-Z]/.test(password),
      lowercase: /[a-z]/.test(password),
      number: /\d/.test(password),
      special: /[@$!%*?&]/.test(password)
    });
  };

  const checkEmailAvailability = async (email: string) => {
    if (!email || (!email.includes('@kluniversity.in') && !email.includes('@gmail.com'))) {
      setEmailAvailable(null);
      return;
    }

    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/users/email/${encodeURIComponent(email)}`);
      if (response.status === 200) {
        // User exists
        setEmailAvailable(false);
      } else if (response.status === 404) {
        // User doesn't exist, email is available
        setEmailAvailable(true);
      }
    } catch (error) {
      // On error, don't show validation status
      setEmailAvailable(null);
    }
  };

  const validateForm = () => {
    // Email validation for all steps
    if (!formData.email.includes('@kluniversity.in') && !formData.email.includes('@gmail.com')) {
      setError('Please enter a valid email (@kluniversity.in or @gmail.com)');
      return false;
    }

    if (isLogin) {
      // Login validation
      if (!formData.password) {
        setError('Password is required');
        return false;
      }
      return true;
    }

    // Registration validation based on step
    if (registrationStep === 'email') {
      if (emailAvailable === false) {
        setError('This email is already registered. Please use a different email.');
        return false;
      }
      return true;
    }

    if (registrationStep === 'verify') {
      if (!formData.verificationCode || formData.verificationCode.length !== 6) {
        setError('Please enter the 6-digit verification code');
        return false;
      }
      return true;
    }

    if (registrationStep === 'complete') {
      if (formData.name.trim().length < 2) {
        setError('Name must be at least 2 characters long');
        return false;
      }
      
      const allCriteriaMet = Object.values(passwordCriteria).every(criteria => criteria);
      if (!allCriteriaMet) {
        setError('Password must meet all criteria');
        return false;
      }
      
      if (formData.password !== formData.confirmPassword) {
        setError('Passwords do not match');
        return false;
      }
    }
    
    return true;
  };

  const handleResendCode = async () => {
    if (countdown > 0) return;
    
    setLoading(true);
    setError('');
    
    try {
      await apiService.auth.sendVerificationCode(formData.email);
      setCountdown(60);
    } catch (err: any) {
      setError('Failed to resend verification code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      if (isLogin) {
        // Login
        const response = await apiService.auth.login({
          email: formData.email,
          password: formData.password
        });
        
        if (response.token && response.user) {
          localStorage.setItem('authToken', response.token);
          onLogin(response.user, response.token);
          navigate('/');
        } else {
          setError('Login failed. Please try again.');
        }
      } else {
        // Registration flow based on step
        if (registrationStep === 'email') {
          // Send verification code
          const response = await apiService.auth.sendVerificationCode(formData.email);
          setVerificationSent(true);
          setRegistrationStep('verify');
          setCountdown(60); // 60 second countdown for resend
          // Show success message
          setError(''); // Clear any previous errors
          setSuccessMessage(response.message || 'Verification code sent to your email!');
          // Clear success message after 3 seconds
          setTimeout(() => setSuccessMessage(''), 3000);
        } else if (registrationStep === 'verify') {
          // Verify the code
          const response = await apiService.auth.verifyCode(formData.email, formData.verificationCode);
          if (response.verified) {
            setError('');
            setSuccessMessage('Email verified successfully! Please complete your registration.');
            setRegistrationStep('complete');
            setTimeout(() => setSuccessMessage(''), 3000);
          } else {
            setError('Invalid verification code. Please try again.');
          }
        } else if (registrationStep === 'complete') {
          // Complete registration
          const response = await apiService.auth.completeRegistration(
            formData.email,
            formData.name,
            formData.password,
            formData.confirmPassword
          );
          
          if (response.token && response.user) {
            localStorage.setItem('authToken', response.token);
            onLogin(response.user, response.token);
            navigate('/');
          } else {
            setError('Registration failed. Please try again.');
          }
        }
      }
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Authentication failed';
      
      // Provide user-friendly messages for common errors
      if (errorMessage.includes('User with this email already exists')) {
        setError('This email is already registered. Please try logging in instead or use a different email.');
      } else if (errorMessage.includes('No account found with this email address')) {
        setError('No account found with this email. Please check your email or register first.');
      } else if (errorMessage.includes('Incorrect password')) {
        setError('Incorrect password. Please check your password and try again.');
      } else if (errorMessage.includes('Invalid email or password')) {
        setError('Invalid email or password. Please check your credentials and try again.');
      } else if (errorMessage.includes('Email not verified')) {
        setError('Please verify your email before logging in. Check your inbox for the verification link.');
      } else {
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-gray-900 p-4">
      <div className="bg-gray-800 p-8 rounded-2xl shadow-lg w-full max-w-md border border-gray-700">
        <h1 className="text-3xl font-bold text-center text-purple-400 mb-2">Campus Beats</h1>
        <p className="text-center text-gray-400 mb-8">Your campus, your sound.</p>

        <div className="flex mb-6">
          <button
            type="button"
            onClick={() => {
              setIsLogin(true);
              setError('');
              setEmailAvailable(null);
              setRegistrationStep('email');
              setVerificationSent(false);
              setCountdown(0);
              setFormData({
                name: '',
                email: '',
                password: '',
                confirmPassword: '',
                verificationCode: ''
              });
              setPasswordsMatch(null);
            }}
            className={`flex-1 py-2 px-4 rounded-l-lg font-medium transition-colors ${
              isLogin 
                ? 'bg-purple-600 text-white' 
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            Login
          </button>
          <button
            type="button"
            onClick={() => {
              setIsLogin(false);
              setError('');
              setEmailAvailable(null);
              setRegistrationStep('email');
              setVerificationSent(false);
              setCountdown(0);
              setFormData({
                name: '',
                email: '',
                password: '',
                confirmPassword: '',
                verificationCode: ''
              });
              setPasswordsMatch(null);
            }}
            className={`flex-1 py-2 px-4 rounded-r-lg font-medium transition-colors ${
              !isLogin 
                ? 'bg-purple-600 text-white' 
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            Register
          </button>
        </div>

        {!isLogin && (
          <div className="mb-4">
            <div className="flex items-center justify-between text-sm text-gray-400">
              <span className={registrationStep === 'email' ? 'text-purple-400 font-medium' : ''}>
                1. Email
              </span>
              <span className={registrationStep === 'verify' ? 'text-purple-400 font-medium' : ''}>
                2. Verify
              </span>
              <span className={registrationStep === 'complete' ? 'text-purple-400 font-medium' : ''}>
                3. Complete
              </span>
            </div>
            <div className="mt-2 bg-gray-700 rounded-full h-2">
              <div 
                className="bg-purple-600 h-2 rounded-full transition-width duration-300"
                style={{ 
                  width: registrationStep === 'email' ? '33%' : 
                         registrationStep === 'verify' ? '66%' : '100%' 
                }}
              ></div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Email Step - Login or Registration */}
          {(isLogin || (!isLogin && registrationStep === 'email')) && (
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-300 mb-2">
                Email
              </label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-colors"
                placeholder="Enter your email"
                required
              />
              {!isLogin && emailAvailable !== null && (
                <div className={`mt-2 text-sm ${
                  emailAvailable 
                    ? 'text-green-400' 
                    : 'text-red-400'
                }`}>
                  {emailAvailable 
                    ? '✓ This email is available' 
                    : '✗ This email is already registered. Please try a different email.'}
                </div>
              )}
            </div>
          )}

          {/* Password for Login */}
          {isLogin && (
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-300 mb-2">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  className="w-full px-4 py-3 pr-12 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-colors"
                  placeholder="••••••••"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300 focus:outline-none"
                >
                  {showPassword ? (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
                    </svg>
                  ) : (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  )}
                </button>
              </div>
            </div>
          )}

          {/* Verification Code Step */}
          {!isLogin && registrationStep === 'verify' && (
            <div>
              <div className="text-center mb-4">
                <h3 className="text-lg font-medium text-white mb-2">Verify Your Email</h3>
                <p className="text-gray-400 text-sm">
                  We've sent a 6-digit verification code to <span className="text-purple-400">{formData.email}</span>
                </p>
              </div>
              
              <div>
                <label htmlFor="verificationCode" className="block text-sm font-medium text-gray-300 mb-2">
                  Verification Code
                </label>
                <input
                  type="text"
                  id="verificationCode"
                  name="verificationCode"
                  value={formData.verificationCode}
                  onChange={handleInputChange}
                  className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-colors text-center text-2xl tracking-widest"
                  placeholder="000000"
                  maxLength={6}
                  required
                />
              </div>
              
              <div className="text-center mt-4">
                <button
                  type="button"
                  onClick={handleResendCode}
                  disabled={countdown > 0 || loading}
                  className="text-purple-400 hover:text-purple-300 disabled:text-gray-500 disabled:cursor-not-allowed text-sm"
                >
                  {countdown > 0 ? `Resend code in ${countdown}s` : 'Resend verification code'}
                </button>
              </div>
            </div>
          )}

          {/* Complete Registration Step */}
          {!isLogin && registrationStep === 'complete' && (
            <>
              <div className="text-center mb-4">
                <h3 className="text-lg font-medium text-white mb-2">Complete Your Registration</h3>
                <p className="text-gray-400 text-sm">
                  Email verified! Now set up your account.
                </p>
              </div>

              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-300 mb-2">
                  Full Name
                </label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-colors"
                  placeholder="Enter your full name"
                  required
                />
              </div>
              
              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-300 mb-2">
                  Password
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    id="password"
                    name="password"
                    value={formData.password}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 pr-12 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-colors"
                    placeholder="••••••••"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300 focus:outline-none"
                  >
                    {showPassword ? (
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
                      </svg>
                    ) : (
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    )}
                  </button>
                </div>
                {formData.password && !formData.confirmPassword && (
                  <div className="mt-3 space-y-2">
                    <div className="text-sm text-gray-300 mb-2">Password Requirements:</div>
                    <div className="grid grid-cols-1 gap-1 text-xs">
                      <div className={`flex items-center ${passwordCriteria.length ? 'text-green-400' : 'text-gray-400'}`}>
                        <span className="mr-2">{passwordCriteria.length ? '✓' : '○'}</span>
                        At least 8 characters
                      </div>
                      <div className={`flex items-center ${passwordCriteria.uppercase ? 'text-green-400' : 'text-gray-400'}`}>
                        <span className="mr-2">{passwordCriteria.uppercase ? '✓' : '○'}</span>
                        One uppercase letter
                      </div>
                      <div className={`flex items-center ${passwordCriteria.lowercase ? 'text-green-400' : 'text-gray-400'}`}>
                        <span className="mr-2">{passwordCriteria.lowercase ? '✓' : '○'}</span>
                        One lowercase letter
                      </div>
                      <div className={`flex items-center ${passwordCriteria.number ? 'text-green-400' : 'text-gray-400'}`}>
                        <span className="mr-2">{passwordCriteria.number ? '✓' : '○'}</span>
                        One number
                      </div>
                      <div className={`flex items-center ${passwordCriteria.special ? 'text-green-400' : 'text-gray-400'}`}>
                        <span className="mr-2">{passwordCriteria.special ? '✓' : '○'}</span>
                        One special character (@$!%*?&)
                      </div>
                    </div>
                  </div>
                )}
              </div>
              
              <div>
                <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-300 mb-2">
                  Confirm Password
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? "text" : "password"}
                    id="confirmPassword"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 pr-12 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-colors"
                    placeholder="••••••••"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300 focus:outline-none"
                  >
                    {showConfirmPassword ? (
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
                      </svg>
                    ) : (
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    )}
                  </button>
                </div>
                {formData.confirmPassword && (
                  <div className={`mt-2 text-sm flex items-center ${
                    passwordsMatch ? 'text-green-400' : 'text-red-400'
                  }`}>
                    <span className="mr-2">{passwordsMatch ? '✓' : '✗'}</span>
                    {passwordsMatch ? 'Passwords match' : 'Passwords do not match'}
                  </div>
                )}
              </div>
            </>
          )}
          
          {successMessage && (
            <div className="bg-green-900/50 border border-green-500 text-green-200 px-4 py-3 rounded-lg text-sm">
              {successMessage}
            </div>
          )}
          
          {error && (
            <div className="bg-red-900/50 border border-red-500 text-red-200 px-4 py-3 rounded-lg text-sm">
              {error}
            </div>
          )}
          
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-purple-600 text-white font-bold py-3 px-4 rounded-lg hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-purple-500"
          >
            {loading ? 'Please wait...' : 
             isLogin ? 'Login' : 
             registrationStep === 'email' ? 'Send Verification Code' :
             registrationStep === 'verify' ? 'Verify Code' :
             'Complete Registration'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;