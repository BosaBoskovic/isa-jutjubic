import { useEffect, useState } from 'react';
import { commentsAPI } from '../api/api';
import { useNavigate } from 'react-router-dom';
import { toDate } from '../utils/date';


const CommentsSection = ({ postId, isLoggedIn }) => {
  const [comments, setComments] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    loadComments(0, true);
    // eslint-disable-next-line
  }, [postId]);

  const navigate = useNavigate();

  const loadComments = async (p, reset = false) => {
    if (loading) return;
    setLoading(true);

    try {
      const data = await commentsAPI.getComments(postId, p, 10);
      setComments(prev =>
        reset ? data.content : [...prev, ...data.content]
      );
      setHasMore(!data.last);
      setPage(p);
    } catch (e) {
      setError('Failed to load comments');
    } finally {
      setLoading(false);
    }
  };

  const submitComment = async () => {
    if (!text.trim()) return;

    try {
      const newComment = await commentsAPI.addComment(postId, text);
      setComments(prev => [newComment, ...prev]); // optimistic
      setText('');
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to add comment');
    }
  };

  return (
    <div id ="comments-anchor" className="comments-section">
      <h3>Comments</h3>

      {error && <div className="videos-error">{error}</div>}

      {isLoggedIn && (
        <div className="comment-form">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="Write a comment…"
            maxLength={1000}
          />
          <button onClick={submitComment}>Post</button>
        </div>
      )}

      {!isLoggedIn && (
        <div className="videos-info">
          You must be logged in to comment.
        </div>
      )}

      <div className="comment-list">
        {comments.map(c => (
          <div key={c.id} className="comment-item">
            <b
              className="clickable-user"
              onClick={() => navigate(`/users/${c.authorUsername}`)}
              style={{ cursor: 'pointer', color: '#1e90ff' }}
              title={`View ${c.authorUsername}'s profile`}
            >
              {c.authorUsername}
            </b>

           <span>
  {toDate(c.createdAt)?.toLocaleString() ?? ''}
</span>

            <p>{c.text}</p>
          </div>
        ))}
      </div>

      {hasMore && !loading && (
      <button className="load-more-btn" onClick={() => loadComments(page + 1)}>
        Load more comments
      </button>
    )}

      {loading && <div className="videos-info">Loading…</div>}
    </div>
  );
};

export default CommentsSection;
