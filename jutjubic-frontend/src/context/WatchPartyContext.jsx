import { createContext, useContext, useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const WatchPartyContext = createContext(null);

export const useWatchParty = () => {
  const context = useContext(WatchPartyContext);
  if (!context) {
    throw new Error('useWatchParty must be used within WatchPartyProvider');
  }
  return context;
};

export const WatchPartyProvider = ({ children }) => {
  const [stompClient, setStompClient] = useState(null);
  const [currentRoom, setCurrentRoom] = useState(null);
  const [connected, setConnected] = useState(false);
  const navigate = useNavigate();
  const subscriptionRef = useRef(null);

  useEffect(() => {
    const socket = new SockJS('http://localhost:8080/ws');
    
    const client = new Client({
      webSocketFactory: () => socket,
      
      onConnect: () => {
        console.log('✅ WebSocket connected');
        setConnected(true);
        setStompClient(client);
      },
      
      onDisconnect: () => {
        console.log('❌ WebSocket disconnected');
        setConnected(false);
      },
      
      onStompError: (frame) => {
        console.error('❌ WebSocket error:', frame);
      },
    });

    client.activate();

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
      client.deactivate();
    };
  }, []);

  const joinRoom = (roomId) => {
    if (!stompClient || !connected) {
      console.error('WebSocket not connected');
      return;
    }

    // Unsubscribe from previous room
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
    }

    // Subscribe to new room
    subscriptionRef.current = stompClient.subscribe(
      `/topic/watchparty/${roomId}`,
      (message) => {
        const data = JSON.parse(message.body);
        console.log('📺 Received video:', data);
        
        // Automatski otvori video
        navigate(`/videos/${data.videoId}`);
      }
    );

    setCurrentRoom(roomId);
    console.log(`✅ Joined room: ${roomId}`);
  };

  const leaveRoom = () => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }
    setCurrentRoom(null);
    console.log('👋 Left room');
  };

  const playVideo = (videoId) => {
    if (!stompClient || !currentRoom || !connected) {
      console.error('Cannot play video: not connected or no room');
      return;
    }

    stompClient.publish({
      destination: `/app/watchparty/${currentRoom}/play`,
      body: JSON.stringify({ videoId, roomId: currentRoom }),
    });

    console.log(`▶️ Playing video ${videoId} in room ${currentRoom}`);
  };

  const value = {
    currentRoom,
    connected,
    joinRoom,
    leaveRoom,
    playVideo,
  };

  return (
    <WatchPartyContext.Provider value={value}>
      {children}
    </WatchPartyContext.Provider>
  );
};