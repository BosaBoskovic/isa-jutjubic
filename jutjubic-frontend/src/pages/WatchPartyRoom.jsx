import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { watchPartyAPI } from '../api/api';
import { useWatchParty } from '../context/WatchPartyContext';
import { postsAPI } from '../api/api';
import VideoCard from '../components/VideoCard';
import '../components/Videos.css';

const WatchPartyRoom = () => {
  const { inviteCode } = useParams();
  const navigate = useNavigate();
  const { joinRoom, leaveRoom, playVideo, currentRoom, connected } = useWatchParty();
  
  const [room, setRoom] = useState(null);
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const isCreator = room?.creatorUsername === JSON.parse(localStorage.getItem('user') || '{}').username;

  useEffect(() => {
    const loadRoom = async () => {
      try {
        setLoading(true);
        const data = await watchPartyAPI.getRoomDetails(inviteCode);
        setRoom(data);
        
        // Pridruži se sobi preko WebSocketa
        joinRoom(inviteCode);
        
        // Učitaj video postove
        const myVideos = await postsAPI.getMine();
        setVideos(myVideos);
      } catch (err) {
        setError(err.message || 'Room not found');
      } finally {
        setLoading(false);
      }
    };

    loadRoom();

    return () => {
      leaveRoom();
    };
  }, [inviteCode]);

  const handlePlayVideo = (videoId) => {
    playVideo(videoId);
  };

  if (loading) return <div className="videos-page"><div className="videos-info">Loading room...</div></div>;
  if (error) return <div className="videos-page"><div className="videos-error">{error}</div></div>;

  return (
    <div className="videos-page">
      <button className="back-btn" onClick={() => navigate('/watch')}>← Leave Room</button>

      <div className="details-card" style={{ marginBottom: '2rem' }}>
        <h2 className="videos-title glow-title">{room.name}</h2>
        <p style={{ opacity: 0.8, marginBottom: '0.5rem' }}>
          Created by <b>{room.creatorUsername}</b>
        </p>
        <p style={{ opacity: 0.7, fontSize: '0.9rem' }}>
          Invite Code: <b style={{ color: '#DFAEA3' }}>{inviteCode}</b>
        </p>
        <p style={{ opacity: connected ? 1 : 0.5 }}>
          {connected ? '🟢 Connected' : '🔴 Disconnected'}
        </p>
      </div>

      {isCreator ? (
        <>
          <h3 style={{ marginBottom: '1rem' }}>Select a video to play for everyone:</h3>
          <div className="video-grid">
            {videos.map((v) => (
              <div key={v.id}>
                <VideoCard video={v} isMine showMeta={false} />
                <button
                  onClick={() => handlePlayVideo(v.id)}
                  style={{
                    width: '100%',
                    marginTop: '0.5rem',
                    padding: '0.7rem',
                    borderRadius: '10px',
                    background: 'linear-gradient(135deg, #895159, #DFAEA3)',
                    color: 'white',
                    border: 'none',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  ▶️ Play for Everyone
                </button>
              </div>
            ))}
          </div>
        </>
      ) : (
        <div className="videos-info">
          Waiting for {room.creatorUsername} to play a video...
        </div>
      )}
    </div>
  );
};

export default WatchPartyRoom;