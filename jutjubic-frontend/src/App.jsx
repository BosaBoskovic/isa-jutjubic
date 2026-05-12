import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { WatchPartyProvider } from './context/WatchPartyContext';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import VerifyEmail from './pages/VerifyEmail';
import PrivateRoute from './components/PrivateRoute';
import UploadVideo from './pages/UploadVideo';
import WatchVideos from './pages/WatchVideos';
import MyVideos from './pages/MyVideos';
import VideoDetails from './pages/VideoDetails';
import MyVideoDetails from './pages/MyVideoDetails';
import UserProfile from './pages/UserProfile'; 
import MapView from './pages/MapView';
import CreateWatchParty from './pages/CreateWatchParty';
import JoinWatchParty from './pages/JoinWatchParty';
import WatchPartyRoom from './pages/WatchPartyRoom';

function App() {
  return (
    <AuthProvider>
      <Router>
        <WatchPartyProvider>
        
          <div className="app">
            <Navbar />
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/verify" element={<VerifyEmail />} />
              <Route path="/create-watch-party" element={
                                                      <PrivateRoute>
                                                        <CreateWatchParty />
                                                      </PrivateRoute>
                                                    } />
              <Route path="/join-watch-party" element={<JoinWatchParty />} />

              <Route path="/watch-party/:inviteCode" element={
                                                      <PrivateRoute>
                                                        <WatchPartyRoom />
                                                      </PrivateRoute>
                                                    } />                                      
              <Route path="/upload" element={
              <PrivateRoute>
                <UploadVideo />
              </PrivateRoute>
            }
            
          />

          <Route path="/watch" element={<WatchVideos />} />
          <Route path="/users/:username" element={<UserProfile />} />

              <Route
                path="/my-videos"
                element={
                  <PrivateRoute>
                    <MyVideos />
                  </PrivateRoute>
                }
              />

              <Route path="/videos/:id" element={<VideoDetails />} />

              <Route
    path="/my-videos/:id"
    element={
      <PrivateRoute>
        <MyVideoDetails />
      </PrivateRoute>
    }
  />
              <Route path="/map" element={<MapView />} />
              {/* Primer zaštićene rute - dodaj kasnije kada budeš pravila nove stranice */}
              {/* 
              <Route path="/profile" element={
                <PrivateRoute>
                  <Profile />
                </PrivateRoute>
              } /> 
              */}
            </Routes>
          </div>
        
        </WatchPartyProvider>
      </Router>
    </AuthProvider>
  );
}

export default App;