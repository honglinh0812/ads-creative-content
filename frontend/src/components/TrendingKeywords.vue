<template>
  <a-card title="📈 Trending Keywords" class="trending-keywords-card">
    <a-row :gutter="16" class="search-row">
      <a-col :span="16">
        <a-form-item
          :validate-status="validationStatus.searchQuery"
          :help="validationMessages.searchQuery"
        >
          <a-input-search
            v-model:value="searchQuery"
            placeholder="Search trends (e.g., 'tech gadgets', 'fashion')"
            @search="handleSearch"
            @input="validateSearchQuery"
            :loading="loading"
            :maxlength="100"
          >
            <template #enterButton>
              <a-button type="primary" :disabled="!isSearchValid">Search</a-button>
            </template>
          </a-input-search>
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-select
          v-model:value="selectedRegion"
          placeholder="Region"
          style="width: 100%"
          @change="handleRegionChange"
        >
          <a-select-option value="US">🇺🇸 United States</a-select-option>
          <a-select-option value="VN">🇻🇳 Vietnam</a-select-option>
          <a-select-option value="UK">🇬🇧 United Kingdom</a-select-option>
          <a-select-option value="JP">🇯🇵 Japan</a-select-option>
          <a-select-option value="SG">🇸🇬 Singapore</a-select-option>
        </a-select>
      </a-col>
    </a-row>

    <a-alert
      v-if="errorMessage"
      :message="errorMessage"
      type="error"
      show-icon
      closable
      @close="errorMessage = ''"
      class="error-alert"
    />

    <a-spin :spinning="loading">
      <a-list
        v-if="trends.length > 0"
        :data-source="trends"
        class="trends-list"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-checkbox
              v-model:checked="item.selected"
              @change="handleSelectionChange(item)"
              :disabled="!item.selected && selectedKeywords.length >= 5"
            >
              <span class="keyword-text">{{ item.keyword }}</span>
              <a-tag color="green" class="growth-tag">
                <arrow-up-outlined /> +{{ item.growth }}%
              </a-tag>
              <span class="search-volume" v-if="item.searchVolume">
                {{ formatSearchVolume(item.searchVolume) }} searches
              </span>
            </a-checkbox>
          </a-list-item>
        </template>
      </a-list>

      <a-empty
        v-else-if="!loading && hasSearched && trends.length === 0"
        description="No trends found. Try a different search term."
      >
        <template #image>
          <span style="font-size: 48px">🔍</span>
        </template>
      </a-empty>

      <a-alert
        v-else-if="!loading && !hasSearched"
        message="Enter a search term to discover trending keywords"
        description="Find popular topics related to your product or service"
        type="info"
        show-icon
      />
    </a-spin>

    <a-alert
      v-if="selectedKeywords.length >= 5"
      message="Maximum keywords selected"
      description="You have selected the maximum of 5 keywords. Deselect some to add different ones."
      type="warning"
      show-icon
      class="selection-warning"
    />

    <div v-if="selectedKeywords.length > 0" class="action-section">
      <a-divider />
      <div class="selected-keywords">
        <strong>Selected Keywords ({{ selectedKeywords.length }}/5):</strong>
        <a-tag
          v-for="keyword in selectedKeywords"
          :key="keyword"
          closable
          @close="deselectKeyword(keyword)"
          color="blue"
        >
          {{ keyword }}
        </a-tag>
      </div>
      <a-button
        type="primary"
        @click="addToPrompt"
        :disabled="selectedKeywords.length === 0"
        block
        size="large"
      >
        <template #icon>
          <plus-outlined />
        </template>
        Add {{ selectedKeywords.length }} Keyword(s) to Prompt
      </a-button>
    </div>

    <div v-if="lastSearchInfo" class="search-info">
      <a-typography-text type="secondary">
        Last searched: "{{ lastSearchInfo.query }}" in {{ lastSearchInfo.region }} - {{ lastSearchInfo.resultCount }} results
      </a-typography-text>
    </div>
  </a-card>
</template>

<script>
import { ArrowUpOutlined, PlusOutlined } from '@ant-design/icons-vue';
import axios from 'axios';

export default {
  name: 'TrendingKeywords',
  components: {
    ArrowUpOutlined,
    PlusOutlined
  },
  emits: ['keywords-selected', 'validation-change'],
  data() {
    return {
      searchQuery: '',
      selectedRegion: 'US',
      trends: [],
      loading: false,
      hasSearched: false,
      errorMessage: '',
      validationStatus: {
        searchQuery: ''
      },
      validationMessages: {
        searchQuery: ''
      },
      lastSearchInfo: null
    };
  },
  computed: {
    selectedKeywords() {
      return this.trends
        .filter(t => t.selected)
        .map(t => t.keyword);
    },
    isSearchValid() {
      const query = this.searchQuery.trim();
      return query.length >= 2 && query.length <= 100;
    }
  },
  methods: {
    validateSearchQuery() {
      const query = this.searchQuery.trim();

      this.validationStatus.searchQuery = '';
      this.validationMessages.searchQuery = '';

      if (query.length === 0) {
        return true;
      }

      if (query.length < 2) {
        this.validationStatus.searchQuery = 'error';
        this.validationMessages.searchQuery = 'Search query must be at least 2 characters';
        return false;
      }

      if (query.length > 100) {
        this.validationStatus.searchQuery = 'error';
        this.validationMessages.searchQuery = 'Search query must not exceed 100 characters';
        return false;
      }

      // Check for valid characters (letters, numbers, spaces, basic punctuation)
      if (!/^[a-zA-Z0-9\s,.\-']+$/.test(query)) {
        this.validationStatus.searchQuery = 'error';
        this.validationMessages.searchQuery = 'Search query contains invalid characters';
        return false;
      }

      this.validationStatus.searchQuery = 'success';
      return true;
    },
    handleSearch() {
      if (!this.validateSearchQuery()) {
        return;
      }

      if (!this.isSearchValid) {
        this.$message.warning('Please enter a valid search query (2-100 characters)');
        return;
      }

      this.fetchTrends();
    },
    handleRegionChange() {
      // Auto-search when region changes if there's already a query
      if (this.searchQuery.trim() && this.hasSearched) {
        this.fetchTrends();
      }
    },
    async fetchTrends() {
      if (!this.searchQuery.trim()) {
        this.$message.warning('Please enter a search query');
        return;
      }

      if (!this.validateSearchQuery()) {
        return;
      }

      this.loading = true;
      this.errorMessage = '';
      this.hasSearched = true;

      try {
        const response = await axios.get('/api/trends/search', {
          params: {
            query: this.searchQuery.trim(),
            region: this.selectedRegion
          }
        });

        if (response.data && Array.isArray(response.data)) {
          this.trends = response.data.map(trend => ({
            ...trend,
            selected: false
          }));

          // Store search info
          this.lastSearchInfo = {
            query: this.searchQuery.trim(),
            region: this.selectedRegion,
            resultCount: this.trends.length
          };

          if (this.trends.length === 0) {
            this.$message.info('No trending keywords found for this search');
          } else {
            this.$message.success(`Found ${this.trends.length} trending keywords`);
          }
        } else {
          throw new Error('Invalid response format');
        }
      } catch (error) {
        console.error('Error fetching trends:', error);
        this.errorMessage = error.response?.data?.message || 'Failed to fetch trending keywords. Please try again.';
        this.$message.error(this.errorMessage);
        this.trends = [];
      } finally {
        this.loading = false;
      }
    },
    handleSelectionChange(item) {
      // If selecting and already at max, prevent it
      if (item.selected && this.selectedKeywords.length > 5) {
        this.$message.warning('You can select up to 5 keywords maximum');
        item.selected = false;
        return;
      }

      this.emitValidation();
    },
    deselectKeyword(keyword) {
      const trend = this.trends.find(t => t.keyword === keyword);
      if (trend) {
        trend.selected = false;
        this.emitValidation();
      }
    },
    addToPrompt() {
      if (this.selectedKeywords.length === 0) {
        this.$message.warning('Please select at least one keyword');
        return;
      }

      this.$emit('keywords-selected', this.selectedKeywords);
      this.$message.success(`Added ${this.selectedKeywords.length} keyword(s) to prompt`);

      // Clear selections after adding
      this.trends.forEach(trend => {
        trend.selected = false;
      });

      this.emitValidation();
    },
    emitValidation() {
      this.$emit('validation-change', {
        valid: true,
        selectedCount: this.selectedKeywords.length,
        keywords: this.selectedKeywords
      });
    },
    formatSearchVolume(volume) {
      if (volume >= 1000000) {
        return (volume / 1000000).toFixed(1) + 'M';
      } else if (volume >= 1000) {
        return (volume / 1000).toFixed(1) + 'K';
      }
      return volume.toString();
    },
    clearSearch() {
      this.searchQuery = '';
      this.trends = [];
      this.hasSearched = false;
      this.validationStatus.searchQuery = '';
      this.validationMessages.searchQuery = '';
      this.errorMessage = '';
    },
    validateAll() {
      return this.validateSearchQuery();
    }
  }
};
</script>

<style scoped>
.trending-keywords-card {
  margin-bottom: 16px;
}

.search-row {
  margin-bottom: 16px;
}

.error-alert {
  margin-bottom: 16px;
}

.trends-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.keyword-text {
  font-weight: 500;
  margin-right: 8px;
}

.growth-tag {
  margin-right: 8px;
}

.search-volume {
  font-size: 12px;
  color: #8c8c8c;
}

.selection-warning {
  margin-top: 12px;
  margin-bottom: 12px;
}

.action-section {
  margin-top: 16px;
}

.selected-keywords {
  margin-bottom: 12px;
}

.selected-keywords .ant-tag {
  margin: 4px;
}

.search-info {
  margin-top: 12px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
  text-align: center;
}

.trends-list :deep(.ant-checkbox-wrapper.ant-checkbox-wrapper-disabled) {
  opacity: 0.5;
}
</style>
