// LiveChat.jsx
import { useEffect, useMemo, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import "./LiveChat.css";
import { toDate } from "../utils/date";

const WS_BASE = "http://localhost:8080";

function computePhase({ scheduled, scheduledAt, videoDurationSeconds, nowMs }) {
  // none = nije streaming video
  if (!scheduled) return "none";

  // ako nemamo podatke, tretiraj kao "waiting" (da chat radi)
  if (!scheduledAt || !videoDurationSeconds) return "waiting";

  const startDate = toDate(scheduledAt);
  if (!startDate) return "waiting";

  const start = startDate.getTime();
  const end = start + Number(videoDurationSeconds) * 1000;

  if (nowMs < start) return "waiting";
  if (nowMs <= end + 2000) return "live"; // +2s buffer
  return "ended";
}

export default function LiveChat({ video, isLoggedIn }) {
  const token = localStorage.getItem("token");
  const videoId = video?.id;

  // chat samo za scheduled/streaming video
  if (!video?.scheduled) return null;

  // ✅ TICK: re-render svake sekunde da phase pređe u "ended" bez refresh-a
  const [nowMs, setNowMs] = useState(Date.now());
  useEffect(() => {
    const id = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  const phase = useMemo(
    () =>
      computePhase({
        scheduled: !!video?.scheduled,
        scheduledAt: video?.scheduledAt,
        videoDurationSeconds: video?.videoDurationSeconds,
        nowMs,
      }),
    [video?.scheduled, video?.scheduledAt, video?.videoDurationSeconds, nowMs]
  );

  const chatEnabled = phase === "waiting" || phase === "live"; // radi i dok odbrojava
  const chatClosed = phase === "ended"; // posle kraja gasimo

  const [connected, setConnected] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");

  const clientRef = useRef(null);
  const bottomRef = useRef(null);

  // auto-scroll na dno
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // ✅ kad se zavrsi live -> ugasi chat + obrisi poruke (nema istorije)
  useEffect(() => {
    if (!chatClosed) return;

    try {
      clientRef.current?._sub?.unsubscribe?.();
    } catch {}
    try {
      clientRef.current?.deactivate?.();
    } catch {}

    clientRef.current = null;
    setConnecting(false);
    setConnected(false);
    setMessages([]);

    // ✅ i odmah ukloni ceo chat iz UI (bez refresh-a)
    // (return null dole će odraditi)
  }, [chatClosed]);

  // ✅ connect samo dok chat treba da radi (waiting ili live) + ulogovan
  useEffect(() => {
    if (!videoId || !isLoggedIn || !token) return;
    if (!chatEnabled) return;

    const wsUrl = `${WS_BASE}/ws?token=${encodeURIComponent(token)}`;

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        setConnecting(false);
        setConnected(true);

        const sub = client.subscribe(`/topic/chat.${videoId}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body);
            setMessages((prev) => {
              const next = [...prev, payload];
              if (next.length > 200) next.shift();
              return next;
            });
          } catch {}
        });

        clientRef.current._sub = sub;
      },

      onDisconnect: () => {
        setConnecting(false);
        setConnected(false);
      },
      onStompError: () => {
        setConnecting(false);
        setConnected(false);
      },
      onWebSocketClose: () => {
        setConnecting(false);
        setConnected(false);
      },
    });

    setConnecting(true);
    client.activate();
    clientRef.current = client;

    return () => {
      try {
        clientRef.current?._sub?.unsubscribe();
      } catch {}
      client.deactivate();
      clientRef.current = null;
      setConnecting(false);
      setConnected(false);
    };
  }, [videoId, isLoggedIn, token, chatEnabled]);

  const send = () => {
    if (!connected || !clientRef.current) return;
    if (!chatEnabled) return; // ne salji posle kraja

    const msg = text.trim();
    if (!msg) return;

    // ✅ odmah prikaži lokalno (bez čekanja servera)
    setMessages((prev) => [
      ...prev,
      { sender: "me", message: msg, timestamp: new Date().toISOString() },
    ]);

    clientRef.current.publish({
      destination: "/app/chat.send",
      body: JSON.stringify({ videoId, message: msg }),
    });

    setText("");
  };

  // ✅ čim završi stream — chat nestane (bez refresh)
  if (chatClosed) return null;

  const statusText = connected
    ? phase === "live"
      ? "Connected"
      : "Connected (waiting…)"
    : !isLoggedIn
      ? "Login required"
      : connecting
        ? "Connecting…"
        : "Disconnected";

  return (
    <div className="livechat">
      <div className="livechat-header">
        <div className="livechat-title">💬 Live Chat</div>
        <div className={`livechat-status ${connected ? "ok" : "bad"}`}>
          {statusText}
        </div>
      </div>

      {phase === "waiting" && (
        <div className="livechat-info">
          Stream hasn’t started yet — you can chat while waiting.
        </div>
      )}

      {/* chat radi i u waiting i u live */}
      {chatEnabled && (
        <>
          <div className="livechat-messages">
            {messages.map((m, idx) => (
              <div key={idx} className="livechat-msg">
                <div className="livechat-meta">
                  <b>{m.sender ?? "user"}</b>
                  <span>
                    {m.timestamp ? new Date(m.timestamp).toLocaleTimeString() : ""}
                  </span>
                </div>
                <div className="livechat-text">{m.message}</div>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>

          <div className="livechat-input">
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder={connected ? "Type a message…" : "Not connected"}
              maxLength={500}
              disabled={!connected}
              onKeyDown={(e) => {
                if (e.key === "Enter") send();
              }}
            />
            <button onClick={send} disabled={!connected || !text.trim()}>
              Send
            </button>
          </div>
        </>
      )}
    </div>
  );
}
