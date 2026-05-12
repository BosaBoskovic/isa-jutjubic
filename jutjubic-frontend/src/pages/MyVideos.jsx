import { useEffect, useState } from 'react';
import { postsAPI } from '../api/api';
import VideoCard from '../components/VideoCard';
import '../components/Videos.css';

const MyVideos = () => {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;

    (async () => {
      try {
        setLoading(true);
        setError('');
        const data = await postsAPI.getMine();
        if (!alive) return;
        setVideos(Array.isArray(data) ? data : []);
      } catch (e) {
        if (!alive) return;
        setError(e?.response?.data?.message || e?.message || 'Failed to load your videos.');
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, []);

  return (
    <div className="videos-page">
      <h2 className="videos-title glow-title">My Videos</h2>
      <p className="videos-subtitle">Videos you uploaded.</p>

      {loading && <div className="videos-info">Loading…</div>}

      {error && <div className="videos-error">{error}</div>}

      {!loading && !error && (
        <div className="video-grid">
          {videos.map((v) => (
  <VideoCard key={v.id} video={v} isMine />
))}

        </div>
      )}
    </div>
  );
};

export default MyVideos;
