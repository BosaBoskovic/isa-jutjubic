import { useNavigate } from 'react-router-dom';
import './Videos.css';
import { toDate } from '../utils/date';


const VideoCard = ({ video, isMine = false, showMeta = true  }) => {
  const navigate = useNavigate();

   const d = toDate(video.createdAt);


  const open = () => {
    navigate(isMine ? `/my-videos/${video.id}` : `/videos/${video.id}`);
  };

  return (
    <div className="video-card">
      <div className="video-thumb-wrap" onClick={open} role="button" tabIndex={0}>
        <img
          className="video-thumb"
          src={`http://localhost:8080/api/posts/${video.id}/thumbnail`}
          alt={video.title}
        />
        <div className="thumb-overlay">
          <span className="thumb-play">▶</span>
        </div>
      </div>

      <div className="video-card-body">
        <h4 className="video-card-title">{video.title}</h4>

        {showMeta && (
          <div className="video-card-meta">
            {!isMine && (
              <span
                className="video-card-author clickable-user"
                onClick={() => navigate(`/users/${video.authorUsername}`)}
                style={{ cursor: 'pointer', color: '#1e90ff' }}
                title={`View ${video.authorName}'s profile`}
              >
                {video.authorName ?? 'Unknown'}
              </span>
            )}
           <span className="video-card-date">
  {d ? d.toLocaleDateString() : ''}
</span>

            <span className="video-card-likes">👍 {video.likesCount ?? 0}</span>
            <span className="video-card-views">👁 {video.viewsCount ?? 0}</span>
          </div>
        )}

        <button className="video-card-btn" type="button" onClick={open}>
          ▶ Watch
        </button>
      </div>
    </div>
  );
};

export default VideoCard;
