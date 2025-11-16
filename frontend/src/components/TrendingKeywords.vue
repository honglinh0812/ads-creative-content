<template>
  <a-card class="trending-keywords-card">
    <template #title>
      <div class="trending-card-title">
        <span>{{ cardTitle }}</span>
        <a-tooltip placement="topRight">
          <template #title>
            <strong>{{ $t('components.trendingKeywords.sourceTooltip.title') }}</strong>
            <div>{{ $t('components.trendingKeywords.sourceTooltip.description') }}</div>
          </template>
          <info-circle-outlined class="source-info-icon" />
        </a-tooltip>
      </div>
    </template>
    <a-row :gutter="16" class="search-row">
      <a-col :span="16">
        <a-form-item
          :validate-status="validationStatus.searchQuery"
          :help="validationMessages.searchQuery"
        >
          <a-input-search
            v-model:value="searchQuery"
            :placeholder="$t('components.trendingKeywords.search.placeholder')"
            @search="handleSearch"
            @input="validateSearchQuery"
            :loading="loading"
            :maxlength="100"
          >
            <template #enterButton>
              <a-button type="primary" :disabled="!isSearchValid">{{ $t('components.trendingKeywords.search.button') }}</a-button>
            </template>
          </a-input-search>
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-select
          v-model:value="selectedRegion"
          :placeholder="$t('components.trendingKeywords.region.placeholder')"
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
              <div class="keyword-row">
                <div class="keyword-info">
                  <span class="keyword-text">{{ item.keyword }}</span>
                  <div class="keyword-tags">
                    <a-tag v-if="item.source" color="blue" class="source-tag">
                      {{ item.source }}
                    </a-tag>
                    <a-tag
                      v-if="item.category && item.category !== 'General'"
                      color="purple"
                      class="category-tag"
                    >
                      {{ item.category }}
                    </a-tag>
                  </div>
                </div>
                <div class="keyword-metrics">
                  <a-tag color="green" class="growth-tag">
                    <arrow-up-outlined /> +{{ item.growth }}%
                  </a-tag>
                  <span class="search-volume" v-if="item.searchVolume">
                    {{ $t('components.trendingKeywords.list.searches', { volume: formatSearchVolume(item.searchVolume) }) }}
                  </span>
                </div>
              </div>
            </a-checkbox>
          </a-list-item>
        </template>
      </a-list>

      <a-empty
        v-else-if="!loading && hasSearched && trends.length === 0"
        :description="$t('components.trendingKeywords.empty.description')"
      >
        <template #image>
          <span style="font-size: 48px">🔍</span>
        </template>
      </a-empty>

      <a-alert
        v-else-if="!loading && !hasSearched"
        :message="$t('components.trendingKeywords.help.message')"
        :description="$t('components.trendingKeywords.help.description')"
        type="info"
        show-icon
      />
    </a-spin>

    <a-alert
      v-if="selectedKeywords.length >= 5"
      :message="$t('components.trendingKeywords.selection.maxTitle')"
      :description="$t('components.trendingKeywords.selection.maxDescription')"
      type="warning"
      show-icon
      class="selection-warning"
    />

    <div v-if="selectedKeywords.length > 0" class="action-section">
      <a-divider />
      <div class="selected-keywords">
        <strong>{{ $t('components.trendingKeywords.selection.selectedTitle', { count: selectedKeywords.length }) }}:</strong>
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
        {{ addButtonText }}
      </a-button>
    </div>

    <div v-if="lastSearchInfo" class="search-info">
      <a-typography-text type="secondary">
        {{ $t('components.trendingKeywords.lastSearch', { query: lastSearchInfo.query, region: lastSearchInfo.region, count: lastSearchInfo.resultCount }) }}
      </a-typography-text>
    </div>
  </a-card>
</template>

<script>
import { ArrowUpOutlined, PlusOutlined, InfoCircleOutlined } from '@ant-design/icons-vue';
import axios from 'axios';

export default {
  name: 'TrendingKeywords',
  components: {
    ArrowUpOutlined,
    PlusOutlined,
    InfoCircleOutlined
  },
  props: {
    language: {
      type: String,
      default: 'en'
    }
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
    cardTitle() {
      return this.$t('components.trendingKeywords.title')
    },
    selectedKeywords() {
      return this.trends
        .filter(t => t.selected)
        .map(t => t.keyword);
    },
    isSearchValid() {
      const query = this.searchQuery.trim();
      return query.length >= 2 && query.length <= 100;
    },
    addButtonText() {
      return this.$t('components.trendingKeywords.addButton', { count: this.selectedKeywords.length })
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
        this.validationMessages.searchQuery = this.$t('components.trendingKeywords.validation.minLength');
        return false;
      }

      if (query.length > 100) {
        this.validationStatus.searchQuery = 'error';
        this.validationMessages.searchQuery = this.$t('components.trendingKeywords.validation.maxLength');
        return false;
      }

      // Check for valid characters (Unicode letters, numbers, spaces, basic punctuation)
      // \p{L} = Unicode letters (supports Vietnamese, Chinese, Japanese, etc.)
      // \p{N} = Unicode numbers
      if (!/^[\p{L}\p{N}\s,.\-']+$/u.test(query)) {
        this.validationStatus.searchQuery = 'error';
        this.validationMessages.searchQuery = this.$t('components.trendingKeywords.validation.invalidChars');
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
        this.$message.warning(this.$t('components.trendingKeywords.messages.invalidQuery'));
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
        this.$message.warning(this.$t('components.trendingKeywords.messages.enterQuery'));
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
            this.$message.info(this.$t('components.trendingKeywords.messages.noResults'));
          } else {
            this.$message.success(this.$t('components.trendingKeywords.messages.found', { count: this.trends.length }));
          }
        } else {
          throw new Error('Invalid response format');
        }
      } catch (error) {
        console.error('Error fetching trends:', error);
        this.errorMessage = error.response?.data?.message || this.$t('components.trendingKeywords.messages.error');
        this.$message.error(this.errorMessage);
        this.trends = [];
      } finally {
        this.loading = false;
      }
    },
    handleSelectionChange(item) {
      // If selecting and already at max, prevent it
      if (item.selected && this.selectedKeywords.length > 5) {
        this.$message.warning(this.$t('components.trendingKeywords.messages.maxSelection'));
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
        this.$message.warning(this.$t('components.trendingKeywords.messages.selectOne'));
        return;
      }

      this.$emit('keywords-selected', this.selectedKeywords);
      this.$message.success(this.$t('components.trendingKeywords.messages.added', { count: this.selectedKeywords.length }));

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

.trending-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-info-icon {
  color: #8c8c8c;
  cursor: pointer;
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

.keyword-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.keyword-info {
  flex: 1;
  min-width: 0;
}

.keyword-text {
  font-weight: 500;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.keyword-metrics {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
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
