import axios from 'axios'
import { getApiBaseUrl } from '@/config/api.config'

const API_URL = getApiBaseUrl()

export default {
  /**
   * Lấy danh sách các nhà cung cấp AI
   * @returns {Promise} - Promise chứa danh sách các nhà cung cấp AI
   */
  getAllProviders() {
    return axios.get(`${API_URL}/ai-providers`)
  },
  
  /**
   * Lấy danh sách các nhà cung cấp AI theo khả năng
   * @param {string} capability - Khả năng cần lọc (TEXT, IMAGE, VIDEO)
   * @returns {Promise} - Promise chứa danh sách các nhà cung cấp AI có khả năng được chỉ định
   */
  getProvidersByCapability(capability) {
    return axios.get(`${API_URL}/ai-providers/capability/${capability}`)
  }
}
