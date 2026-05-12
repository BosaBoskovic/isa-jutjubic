import { useEffect, useState, useRef, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import { useNavigate } from 'react-router-dom';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './MapView.css';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const createThumbnailIcon = (videoId) => {
  return L.divIcon({
    className: 'custom-thumbnail-marker',
    html: `
      <div class="marker-thumbnail-wrapper">
        <img 
          src="http://localhost:8080/api/posts/${videoId}/thumbnail" 
          alt="Video"
          class="marker-thumbnail-img"
          onerror="this.style.display='none'; this.parentElement.classList.add('thumbnail-error');"
        />
        <div class="marker-play-overlay">▶</div>
      </div>
    `,
    iconSize: [60, 60],
    iconAnchor: [30, 60],
    popupAnchor: [0, -60],
  });
};

const MapEventHandler = ({ onBoundsChange}) => {
  const map = useMap();
  const debounceTimer = useRef(null);
  const isMoving = useRef(false);

  useEffect(() => {
    const handleMoveStart = () => {
      isMoving.current = true;
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    };

    const handleMoveEnd = () => {
      isMoving.current = false;
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
      const zoom = map.getZoom();
      
      debounceTimer.current = setTimeout(() => {
        if (!isMoving.current) {
          const bounds = map.getBounds();
          onBoundsChange({
            minLat: bounds.getSouth(),
            maxLat: bounds.getNorth(),
            minLng: bounds.getWest(),
            maxLng: bounds.getEast(),
            zoom,
          });
        }
      }, 800);
    };

    map.on('movestart', handleMoveStart);
    map.on('moveend', handleMoveEnd);
    map.on('zoomend', handleMoveEnd);
    handleMoveEnd();

    return () => {
      map.off('movestart', handleMoveStart);
      map.off('moveend', handleMoveEnd);
      map.off('zoomend', handleMoveEnd);
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [map, onBoundsChange]);

  return null;
};

const createMarkerIcon = (video) => {
  if (video.cluster) {
    return L.divIcon({
      className: 'cluster-marker',
      html: `<div class="cluster-marker-circle">${video.count}</div>`,
      iconSize: [50, 50],
      iconAnchor: [25, 25],
      popupAnchor: [0, -25],
    });
  } else {
    return L.divIcon({
      className: 'custom-thumbnail-marker',
      html: `
        <div class="marker-thumbnail-wrapper">
          <img 
            src="http://localhost:8080/api/posts/${video.id}/thumbnail" 
            alt="Video"
            class="marker-thumbnail-img"
            onerror="this.style.display='none'; this.parentElement.classList.add('thumbnail-error');"
          />
          <div class="marker-play-overlay">▶</div>
        </div>
      `,
      iconSize: [60, 60],
      iconAnchor: [30, 60],
      popupAnchor: [0, -60],
    });
  }
};


const MapView = () => {
  const navigate = useNavigate();
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [timePeriod, setTimePeriod] = useState('ALL');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [hasLoadedOnce, setHasLoadedOnce] = useState(false);
  
  const dropdownRef = useRef(null);
  const currentBoundsRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  
const lon2tile = (lon, zoom) => Math.floor(((lon + 180) / 360) * Math.pow(2, zoom));
const lat2tile = (lat, zoom) =>
  Math.floor(
    ((1 -
      Math.log(
        Math.tan((lat * Math.PI) / 180) + 1 / Math.cos((lat * Math.PI) / 180)
      ) /
        Math.PI) /
      2) *
      Math.pow(2, zoom)
  );

const abortControllerRef = useRef(null);

const fetchVideos = useCallback(async (bounds) => {
  if (abortControllerRef.current) {
    abortControllerRef.current.abort();
  }
  abortControllerRef.current = new AbortController();
  const signal = abortControllerRef.current.signal;

  try {
    setLoading(true);
    const z = bounds.zoom;
    const minX = lon2tile(bounds.minLng, z);
    const maxX = lon2tile(bounds.maxLng, z);
    const minY = lat2tile(bounds.maxLat, z);
    const maxY = lat2tile(bounds.minLat, z);

    const tileRequests = [];
    const token = localStorage.getItem('token');
    const headers = { 
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      'Content-Type': 'application/json'
    };

    for (let x = minX; x <= maxX; x++) {
      for (let y = minY; y <= maxY; y++) {
        const url = `http://localhost:8080/api/map/tile?z=${z}&x=${x}&y=${y}&period=${timePeriod}`;
        tileRequests.push(fetch(url, { headers, signal }).then(res => res.json()));
      }
    }

    const results = await Promise.all(tileRequests);
    
    // ... spajanje rezultata (allVideos) ...
    // BITNO: Koristi Map umesto Set-a za ID-eve da bi spajanje bilo O(n)
    const videoMap = new Map();
    results.forEach(tileData => {
      if (tileData?.videos) {
        tileData.videos.forEach(v => videoMap.set(v.id, v));
      }
    });

    setVideos(Array.from(videoMap.values()));
  } catch (e) {
    if (e.name === 'AbortError') return; // Ignoriši grešku ako smo sami otkazali zahtev
    setError('Greška pri učitavanju');
  } finally {
    setLoading(false);
  }
}, [timePeriod]);

  useEffect(()=>{
    if(currentBoundsRef.current && hasLoadedOnce){
        fetchVideos(currentBoundsRef.current);
    }
  }, [timePeriod, fetchVideos, hasLoadedOnce]);

  const handleTimePeriodChange = (newPeriod) => {
    setTimePeriod(newPeriod);
    setDropdownOpen(false);
  };

  const periodLabels = {
    'ALL': 'All Time',
    'LAST_30_DAYS': 'Last 30 Days',
    'THIS_YEAR': 'This Year'
  };


  const europeBounds = [
    [34.5, -25.0], // Jugozapad (ispod Maroka / Atlantik)
    [71.0, 45.0]   // Severoistok (Sever Norveške / Rusija granica)
  ];

  return (
    <div className="map-page-container">
      <div className="map-two-column-layout">
        
        <div className="map-column-left">
          <div className="map-square-container">
            <MapContainer
              center={[44.0165, 21.0059]}
              zoom={4}
              minZoom={2}
              className="leaflet-map"
              zoomControl={true}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />

              <MapEventHandler onBoundsChange={fetchVideos}/>

              {videos.map((video) => {
  if (!video.latitude || !video.longitude) return null;

  const markerKey = video.cluster ? `cluster-${video.id}` : `video-${video.id}`;

  return (
    <Marker
      key={markerKey}
      position={[video.latitude, video.longitude]}
      icon={createMarkerIcon(video)}
    >
      <Popup className="custom-popup" autoPan={false}>
        <div className="popup-content">
          {video.cluster ? (
            <div>
              <div className="popup-thumbnail-wrapper">
                <img
                  src={`http://localhost:8080/api/posts/${video.id}/thumbnail`}
                  alt="Representative video"
                  className="popup-thumbnail"
                />
              </div>

              <h3>{video.count} videos in this area</h3>

              <button
                className="popup-watch-btn"
                onClick={() => navigate(`/videos/${video.id}`)}
              >
                Watch a video →
              </button>
            </div>
          ) : (
            <div>
              <div className="popup-thumbnail-wrapper">
                <img
                  src={`http://localhost:8080/api/posts/${video.id}/thumbnail`}
                  alt={video.title}
                  className="popup-thumbnail"
                />
              </div>

              <div className="popup-details">
                <h3 className="popup-title">{video.title}</h3>
                <div className="popup-meta">
                  <span>👁 {video.viewsCount || 0}</span>
                  <span>
                    {video.createdAt
                      ? new Date(video.createdAt).toLocaleDateString()
                      : ''}
                  </span>
                </div>

                <button
                  className="popup-watch-btn"
                  onClick={() => navigate(`/videos/${video.id}`)}
                >
                  Watch Now →
                </button>
              </div>
            </div>
          )}
        </div>
      </Popup>
    </Marker>
  );
})}

            </MapContainer>

            {hasLoadedOnce && videos.length === 0 && !loading && (
              <div className="map-empty-overlay">
                <div className="empty-icon">🔍</div>
                <p className="empty-title">No videos in this area</p>
                <p className="empty-hint">Try zooming out or adjusting the time filter</p>
              </div>
            )}

            {loading && (
              <div className="map-loading-overlay">
                <div className="loading-spinner"></div>
                <p>Loading videos...</p>
              </div>
            )}
          </div>
        </div>

        <div className="map-column-right">
          
          <div className="map-header-right">
            <h1 className="map-main-title">🗺️ Video Map Explorer</h1>
            <p className="map-subtitle">Discover videos by their location around the world</p>
          </div>

          <div className="map-controls-card">
            <h3 className="controls-title">Filters & Settings</h3>
            
            <div className="control-group">
              <label className="control-label">Time Period</label>
              <div className="time-dropdown-wrapper" ref={dropdownRef}>
                <button
                  className="dropdown-trigger"
                  onClick={() => setDropdownOpen(!dropdownOpen)}
                >
                  <span>📅</span>
                  <span>{periodLabels[timePeriod]}</span>
                  <span className={`dropdown-arrow ${dropdownOpen ? 'open' : ''}`}>▼</span>
                </button>

                {dropdownOpen && (
                  <div className="dropdown-menu">
                    {Object.entries(periodLabels).map(([value, label]) => (
                      <button
                        key={value}
                        className={`dropdown-item ${timePeriod === value ? 'active' : ''}`}
                        onClick={() => handleTimePeriodChange(value)}
                      >
                        {timePeriod === value && '✓ '}{label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="control-group">
              <label className="control-label">Statistics</label>
              <div className="map-stats-card">
                <div className="stat-row">
                  <span className="stat-icon">📍</span>
                  <span className="stat-text">{videos.length} video{videos.length !== 1 ? 's' : ''} found</span>
                </div>
                {loading && (
                  <div className="stat-row loading-row">
                    <span className="stat-icon">⏳</span>
                    <span className="stat-text">Loading...</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {error && (
            <div className="map-error-banner">{error}</div>
          )}

          <div className="info-cards-stack">
            <h3 className="info-section-title">How to Use</h3>
            
            <div className="info-card">
              <span className="info-icon">🔍</span>
              <div>
                <div className="info-title">Pan & Zoom</div>
                <div className="info-desc">Move around and zoom to explore different areas</div>
              </div>
            </div>

            <div className="info-card">
              <span className="info-icon">📍</span>
              <div>
                <div className="info-title">Video Markers</div>
                <div className="info-desc">Click on thumbnails to see video details</div>
              </div>
            </div>

            <div className="info-card">
              <span className="info-icon">⏱️</span>
              <div>
                <div className="info-title">Time Filter</div>
                <div className="info-desc">Filter videos by upload date period</div>
              </div>
            </div>

            <div className="info-card">
              <span className="info-icon">⏸️</span>
              <div>
                <div className="info-title">Smart Loading</div>
                <div className="info-desc">Videos load when you stop moving the map</div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};

export default MapView;