
import React, { useState, useContext } from 'react';
import { AppContext } from '../../App';
import { AppContextType } from '../../types';
import apiService from '../../services/apiService';
import { API_CONFIG } from '../../config/api';

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const UploadModal: React.FC<UploadModalProps> = ({ isOpen, onClose }) => {
  const { user } = useContext(AppContext) as AppContextType;
  const [title, setTitle] = useState('');
  const [artist, setArtist] = useState('');
  const [album, setAlbum] = useState('');
  const [genre, setGenre] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !genre || !file || !user) {
      setError('All fields are required.');
      return;
    }

    setIsUploading(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('title', title);
      formData.append('genre', genre);
      formData.append('album', album || '');
      formData.append('uploaderId', user.id);

      const response = await fetch(`${API_CONFIG.BASE_URL}/upload/track`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: formData
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Upload failed');
      }

      const uploadedTrack = await response.json();
      console.log('Track uploaded successfully:', uploadedTrack);
      
      onClose();
      // Reset form
      setTitle('');
      setArtist('');
      setAlbum('');
      setGenre('');
      setFile(null);
      setError(null);
      
      // Refresh the page or update the track list
      window.location.reload();
      
    } catch (error) {
      console.error('Upload error:', error);
      setError(error instanceof Error ? error.message : 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50">
      <div className="bg-gray-800 rounded-lg p-8 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Upload a Track</h2>
        {error && <p className="text-red-500 mb-4">{error}</p>}
        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="title" className="block text-sm font-medium text-gray-300 mb-1">Title *</label>
            <input 
              type="text" 
              id="title" 
              value={title} 
              onChange={(e) => setTitle(e.target.value)} 
              className="w-full bg-gray-700 border border-gray-600 rounded-md p-2 text-white focus:ring-purple-500 focus:border-purple-500" 
              disabled={isUploading}
            />
          </div>
          <div className="mb-4">
            <label htmlFor="album" className="block text-sm font-medium text-gray-300 mb-1">Album</label>
            <input 
              type="text" 
              id="album" 
              value={album} 
              onChange={(e) => setAlbum(e.target.value)} 
              className="w-full bg-gray-700 border border-gray-600 rounded-md p-2 text-white focus:ring-purple-500 focus:border-purple-500" 
              disabled={isUploading}
            />
          </div>
          <div className="mb-4">
            <label htmlFor="genre" className="block text-sm font-medium text-gray-300 mb-1">Genre *</label>
            <input 
              type="text" 
              id="genre" 
              value={genre} 
              onChange={(e) => setGenre(e.target.value)} 
              className="w-full bg-gray-700 border border-gray-600 rounded-md p-2 text-white focus:ring-purple-500 focus:border-purple-500" 
              disabled={isUploading}
            />
          </div>
          <div className="mb-6">
            <label htmlFor="file" className="block text-sm font-medium text-gray-300 mb-1">Audio File * (Max 50MB)</label>
            <input 
              type="file" 
              id="file" 
              accept="audio/*" 
              onChange={(e) => setFile(e.target.files ? e.target.files[0] : null)} 
              className="w-full text-sm text-gray-400 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-purple-50 file:text-purple-700 hover:file:bg-purple-100" 
              disabled={isUploading}
            />
            {file && (
              <p className="text-sm text-gray-400 mt-1">
                Selected: {file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)
              </p>
            )}
          </div>
          <div className="flex justify-end space-x-4">
            <button 
              type="button" 
              onClick={onClose} 
              className="px-4 py-2 bg-gray-600 rounded-md hover:bg-gray-700 disabled:opacity-50" 
              disabled={isUploading}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="px-4 py-2 bg-purple-600 rounded-md hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed" 
              disabled={isUploading}
            >
              {isUploading ? 'Uploading...' : 'Upload'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UploadModal;
