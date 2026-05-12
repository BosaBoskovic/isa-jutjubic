import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { postsAPI } from '../api/api';
import '../components/Videos.css';
import CommentsSection from '../components/CommentsSection';
import HLSVideoPlayer from '../components/HLSVideoPlayer';
import LiveChat from '../components/LiveChat';
import { toDate } from '../utils/date';



const reverseGeocodeCityCountry = async (lat, lng) => {
  const url =
    `https://nominatim.openstreetmap.org/reverse?` +
    new URLSearchParams({
      lat: String(lat),
      lon: String(lng),
      format: 'json',
      zoom: '10',
      addressdetails: '1',
    });

  const res = await fetch(url, {
    headers: {
      Accept: 'application/json',
      'Accept-Language': 'en',
    },
  });

  if (!res.ok) throw new Error('Reverse geocoding failed');

  const geo = await res.json();
  const addr = geo.address || {};

  const city =
    addr.city ||
    addr.town ||
    addr.village ||
    addr.municipality ||
    addr.county ||
    '';

  const country = addr.country || '';

  return city && country ? `${city}, ${country}` : city || country || '';
};

const VideoDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [video, setVideo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [locationName, setLocationName] = useState('');
  const [showLoginPrompt, setShowLoginPrompt] = useState(false);

  const isLoggedIn = !!localStorage.getItem('token');

  const onToggleLike = async () => {
    if (!isLoggedIn) {
      setShowLoginPrompt(true);
      return;
    }
    if (!video?.id) return;

    try {
      const res = await postsAPI.toggleLike(video.id);
      setVideo((prev) => ({
        ...prev,
        likesCount: res.likesCount,
        likedByMe: res.likedByMe,
      }));
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to toggle like.');
    }
  };

  useEffect(() => {
    let alive = true;

    (async () => {
      try {
        setLoading(true);
        setError('');

        const data = await postsAPI.getById(id);
        if (!alive) return;

        setVideo(data);
        setLocationName('');

        if (data?.latitude != null && data?.longitude != null) {
          const lat = Number(data.latitude);
          const lng = Number(data.longitude);

          const cacheKey = `geo:${lat.toFixed(4)},${lng.toFixed(4)}`;
          const cached = sessionStorage.getItem(cacheKey);

          if (cached) {
            setLocationName(cached);
          } else {
            try {
              const pretty = await reverseGeocodeCityCountry(lat, lng);
              if (!alive) return;

              if (pretty) {
                sessionStorage.setItem(cacheKey, pretty);
                setLocationName(pretty);
              }
            } catch {
              setLocationName('');
            }
          }
        }
      } catch (e) {
        if (!alive) return;
        setError(e?.response?.data?.message || e?.message || 'Failed to load video details.');
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [id]);

  const scrollToComments = () => {
    if (!isLoggedIn) {
      setShowLoginPrompt(true);
      return;
    }
    const element = document.getElementById('comments-anchor');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="videos-page">
      <button className="back-btn" type="button" onClick={() => navigate(-1)}>
        ← Back
      </button>

      {loading && <div className="videos-info">Loading…</div>}
      {error && <div className="videos-error">{error}</div>}

      {!loading && !error && video && (
        <div className="details-layout">
          <div className="details-main">
            <div className="details-player">
              <HLSVideoPlayer
                videoId={video.id}
                isScheduled={video.scheduled}
                scheduledAt={video.scheduledAt}
                currentStreamOffsetSeconds={video.currentStreamOffsetSeconds}
                videoDurationSeconds={video.videoDurationSeconds}
                isOwner={video.isOwner}
              />
              {video?.scheduled && (
  <LiveChat video={video} isLoggedIn={isLoggedIn} />
)}

            </div>

            <h2 className="details-title glow-title">{video.title}</h2>

            <div className="details-meta-row">
              <div className="details-meta-left">
                <span className="details-author">
                  By{' '}
                  <b
                    className="clickable-user"
                    onClick={() => navigate(`/users/${video.authorUsername}`)}
                    style={{ cursor: 'pointer', color: '#1e90ff' }}
                    title={`View ${video.authorName}'s profile`}
                  >
                    {video.authorName ?? 'Unknown'}
                  </b>
                </span>
                <span className="details-dot">•</span>
              <span className="details-date">
  {toDate(video.createdAt)?.toLocaleDateString() ?? '—'}
</span>


                {video.scheduled && video.live && (

                  <>
                    <span className="details-dot">•</span>
                    <span className="live-badge">🔴 LIVE</span>
                  </>
                )}
              </div>

              <div className="details-toolbar">
                <span className="like-count">👍 {video.likesCount ?? 0}</span>
                <span className="views-count">👁 {video.viewsCount ?? 0}</span>

                <button
                  className={`action-btn ${video.likedByMe ? 'action-btn-liked' : ''}`}
                  type="button"
                  onClick={onToggleLike}
                  aria-pressed={!!video.likedByMe}
                  title={video.likedByMe ? 'Click to unlike' : 'Click to like'}
                >
                  👍 <span>{video.likedByMe ? 'Liked' : 'Like'}</span>
                </button>

                <button
                  className="action-btn"
                  type="button"
                  onClick={scrollToComments}
                  title="Jump to comments"
                >
                  💬 <span>Comment</span>
                </button>
              </div>
            </div>

            {(Array.isArray(video.tags) && video.tags.length > 0) ||
            (video.latitude != null && video.longitude != null) ? (
              <div className="details-chips">
                {Array.isArray(video.tags) &&
                  video.tags.map((t) => (
                    <span key={t} className="chip">
                      #{t}
                    </span>
                  ))}

                {video.latitude != null && video.longitude != null && (
                  <span className="chip chip-location">
                    📍{' '}
                    <span className="chip-text">
                      {locationName
                        ? locationName
                        : `${Number(video.latitude).toFixed(6)}, ${Number(video.longitude).toFixed(6)}`}
                    </span>
                  </span>
                )}
              </div>
            ) : null}

            {/* Opis videa */}
            <div className="details-desc-card">
              <div className="details-desc-title">Description</div>
              <p className="details-desc">{video.description}</p>
            </div>
            <CommentsSection
              postId={video.id}
              isLoggedIn={isLoggedIn}
            />
          </div>

          {showLoginPrompt && (
            <div className="login-prompt-overlay">
              <div className="login-prompt-card">
                <p>You must be logged in to like or comment on this video.</p>
                <button className="btn-register" onClick={() => navigate('/register')}>
                  Register
                </button>
                <button className="btn-close" onClick={() => setShowLoginPrompt(false)}>
                  Close
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default VideoDetails;