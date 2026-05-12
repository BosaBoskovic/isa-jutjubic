import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { watchPartyAPI } from '../api/api';
import '../components/Videos.css';

const JoinWatchParty = () => {
  const [inviteCode, setInviteCode] = useState('');
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const loadRooms = async () => {
    setLoading(true);
    setError('');
    
    try {
      const data = await watchPartyAPI.getActiveRooms();
      setRooms(data);
    } catch (err) {
      setError('Failed to load rooms');
    } finally {
      setLoading(false);
    }
  };

  const handleJoinByCode = (e) => {
    e.preventDefault();
    
    if (!inviteCode.trim()) {
      setError('Please enter an invite code');
      return;
    }

    navigate(`/watch-party/${inviteCode.trim()}`);
  };

  const handleJoinRoom = (code) => {
    navigate(`/watch-party/${code}`);
  };

  return (
    <div className="videos-page">
      <h2 className="videos-title glow-title">Join Watch Party</h2>
      <p className="videos-subtitle">Enter an invite code or browse active rooms</p>

      {/* Join by code */}
      <div className="details-card" style={{ maxWidth: '500px', margin: '0 auto 2rem' }}>
        <form onSubmit={handleJoinByCode}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 700 }}>
              Invite Code
            </label>
            <input
              type="text"
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
              placeholder="e.g., A87346D1"
              style={{
                width: '100%',
                padding: '0.75rem',
                borderRadius: '12px',
                border: '1px solid rgba(255,255,255,0.2)',
                background: 'rgba(0,0,0,0.3)',
                color: 'white',
                fontSize: '1rem',
                textTransform: 'uppercase',
              }}
            />
          </div>

          {error && <div className="videos-error">{error}</div>}

          <button
            type="submit"
            style={{
              width: '100%',
              padding: '0.8rem',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #895159, #DFAEA3)',
              color: 'white',
              border: 'none',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            🚪 Join Room
          </button>
        </form>
      </div>

      {/* Active rooms */}
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h3 style={{ margin: 0 }}>Active Rooms</h3>
          <button
            onClick={loadRooms}
            disabled={loading}
            style={{
              padding: '0.6rem 1rem',
              borderRadius: '10px',
              background: 'rgba(255,255,255,0.1)',
              color: 'white',
              border: 'none',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? 'Loading...' : '🔄 Refresh'}
          </button>
        </div>

        <div className="video-grid">
          {rooms.map((room) => (
            <div key={room.id} className="details-card">
              <h4 style={{ margin: '0 0 0.5rem' }}>{room.name}</h4>
              <p style={{ margin: '0 0 0.8rem', opacity: 0.7, fontSize: '0.9rem' }}>
                By {room.creator?.username || 'Unknown'}
              </p>
              <button
                onClick={() => handleJoinRoom(room.inviteCode)}
                style={{
                  width: '100%',
                  padding: '0.7rem',
                  borderRadius: '10px',
                  background: 'linear-gradient(135deg, #895159, #DFAEA3)',
                  color: 'white',
                  border: 'none',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Join ({room.inviteCode})
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default JoinWatchParty;