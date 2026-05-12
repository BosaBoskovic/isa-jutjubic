import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authAPI } from '../api/api';
import './Auth.css';

const VerifyEmail = () => {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    
    if (!token) {
      setStatus('error');
      setMessage('Activation token is missing.');
      return;
    }

    const verifyEmail = async () => {
      try {
        await authAPI.verifyEmail(token);
        setStatus('success');
        setMessage('Your account has been successfully activated!');
      } catch (error) {
        setStatus('error');
        const errorMsg = error.response?.data?.error || 'Error during account activation';
        setMessage(errorMsg);
      }
    };

    verifyEmail();
  }, [searchParams]);

  return (
    <div className="auth-container">
      <div className="auth-card">
        {status === 'loading' && (
          <>
            <div className="loading-spinner"></div>
            <h2>Activating account...</h2>
            <p>Please wait</p>
          </>
        )}

        {status === 'success' && (
          <>
            <div className="success-icon">✓</div>
            <h2>Activation successful!</h2>
            <p>{message}</p>
            <Link to="/login" className="btn-link">
              <button className="btn-submit">Go to login</button>
            </Link>
          </>
        )}

        {status === 'error' && (
          <>
            <div className="error-icon">✕</div>
            <h2>Error</h2>
            <p className="error-message">{message}</p>
            <Link to="/register" className="btn-link">
              <button className="btn-submit">Back to registration</button>
            </Link>
          </>
        )}
      </div>
    </div>
  );
};

export default VerifyEmail;