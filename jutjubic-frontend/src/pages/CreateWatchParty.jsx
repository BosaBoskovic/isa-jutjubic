import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { watchPartyAPI } from '../api/api';
import { useWatchParty } from '../context/WatchPartyContext';
import '../components/Videos.css';

const CreateWatchParty = () => {
  const [roomName, setRoomName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { joinRoom } = useWatchParty();

  const handleCreate = async (e) => {
    e.preventDefault();
    
    if (!roomName.trim()) {
      setError('Please enter a room name');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const room = await watchPartyAPI.createRoom(roomName.trim());
      
      // Pridruži se svojoj sobi
      joinRoom(room.inviteCode);
      
      // Idi na watch party stranicu
      navigate(`/watch-party/${room.inviteCode}`);
    } catch (err) {
      setError(err.message || 'Failed to create room');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="videos-page">
      <h2 className="videos-title glow-title">Create Watch Party</h2>
      <p className="videos-subtitle">Start a room and watch videos together!</p>

      <div className="details-card" style={{ maxWidth: '500px', margin: '0 auto' }}>
        <form onSubmit={handleCreate}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 700 }}>
              Room Name
            </label>
            <input
              type="text"
              value={roomName}
              onChange={(e) => setRoomName(e.target.value)}
              placeholder="e.g., Friday Movie Night"
              disabled={loading}
              style={{
                width: '100%',
                padding: '0.75rem',
                borderRadius: '12px',
                border: '1px solid rgba(255,255,255,0.2)',
                background: 'rgba(0,0,0,0.3)',
                color: 'white',
                fontSize: '1rem',
              }}
            />
          </div>

          {error && <div className="videos-error">{error}</div>}

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '0.8rem',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #895159, #DFAEA3)',
              color: 'white',
              border: 'none',
              fontWeight: 700,
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.7 : 1,
            }}
          >
            {loading ? 'Creating...' : '🎬 Create Room'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default CreateWatchParty;