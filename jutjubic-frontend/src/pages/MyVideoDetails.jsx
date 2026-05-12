import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { postsAPI } from '../api/api';
import '../components/Videos.css';
import HLSVideoPlayer from '../components/HLSVideoPlayer';
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
    headers: { Accept: 'application/json', 'Accept-Language': 'en' },
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

const MyVideoDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [video, setVideo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [locationName, setLocationName] = useState('');

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

  return (
    <div className="videos-page">
      <button className="back-btn" type="button" onClick={() => navigate(-1)}>
        ← Back
      </button>

      {loading && <div className="videos-info">Loading…</div>}
      {error && <div className="videos-error">{error}</div>}

      {!loading && !error && video && (
        <div className="details-card">
          <div className="details-header">
            <h2 className="details-title glow-title">{video.title}</h2>

            <div className="details-meta">
             <span>{toDate(video.createdAt)?.toLocaleString() ?? ''}</span>

              {video.isScheduled && (
                <>
                  <span className="details-dot">•</span>
                  <span className="scheduled-badge">
                   📅 Scheduled for: {toDate(video.scheduledAt)?.toLocaleString() ?? ''}

                  </span>
                </>
              )}
              {video.isScheduled && video.isLive && (
                <>
                  <span className="details-dot">•</span>
                  <span className="live-badge">🔴 LIVE NOW</span>
                </>
              )}
            </div>
          </div>

          <div className="details-player">
            <HLSVideoPlayer
              videoId={video.id}
              isScheduled={video.isScheduled}
              scheduledAt={video.scheduledAt}
              currentStreamOffsetSeconds={video.currentStreamOffsetSeconds}
              videoDurationSeconds={video.videoDurationSeconds}
              isOwner={true} // THIS IS YOUR VIDEO - full control
            />
          </div>

         <div className="details-description details-location">
  <span className="details-label">Description:</span>
  <p className="details-desc" style={{ marginTop: '0.4rem' }}>
    {video.description}
  </p>
</div>


          {/* tagovii */}
          {Array.isArray(video.tags) && video.tags.length > 0 && (
            <div className="details-tags">
              <span className="details-label">Tags:</span>
              <div className="tag-list">
                {video.tags.map((t) => (
                  <span key={t} className="tag-chip">
                    #{t}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* za lokaciju */}
          {(video.latitude != null && video.longitude != null) && (
            <div className="details-location">
              <span className="details-label">Location:</span>
              <span className="location-value">
                {locationName
                  ? locationName
                  : `${Number(video.latitude).toFixed(6)}, ${Number(video.longitude).toFixed(6)}`}
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MyVideoDetails;