// src/store/modules/videoGenerationModule.js
import { videoService } from '@/services/videoService';

export default {
  namespaced: true,
  
  state: {
    jobs: [],
    currentJob: null,
    loading: false,
    error: null
  },
  
  mutations: {
    SET_JOBS(state, jobs) {
      state.jobs = jobs;
    },
    SET_CURRENT_JOB(state, job) {
      state.currentJob = job;
    },
    SET_LOADING(state, loading) {
      state.loading = loading;
    },
    SET_ERROR(state, error) {
      state.error = error;
    }
  },
  
  actions: {
    async generateVideo({ commit }, { prompt, durationSeconds }) {
      commit('SET_LOADING', true);
      try {
        const job = await videoService.generateVideo(prompt, durationSeconds);
        commit('SET_CURRENT_JOB', job);
        return job;
      } catch (error) {
        commit('SET_ERROR', error.message);
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async checkJobStatus({ commit }, jobId) {
      try {
        const job = await videoService.getJobStatus(jobId);
        commit('SET_CURRENT_JOB', job);
        return job;
      } catch (error) {
        commit('SET_ERROR', error.message);
        throw error;
      }
    },
    
    async getAllJobs({ commit }) {
      commit('SET_LOADING', true);
      try {
        const jobs = await videoService.getAllJobs();
        commit('SET_JOBS', jobs);
        return jobs;
      } catch (error) {
        commit('SET_ERROR', error.message);
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    }
  },
  
  getters: {
    isJobComplete: state => {
      return state.currentJob && state.currentJob.status === 'COMPLETED';
    },
    isJobFailed: state => {
      return state.currentJob && state.currentJob.status === 'FAILED';
    },
    isJobInProgress: state => {
      return state.currentJob && 
        (state.currentJob.status === 'PENDING' || state.currentJob.status === 'PROCESSING');
    }
  }
};
