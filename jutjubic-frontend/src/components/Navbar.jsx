import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);

  const handleLogout = () => { logout(); navigate('/login'); };

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <nav className="navbar">
      <div className="navbar-container">

        <Link to="/" className="navbar-logo">
          ▶ SaBoNa
        </Link>

        <div className="navbar-right" ref={menuRef}>
          <button className="hamburger" onClick={() => setOpen((prev) => !prev)}>
            ☰
          </button>

          {open && (
            <div className="hamburger-menu">
              <Link to="/watch" onClick={() => setOpen(false)}>
                Watch videos
              </Link>

              <Link to="/map" onClick={() => setOpen(false)}>
                Explore Map
              </Link>

              {isAuthenticated && (
                <Link to="/create-watch-party" onClick={() => setOpen(false)}>
                  🎬 Create Watch Party
                </Link>
              )}

              <Link to="/join-watch-party" onClick={() => setOpen(false)}>
                🚪 Join Watch Party
              </Link>

              {isAuthenticated && (
                <Link to="/my-videos" onClick={() => setOpen(false)}>
                  My videos
                </Link>
              )}

              {isAuthenticated && (
                <Link to="/upload" onClick={() => setOpen(false)}>
                  Upload video
                </Link>
              )}

              <hr />

              {isAuthenticated ? (
                <>
                  <span className="menu-user">
                    👋 {user?.username}
                  </span>

                  <button
                    className="menu-logout"
                    onClick={() => { setOpen(false); handleLogout(); }}
                  >
                    Log out
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login" onClick={() => setOpen(false)}>
                    Log in
                  </Link>
                  <Link to="/register" onClick={() => setOpen(false)}>
                    Register
                  </Link>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
