import { useMemo, useState } from 'react';
import { postsAPI } from '../api/api';
import './UploadVideo.css';
import MapPicker from '../components/MapPicker';
import { useNavigate } from 'react-router-dom';

const MAX_VIDEO_MB = 200;
const MAX_THUMBNAIL_MB = 3;
const MAX_VIDEO_SECONDS = 30;

const mb = (bytes) => Math.round((bytes / (1024 * 1024)) * 100) / 100;
const isNumber = (v) => v !== '' && v !== null && !Number.isNaN(Number(v));

const UploadVideo = () => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [tagsText, setTagsText] = useState(''); 
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');

  const [thumbnail, setThumbnail] = useState(null);
  const [video, setVideo] = useState(null);

  const [saving, setSaving] = useState(false);
  const [progress, setProgress] = useState(0);
  const [errors, setErrors] = useState([]);

  const [picked, setPicked] = useState(null); 

  const [locationQuery, setLocationQuery] = useState('');
  const [locationResults, setLocationResults] = useState([]);
  const [searching, setSearching] = useState(false);
  
  // Scheduled video state
  const [isScheduled, setIsScheduled] = useState(false);
  const [scheduledDate, setScheduledDate] = useState('');
  const [scheduledTime, setScheduledTime] = useState('');
  
  const navigate = useNavigate();


  const tags = useMemo(() => {
    return tagsText
      .split(',')
      .map((t) => t.trim())
      .filter(Boolean);
  }, [tagsText]);

  const thumbPreview = useMemo(() => {
    if (!thumbnail) return null;
    return URL.createObjectURL(thumbnail);
  }, [thumbnail]);

  const validate = () => {
    const errs = [];

    if (!title.trim()) errs.push('Title is required.');
    if (title.length > 200) errs.push('Title must be at most 200 characters.');

    if (!description.trim()) errs.push('Description is required.');
    if (description.length > 2000) errs.push('Description must be at most 2000 characters.');

    if (tags.length === 0) errs.push('Please add at least one tag.');

    if (!thumbnail) errs.push('Thumbnail image is required.');
    if (thumbnail && !thumbnail.type.startsWith('image/')) errs.push('Thumbnail must be an image file.');
    if (thumbnail && mb(thumbnail.size) > MAX_THUMBNAIL_MB) {
      errs.push(`Thumbnail is too large. Max size is ${MAX_THUMBNAIL_MB}MB.`);
    }

    if (!video) errs.push('Video file is required.');
    if (video && video.type !== 'video/mp4') errs.push('Video must be an MP4 file.');
    if (video && mb(video.size) > MAX_VIDEO_MB) {
      errs.push(`Video is too large. Max size is ${MAX_VIDEO_MB}MB.`);
    }

    const latFilled = latitude !== '';
    const lonFilled = longitude !== '';

    if ((latFilled && !lonFilled) || (!latFilled && lonFilled)) {
      errs.push('If you provide a location, both latitude and longitude are required.');
    }

    if (latFilled) {
      if (!isNumber(latitude)) errs.push('Latitude must be a valid number.');
      else {
        const lat = Number(latitude);
        if (lat < -90 || lat > 90) errs.push('Latitude must be between -90 and 90.');
      }
    }

    if (lonFilled) {
      if (!isNumber(longitude)) errs.push('Longitude must be a valid number.');
      else {
        const lon = Number(longitude);
        if (lon < -180 || lon > 180) errs.push('Longitude must be between -180 and 180.');
      }
    }
    
    // Validate scheduled time
    if (isScheduled) {
      if (!scheduledDate) errs.push('Scheduled date is required.');
      if (!scheduledTime) errs.push('Scheduled time is required.');
      
      if (scheduledDate && scheduledTime) {
        // Create date in LOCAL timezone (not UTC)
        const scheduledDateTime = new Date(`${scheduledDate}T${scheduledTime}`);
        const now = new Date();
        
        // Add 1 minute buffer for processing time
        const oneMinuteFromNow = new Date(now.getTime() + 60000);
        
        if (scheduledDateTime <= oneMinuteFromNow) {
          errs.push('Scheduled time must be at least 1 minute in the future.');
        }
      }
    }

    return errs;
  };

  const handlePick = (latlng) => {
    setPicked(latlng);
    setLatitude(latlng.lat.toFixed(6));
    setLongitude(latlng.lng.toFixed(6));
    setErrors([]);
  };

  const handleUseMyLocation = () => {
    if (!navigator.geolocation) {
      setErrors(['Geolocation is not supported in this browser.']);
      return;
    }

    setErrors([]);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        setPicked({ lat, lng });
        setLatitude(lat.toFixed(6));
        setLongitude(lng.toFixed(6));
        setErrors([]);
      },
      (err) => {
        switch (err.code) {
          case err.PERMISSION_DENIED:
            setErrors(['Location permission was denied in the browser.']);
            break;
          case err.POSITION_UNAVAILABLE:
            setErrors(['Your location is currently unavailable.']);
            break;
          case err.TIMEOUT:
            setErrors(['Timed out while trying to get your location.']);
            break;
          default:
            setErrors([
              'Failed to get your current location. You can click on the map or enter coordinates manually.',
            ]);
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      }
    );
  };

  const getVideoDurationSeconds = (file) => {
    return new Promise((resolve, reject) => {
      try {
        const url = URL.createObjectURL(file);
        const videoEl = document.createElement('video');
        videoEl.preload = 'metadata';
        videoEl.src = url;

        videoEl.onloadedmetadata = () => {
          URL.revokeObjectURL(url);
          resolve(videoEl.duration);
        };

        videoEl.onerror = () => {
          URL.revokeObjectURL(url);
          reject(new Error('Failed to read video metadata.'));
        };
      } catch (e) {
        reject(e);
      }
    });
  };

  const searchLocation = async () => {
    const q = locationQuery.trim();
    if (!q) {
      setErrors(['Please enter a city or country to search.']);
      return;
    }

    setErrors([]);
    setSearching(true);

    try {
      const url =
        `https://nominatim.openstreetmap.org/search?` +
        new URLSearchParams({
          q,
          format: 'json',
          addressdetails: '1',
          limit: '5',
        });

      const res = await fetch(url, {
        headers: { Accept: 'application/json' },
      });

      if (!res.ok) throw new Error('Location search failed.');

      const data = await res.json();
      const mapped = data.map((x) => ({
        displayName: x.display_name,
        lat: Number(x.lat),
        lng: Number(x.lon),
      }));

      setLocationResults(mapped);

      if (mapped.length === 0) {
        setErrors(['No locations found. Try a different query.']);
      }
    } catch (e) {
      setErrors(['Could not search locations right now. Please try again later.']);
    } finally {
      setSearching(false);
    }
  };

  const applyLocation = ({ lat, lng }) => {
    setPicked({ lat, lng });
    setLatitude(lat.toFixed(6));
    setLongitude(lng.toFixed(6));
    setLocationResults([]);
    setErrors([]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrors([]);

    const errs = validate();
    if (errs.length) {
      setErrors(errs);
      return;
    }

    if (video) {
      try {
        const duration = await getVideoDurationSeconds(video);
        if (Number.isFinite(duration) && duration > MAX_VIDEO_SECONDS) {
          setErrors([`Video is too long. Max duration is ${MAX_VIDEO_SECONDS} seconds.`]);
          return;
        }
      } catch {
        setErrors(['Could not read video duration. Please try another MP4 file.']);
        return;
      }
    }

    const metadata = {
      title: title.trim(),
      description: description.trim(),
      tags: tags,
    };

    if (latitude !== '' && longitude !== '') {
      metadata.latitude = Number(latitude);
      metadata.longitude = Number(longitude);
    }
    
    // Add scheduled time if enabled
    if (isScheduled && scheduledDate && scheduledTime) {
      // Create date in LOCAL timezone
      const scheduledDateTime = new Date(`${scheduledDate}T${scheduledTime}`);
      
      // Format for backend: YYYY-MM-DDTHH:mm:ss (without timezone, backend will treat as local)
      const year = scheduledDateTime.getFullYear();
      const month = String(scheduledDateTime.getMonth() + 1).padStart(2, '0');
      const day = String(scheduledDateTime.getDate()).padStart(2, '0');
      const hours = String(scheduledDateTime.getHours()).padStart(2, '0');
      const minutes = String(scheduledDateTime.getMinutes()).padStart(2, '0');
      const seconds = String(scheduledDateTime.getSeconds()).padStart(2, '0');
      
      metadata.scheduledAt = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
      
      console.log('Scheduled time (local):', metadata.scheduledAt);
      console.log('Current time:', new Date().toISOString());
    }

    setSaving(true);
    setProgress(0);

    try {
      const createdId = await postsAPI.createVideoPost(metadata, thumbnail, video, {
        timeout: 30000,
        onUploadProgress: (evt) => {
          if (!evt.total) return;
          setProgress(Math.round((evt.loaded * 100) / evt.total));
        },
      });

      setTitle('');
      setDescription('');
      setTagsText('');
      setLatitude('');
      setLongitude('');
      setPicked(null);
      setIsScheduled(false);
      setScheduledDate('');
      setScheduledTime('');

      setThumbnail(null);
      setVideo(null);
      setProgress(0);

      setLocationQuery('');
      setLocationResults([]);
      setErrors([]);

      navigate('/my-videos');
    } catch (err) {
      const msg =
        err.response?.data?.error ||
        err.response?.data?.message ||
        err.message ||
        'Error creating post.';
      setErrors([msg]);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-card">
        <h2>Upload video</h2>
        <p className="upload-subtitle">Create a video post</p>

        <form className="upload-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Title</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
              disabled={saving}
              placeholder="Enter the title of the video post (max 200)"
            />
          </div>

          <div className="form-group">
            <label>Video description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              maxLength={2000}
              disabled={saving}
              placeholder="Enter a description (max 2000)"
            />
          </div>

          <div className="form-group">
            <label>Tags</label>
            <input
              value={tagsText}
              onChange={(e) => setTagsText(e.target.value)}
              disabled={saving}
              placeholder="travel, food, vlog..."
            />
            {tags.length > 0 && (
              <div className="tags-preview">
                {tags.map((t) => (
                  <span key={t} className="tag-chip">
                    {t}
                  </span>
                ))}
              </div>
            )}
          </div>
          
          {/* Scheduled Video Section */}
          <div className="form-group scheduled-section">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={isScheduled}
                onChange={(e) => setIsScheduled(e.target.checked)}
                disabled={saving}
              />
              <span>📅 Schedule this video for later</span>
            </label>
            
            {isScheduled && (
              <div className="scheduled-inputs">
                <div className="form-row-2">
                  <div className="form-group">
                    <label>Date</label>
                    <input
                      type="date"
                      value={scheduledDate}
                      onChange={(e) => setScheduledDate(e.target.value)}
                      disabled={saving}
                      min={new Date().toISOString().split('T')[0]}
                    />
                  </div>
                  <div className="form-group">
                    <label>Time</label>
                    <input
                      type="time"
                      value={scheduledTime}
                      onChange={(e) => setScheduledTime(e.target.value)}
                      disabled={saving}
                    />
                  </div>
                </div>
                <p className="schedule-hint">
                  ⏰ Video will go live at the scheduled time and all viewers will watch it synchronized
                </p>
              </div>
            )}
          </div>

          <div className="form-group">
            <label>Location (optional)</label>

            <div className="location-search">
              <input
                value={locationQuery}
                onChange={(e) => setLocationQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    searchLocation();
                  }
                }}
                placeholder="Search city/country (e.g., Maldives, Paris, Novi Sad)"
                disabled={saving || searching}
              />
              <button
                type="button"
                className="btn-secondary-small"
                onClick={searchLocation}
                disabled={saving || searching}
              >
                {searching ? 'Searching...' : 'Search'}
              </button>
            </div>

            {locationResults.length > 0 && (
              <div className="location-results">
                {locationResults.map((r) => (
                  <button
                    type="button"
                    key={r.displayName}
                    className="location-result-item"
                    onClick={() => applyLocation(r)}
                  >
                    {r.displayName}
                  </button>
                ))}
              </div>
            )}

            <MapPicker value={picked} onChange={handlePick} />

            <div className="location-buttons">
              <button
                type="button"
                className="btn-secondary-small"
                onClick={handleUseMyLocation}
                disabled={saving}
              >
                📍 Use my current location
              </button>

              <button
                type="button"
                className="btn-secondary-small"
                onClick={() => {
                  setPicked(null);
                  setLatitude('');
                  setLongitude('');
                  setLocationQuery('');
                  setLocationResults([]);
                  setErrors([]);
                }}
                disabled={saving}
              >
                ❌ Remove location
              </button>
            </div>
          </div>

          <div className="form-row-2">
            <div className="form-group">
              <label>Latitude</label>
              <input
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                disabled={saving}
                placeholder="45.267"
              />
            </div>
            <div className="form-group">
              <label>Longitude</label>
              <input
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                disabled={saving}
                placeholder="19.833"
              />
            </div>
          </div>

          <div className="form-row-2">
            <div className="form-group">
              <label>Thumbnail (photo)</label>
              <input
                type="file"
                accept="image/*"
                disabled={saving}
                onChange={(e) => setThumbnail(e.target.files?.[0] ?? null)}
              />
              {thumbPreview && <img className="thumb-preview" src={thumbPreview} alt="preview" />}
            </div>

            <div className="form-group">
              <label>Video (MP4, max 200MB)</label>
              <input
                type="file"
                accept="video/mp4"
                disabled={saving}
                onChange={(e) => setVideo(e.target.files?.[0] ?? null)}
              />
              {video && (
                <div className="file-info">
                  <div>
                    <b>{video.name}</b>
                  </div>
                  <div>{mb(video.size)} MB</div>
                </div>
              )}
            </div>
          </div>

          {saving && (
            <div className="progress-wrap">
              <div className="progress-bar">
                <div className="progress-fill" style={{ width: `${progress}%` }} />
              </div>
              <div className="progress-text">{progress}%</div>
            </div>
          )}

          {errors.length > 0 && (
            <div className="error-message submit-error">
              <ul style={{ margin: 0, paddingLeft: '1.2rem' }}>
                {errors.map((e) => (
                  <li key={e}>{e}</li>
                ))}
              </ul>
            </div>
          )}

          <button className="btn-submit" type="submit" disabled={saving}>
            {saving ? 'Uploading...' : 'Create post'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default UploadVideo;