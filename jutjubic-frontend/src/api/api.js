import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type':'application/json',
  },
})

//Add token to requests if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if(token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth API
export const authAPI = {
  register: async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },
  
  login: async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    return response.data;
  },
  
  verifyEmail: async (token) => {
    const response = await api.get(`/auth/verify?token=${token}`);
    return response.data;
  },
};

export default api;

export const testBackend = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/test");
      const text = await response.text();
      console.log(text);
      return text;
    } catch (error) {
      console.error("Greška pri pozivu backend-a:", error);
    }
  };
  //UserAPI
  export const usersAPI = {
    getPublicProfile: async (username) => {
      // username jer backend rutu koristi /users/{username}/public
      const res = await api.get(`/users/${username}/public`);
      return res.data; // backend vraća javne podatke + videe
    },
  };


  export const postsAPI = {
  createVideoPost: async (metadataObj, thumbnailFile, videoFile, options = {}) => {
    const fd = new FormData();

    // backend traži @RequestPart("metadata") kao JSON
    fd.append(
      'metadata',
      new Blob([JSON.stringify(metadataObj)], { type: 'application/json' })
    );

    fd.append('thumbnail', thumbnailFile);
    fd.append('video', videoFile);

    const response = await api.post('/posts', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: options.timeout ?? 30000, // 30s (backend timeout je 15s, pa je ok da bude malo veći)
      onUploadProgress: options.onUploadProgress,
    });

    return response.data; // backend vraća ID
  },


  getAll: async () => {
    const res = await api.get('/posts');
    return res.data;
  },

  // ✅ My Videos
  getMine: async () => {
    const res = await api.get('/posts/mine');
    return res.data;
  },

  // ✅ Details
  getById: async (id) => {
    const res = await api.get(`/posts/${id}`);
    return res.data;
  },

    toggleLike: async (postId) => {
    const res = await api.post(`/posts/${postId}/like`);
    return res.data; // { likesCount, likedByMe }
  },
  
  checkAvailability: (id) =>
  api.get(`/posts/${id}/availability`).then((res) => res.data),

};

export const commentsAPI = {
  getComments: async (postId, page = 0, size = 10) => {
    const res = await api.get(`/posts/${postId}/comments`, {
      params: { page, size },
    });
    return res.data; // Spring Page<CommentDto>
  },

  addComment: async (postId, text) => {
    const res = await api.post(`/posts/${postId}/comments`, { text });
    return res.data; // CommentDto
  },
};

export const watchPartyAPI = {
  // Kreiranje sobe
  createRoom: async (name) => {
    const token = localStorage.getItem('token');
    const res = await fetch('http://localhost:8080/api/watchparty', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({ name }),
    });

    if (!res.ok) throw new Error('Failed to create room');
    return await res.json();
  },

  // Lista svih soba
  getActiveRooms: async () => {
    const res = await fetch('http://localhost:8080/api/watchparty');
    if (!res.ok) throw new Error('Failed to fetch rooms');
    return await res.json();
  },

  // Detalji sobe
  getRoomDetails: async (inviteCode) => {
    const res = await fetch(`http://localhost:8080/api/watchparty/${inviteCode}`);
    if (!res.ok) throw new Error('Room not found');
    return await res.json();
  },
};
