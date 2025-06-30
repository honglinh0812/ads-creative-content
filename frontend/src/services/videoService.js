// src/services/videoService.js
import axios from 'axios';

const API_URL = '/api/video';

export const videoService = {
  /**
   * Generate a new video from text prompt
   * @param {string} prompt - The text prompt
   * @param {number} durationSeconds - Duration in seconds (10-15)
   * @returns {Promise<Object>} - The job object
   */
  async generateVideo(prompt, durationSeconds) {
    const response = await axios.post(`${API_URL}/generate`, {
      prompt,
      durationSeconds
    });
    return response.data;
  },
  
  /**
   * Get the status of a video generation job
   * @param {number} jobId - The job ID
   * @returns {Promise<Object>} - The updated job object
   */
  async getJobStatus(jobId) {
    const response = await axios.get(`${API_URL}/${jobId}`);
    return response.data;
  },
  
  /**
   * Get all video generation jobs
   * @returns {Promise<Array>} - List of all jobs
   */
  async getAllJobs() {
    const response = await axios.get(API_URL);
    return response.data;
  }
};
