import { useAuth } from '../context/AuthContext';
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import './Home.css';

const Home = () => {
  const { isAuthenticated, user } = useAuth();
  const [popularVideos, setPopularVideos] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchPopularVideos = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/popular/top3');
      setPopularVideos(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching popular videos:', error);
      setLoading(false);
    }
  };

  
  useEffect(() => {
    if (isAuthenticated) {
      fetchPopularVideos();
    }
  }, [isAuthenticated]);

  if (isAuthenticated) {
    return (
      <div className="home-container">
        <div className="welcome-card">
          <h1>Welcome back, {user?.username}! 🎉</h1>
          <p className="welcome-text">
            Your account is active and ready to use.
          </p>
           

          {!loading && popularVideos.length > 0 && (
            <div className="popular-videos-section">
              <h2 className="popular-title">🔥 Trending This Week</h2>
              <div className="popular-videos-grid">
                {popularVideos.map((video, index) => (
                  <Link 
                    key={video.videoId} 
                    to={`/videos/${video.videoId}`}
                    className="popular-video-card"
                  >
                    <div className="popular-rank">#{index + 1}</div>
                    <img 
                      src={`http://localhost:8080/api/posts/${video.videoId}/thumbnail`}
                      alt={video.title}
                      className="popular-thumbnail"
                    />
                    <div className="popular-info">
                      <h3>{video.title}</h3>
                      <p className="popular-author">by {video.authorUsername}</p>
                      <div className="popular-stats">
                        <span>👁 {video.viewsLast7Days} views</span>
                        <span className="popularity-score">
                          Score: {Math.round(video.popularityScore)}
                        </span>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          )}
           

          <div className="home-features">
            <Link to="/watch" style={{ textDecoration: 'none' }}>
            <div className="feature-card">
              <span className="feature-icon">▶</span>
              <h3>Watch Videos</h3>
              <p>Explore thousands of video content</p>
            </div>
            </Link>

            <Link to="/map" style={{ textDecoration: 'none' }}>
              <div className="feature-card">
              <span className="feature-icon map-icon">⌖</span>
                <h3>Explore Map</h3>
                <p>Discover videos by location</p>
              </div>
            </Link>

            <Link to="/my-videos" style={{ textDecoration: 'none' }}>
            <div className="feature-card">
              <span className="feature-icon">🎞</span>
              <h3>My Videos</h3>
              <p>See videos you uploaded</p>
            </div>
          </Link>

            <Link to="/upload" style={{ textDecoration: 'none' }}>
              <div className="feature-card">
                <span className="feature-icon">⬆</span>
                <h3>Upload Videos</h3>
                <p>Share your content with the world</p>
              </div>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="home-container">
      <div className="hero-section">
        <h1 className="hero-title">
          Welcome to <span className="brand-name">SaBoNa</span>
        </h1>
        <p className="hero-subtitle">
          A platform for sharing video content
        </p>
        
        <div className="cta-buttons">
          <Link to="/register">
            <button className="btn-primary">Start for Free</button>
          </Link>
          <Link to="/login">
            <button className="btn-secondary">Log in</button>
          </Link>
        </div>

        <div className="features-grid">
          <Link to="/watch" style={{ textDecoration: 'none' }}>
          <div className="feature-item">
            <div className="feature-icon-large">🎬</div>
            <h3>Free Video Hosting</h3>
            <p>Upload and share your videos</p>
          </div>
          </Link>
          <div className="feature-item">
            <div className="feature-icon-large">👥</div>
            <h3>Community</h3>
            <p>Connect with other users</p>
          </div>
          <div className="feature-item">
            <div className="feature-icon-large">🔒</div>
            <h3>Security</h3>
            <p>Your data is safe</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;