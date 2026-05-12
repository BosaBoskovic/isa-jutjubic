import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/api';
import './UserProfile.css';
import VideoCard from '../components/VideoCard'; // koristi već postojeći VideoCard

const UserProfile = () => {
  const { username } = useParams();
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;

    (async () => {
      try {
        setLoading(true);
        setError('');
        const res = await api.get(`/users/${username}/public`);
        if (!alive) return;
        setUser(res.data);
      } catch (e) {
        if (!alive) return;
        setError(e?.response?.data?.message || e?.message || 'Failed to load user profile.');
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => { alive = false; };
  }, [username]);

  if (loading) return <div className="user-profile-page">Loading…</div>;
  if (error) return <div className="user-profile-page">{error}</div>;
  if (!user) return null;

  return (
    <div className="user-profile-page">
      <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>

      <div className="profile-header">
        <h1 className="profile-username">{user.username}</h1>
        <p className="profile-name">{user.firstName} {user.lastName}</p>
      </div>

      <h2 className="profile-section-title">Videos</h2>
      {user.videos && user.videos.length > 0 ? (
        <div className="profile-videos-grid">
          {user.videos.map((video) => (
            <VideoCard key={video.id} video={video}  showMeta={false}/>
          ))}
        </div>
      ) : (
        <p className="no-videos-text">This user hasn't uploaded any videos yet.</p>
      )}
    </div>
  );
};

export default UserProfile;
