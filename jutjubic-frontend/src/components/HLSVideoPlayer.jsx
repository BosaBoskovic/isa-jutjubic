import { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import './HLSVideoPlayer.css';
import { toDate } from "../utils/date";


const HLSVideoPlayer = ({ 
  videoId, 
  isScheduled, 
  scheduledAt, 
  currentStreamOffsetSeconds, 
  videoDurationSeconds,
  isOwner = false
}) => {
  const videoRef = useRef(null);
  const hlsRef = useRef(null);
  const [canPlay, setCanPlay] = useState(false);
  const [timeUntilStart, setTimeUntilStart] = useState('');
  const [error, setError] = useState('');
  const [streamEnded, setStreamEnded] = useState(false);
  
  const streamEndedRef = useRef(false);
  const checkIntervalRef = useRef(null);
  const lastSyncTimeRef = useRef(0); 

console.log("props:", { isScheduled, scheduledAt, isOwner });



  useEffect(() => {
    if (!videoId) return;

    if (isOwner) {
      setCanPlay(true);
      return;
    }

    if (!isScheduled) {
      setCanPlay(true);
      return;
    }

   // const scheduledTime = new Date(scheduledAt);
   const scheduledTime = toDate(scheduledAt);

if (!scheduledTime) {
  setError("Invalid scheduled time");
  setCanPlay(false);
  return;
}


    const now = new Date();

    if (now >= scheduledTime) {
      setCanPlay(true);
    } else {
      setCanPlay(false);

       const diff0 = scheduledTime - new Date();
  if (diff0 > 0) {
    const hours = Math.floor(diff0 / (1000 * 60 * 60));
    const minutes = Math.floor((diff0 % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff0 % (1000 * 60)) / 1000);
    setTimeUntilStart(`${hours}h ${minutes}m ${seconds}s`);
  } else {
    setTimeUntilStart("0h 0m 0s");
  }

      const interval = setInterval(() => {
        const nowCheck = new Date();
        const diff = scheduledTime - nowCheck;

        if (diff <= 0) {
          setCanPlay(true);
          clearInterval(interval);
        } else {
          const hours = Math.floor(diff / (1000 * 60 * 60));
          const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
          const seconds = Math.floor((diff % (1000 * 60)) / 1000);
          setTimeUntilStart(`${hours}h ${minutes}m ${seconds}s`);
        }
      }, 1000);

      return () => clearInterval(interval);
    }
  }, [videoId, isScheduled, scheduledAt, isOwner]);

  useEffect(() => {
    if (!canPlay || !videoId) return;

    const videoElement = videoRef.current;
    if (!videoElement) return;

    const hlsUrl = `http://localhost:8080/api/posts/${videoId}/hls/playlist.m3u8`;
    const isActuallyLive = isScheduled && !isOwner && scheduledAt && videoDurationSeconds && !streamEndedRef.current;
    
    let seekingHandler = null;
    let syncIntervalRef = null;

    const endStreamAndReload = () => {
      if (streamEndedRef.current) return;
      
      console.log('ENDING STREAM!! Reloading as normal video');
      streamEndedRef.current = true;
      setStreamEnded(true);
      
      if (checkIntervalRef.current) {
        clearInterval(checkIntervalRef.current);
        checkIntervalRef.current = null;
      }
      if (syncIntervalRef) {
        clearInterval(syncIntervalRef);
        syncIntervalRef = null;
      }
      
      if (seekingHandler && videoElement) {
        videoElement.removeEventListener('seeking', seekingHandler);
        seekingHandler = null;
      }
      
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      
      videoElement.pause();
      
      setTimeout(() => {
        console.log('Reloading video as normal playback');
        
        if (Hls.isSupported()) {
          const newHls = new Hls({
            debug: false,
            enableWorker: true,
            lowLatencyMode: false,
            backBufferLength: 90,
          });

          newHls.loadSource(hlsUrl);
          newHls.attachMedia(videoElement);

          newHls.on(Hls.Events.MANIFEST_PARSED, function () {
            console.log('Video reloaded!!!! normal playback mode');
            videoElement.controls = true;
            videoElement.loop = false;
            videoElement.currentTime = 0;
          });

          newHls.on(Hls.Events.ERROR, function (event, data) {
            if (data.fatal) {
              console.error('HLS reload error:', data.type);
              newHls.destroy();
            }
          });

          hlsRef.current = newHls;
          
        } else if (videoElement.canPlayType('application/vnd.apple.mpegurl')) {
          videoElement.src = hlsUrl;
          videoElement.controls = true;
          videoElement.loop = false;
          videoElement.currentTime = 0;
          videoElement.load();
        }
      }, 500);
    };

    if (Hls.isSupported()) {
      const hls = new Hls({
        debug: false,
        enableWorker: true,
        lowLatencyMode: false,
        backBufferLength: 90,
      });

      hls.loadSource(hlsUrl);
      hls.attachMedia(videoElement);

      hls.on(Hls.Events.MANIFEST_PARSED, function () {
        console.log('HLS manifest loaded');

        videoElement.controls = true;
        videoElement.loop = false;

        if (isActuallyLive) {
          if (currentStreamOffsetSeconds != null) {
            videoElement.currentTime = currentStreamOffsetSeconds;
            lastSyncTimeRef.current = Date.now();
            console.log(`Initial sync to ${currentStreamOffsetSeconds}s`);
          }

          syncIntervalRef = setInterval(() => {
            if (streamEndedRef.current) {
              clearInterval(syncIntervalRef);
              return;
            }

          //  const scheduledTime = new Date(scheduledAt);
          const scheduledTime = toDate(scheduledAt);
if (!scheduledTime) return; // ili break/continue zavisi gde si

            const now = new Date();
            const secondsSinceStart = Math.floor((now - scheduledTime) / 1000);
            const expectedPosition = secondsSinceStart % videoDurationSeconds;
            const currentPosition = videoElement.currentTime;
            const drift = Math.abs(expectedPosition - currentPosition);

            if (drift > 2) {
              console.log(`Drift detected: ${drift.toFixed(1)}s - resyncing`);
              videoElement.currentTime = expectedPosition;
              lastSyncTimeRef.current = Date.now();
            }
          }, 3000);

          seekingHandler = function preventSeek(e) {
            if (streamEndedRef.current) {
              return; 
            }

            const timeSinceLastSync = Date.now() - lastSyncTimeRef.current;
            if (timeSinceLastSync < 500) {
              return;
            }

            console.log('Manual seeking prevented');
            e.preventDefault();
            
           // const scheduledTime = new Date(scheduledAt);
           const scheduledTime = toDate(scheduledAt);
if (!scheduledTime) return; // ili break/continue zavisi gde si

            const now = new Date();
            const secondsSinceStart = Math.floor((now - scheduledTime) / 1000);
            const expectedPosition = secondsSinceStart % videoDurationSeconds;
            
            videoElement.currentTime = expectedPosition;
            lastSyncTimeRef.current = Date.now();
          };
          
          videoElement.addEventListener('seeking', seekingHandler);

          console.log('Stream will end after', videoDurationSeconds, 'seconds');
          
          checkIntervalRef.current = setInterval(() => {
            if (streamEndedRef.current) {
              clearInterval(checkIntervalRef.current);
              return;
            }

          //  const scheduledTime = new Date(scheduledAt);
          const scheduledTime = toDate(scheduledAt);
if (!scheduledTime) return; // ili break/continue zavisi gde si

            const now = new Date();
            const secondsSinceStart = Math.floor((now - scheduledTime) / 1000);

            if (secondsSinceStart >= videoDurationSeconds + 2) {
              console.log(`Stream ended: ${secondsSinceStart}s / ${videoDurationSeconds}s`);
              endStreamAndReload();
            }
          }, 1000);

          // Auto-play pokušaj
          const playPromise = videoElement.play();
          if (playPromise !== undefined) {
            playPromise.catch(error => {
              console.log('Autoplay prevented:', error.message);
            });
          }

        } else {
          console.log('Regular video mode');
          videoElement.loop = false;
        }
      });

      hls.on(Hls.Events.ERROR, function (event, data) {
        if (data.fatal) {
          console.error(' HLS fatal error:', data.type);
          setError('Failed to load video stream');
          hls.destroy();
        }
      });

      hlsRef.current = hls;

    } else if (videoElement.canPlayType('application/vnd.apple.mpegurl')) {
      videoElement.src = hlsUrl;
      videoElement.controls = true;
      videoElement.loop = false;

      if (isActuallyLive) {
        videoElement.addEventListener('loadedmetadata', function () {
          if (currentStreamOffsetSeconds != null) {
            videoElement.currentTime = currentStreamOffsetSeconds;
            lastSyncTimeRef.current = Date.now();
          }
        });

        syncIntervalRef = setInterval(() => {
          if (streamEndedRef.current) {
            clearInterval(syncIntervalRef);
            return;
          }

         // const scheduledTime = new Date(scheduledAt);
         const scheduledTime = toDate(scheduledAt);
if (!scheduledTime) return; // ili break/continue zavisi gde si

          const now = new Date();
          const secondsSinceStart = Math.floor((now - scheduledTime) / 1000);
          const expectedPosition = secondsSinceStart % videoDurationSeconds;
          const drift = Math.abs(expectedPosition - videoElement.currentTime);

          if (drift > 2) {
            videoElement.currentTime = expectedPosition;
            lastSyncTimeRef.current = Date.now();
          }
        }, 3000);

        seekingHandler = function preventSeek() {
          if (streamEndedRef.current) return;

          const timeSinceLastSync = Date.now() - lastSyncTimeRef.current;
          if (timeSinceLastSync < 500) return;

         // const scheduledTime = new Date(scheduledAt);
         const scheduledTime = toDate(scheduledAt);
if (!scheduledTime) return; // ili break/continue zavisi gde si

          const now = new Date();
          const secondsSinceStart = Math.floor((now - scheduledTime) / 1000);
          const expectedPosition = secondsSinceStart % videoDurationSeconds;
          videoElement.currentTime = expectedPosition;
          lastSyncTimeRef.current = Date.now();
        };
        
        videoElement.addEventListener('seeking', seekingHandler);

        checkIntervalRef.current = setInterval(() => {
          if (streamEndedRef.current) {
            clearInterval(checkIntervalRef.current);
            return;
          }

         // const scheduledTime = new Date(scheduledAt);
         const scheduledTime = toDate(scheduledAt);
if (!scheduledTime) return; // ili break/continue zavisi gde si

          const now = new Date();
          const secondsSinceStart = Math.floor((now - scheduledTime) / 1000);

          if (secondsSinceStart >= videoDurationSeconds + 2) {
            endStreamAndReload();
          }
        }, 1000);
      }
    } else {
      setError('HLS is not supported in this browser');
    }

    return () => {
      if (checkIntervalRef.current) {
        clearInterval(checkIntervalRef.current);
      }
      if (syncIntervalRef) {
        clearInterval(syncIntervalRef);
      }
      
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      
      if (videoElement && seekingHandler) {
        videoElement.removeEventListener('seeking', seekingHandler);
      }
    };
  }, [canPlay, videoId, isScheduled, currentStreamOffsetSeconds, isOwner, scheduledAt, videoDurationSeconds]);

  if (error) {
    return (
      <div className="hls-player-error">
        <p>⚠️ {error}</p>
      </div>
    );
  }

  if (!canPlay) {
    return (
      <div className="hls-player-scheduled">
        <div className="scheduled-overlay">
          <div className="scheduled-icon">🕐</div>
          <h3>Scheduled Stream</h3>
          <p>This video will start in:</p>
          <div className="countdown">{timeUntilStart}</div>
          <p className="scheduled-time">
            Starts at: {toDate(scheduledAt)?.toLocaleString() ?? "—"}

          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="hls-player-container">
      {isScheduled && !isOwner && !streamEnded && (
        <div className="live-indicator">
          <span className="live-dot"></span>
          LIVE
        </div>
      )}
      
      {streamEnded && (
        <div className="stream-ended-badge">
          ✓ Stream Ended - Replay Available
        </div>
      )}
      
      <video
        ref={videoRef}
        className="hls-video"
        controls
        playsInline
      />
    </div>
  );
};

export default HLSVideoPlayer;