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
      <a-col :span="12">
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
      <a-col :span="6">
        <a-form-item :label="$t('components.trendingKeywords.location.label')">
          <a-select
            v-model:value="selectedLocation"
            :placeholder="$t('components.trendingKeywords.location.placeholder')"
            style="width: 100%"
            :loading="locationsLoading"
            :disabled="locationsLoading || locationOptions.length === 0"
            @change="handleLocationChange"
          >
            <a-select-option
              v-for="location in locationOptions"
              :key="location.countryCode"
              :value="location.countryCode"
            >
              {{ formatLocationOption(location) }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item :label="$t('components.trendingKeywords.language.label')">
          <a-select
            v-model:value="selectedLanguage"
            :placeholder="$t('components.trendingKeywords.language.placeholder')"
            style="width: 100%"
            :loading="languagesLoading"
            :disabled="languagesLoading || languageOptions.length === 0"
            @change="handleLanguageChange"
          >
            <a-select-option
              v-for="lang in languageOptions"
              :key="lang.languageCode"
              :value="lang.languageCode"
            >
              {{ formatLanguageOption(lang) }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="16" class="search-row">
      <a-col :span="6">
        <a-form-item :label="$t('components.trendingKeywords.limit.label')">
          <a-input-number
            v-model:value="keywordLimit"
            :min="1"
            :max="50"
            :placeholder="$t('components.trendingKeywords.limit.placeholder')"
            style="width: 100%"
            @change="handleLimitChange"
          />
        </a-form-item>
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
        {{ $t('components.trendingKeywords.lastSearch', { query: lastSearchInfo.query, location: lastSearchInfo.locationLabel, language: lastSearchInfo.languageLabel, count: lastSearchInfo.resultCount }) }}
      </a-typography-text>
    </div>
  </a-card>
</template>

<script>
import { ArrowUpOutlined, PlusOutlined, InfoCircleOutlined } from '@ant-design/icons-vue';
import axios from 'axios';

const PREFERRED_LOCATIONS = ['VN', 'US', 'SG', 'TH', 'JP', 'KR', 'AU', 'GB', 'FR', 'CN'];
const PREFERRED_LANGUAGES = ['vi', 'en', 'ja', 'ko', 'zh', 'th', 'fr', 'de'];

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
      selectedLocation: 'VN',
      selectedLanguage: 'vi',
      keywordLimit: 10,
      locationOptions: [],
      languageOptions: [],
      locationsLoading: false,
      languagesLoading: false,
      initializingLocations: true,
      initializingLanguages: true,
      hasManualLocationSelection: false,
      hasManualLanguageSelection: false,
      pendingLanguageFromProp: this.language || 'en',
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
  watch: {
    language: {
      immediate: true,
      handler(newLanguage) {
        this.pendingLanguageFromProp = newLanguage || 'en';
        if (!this.hasManualLanguageSelection && !this.initializingLanguages) {
          this.applyLanguagePreference();
        }
        if (!this.hasManualLocationSelection && !this.initializingLocations) {
          this.applyLocationPreference();
        }
      }
    }
  },
  mounted() {
    this.initializeLocaleOptions();
  },
  methods: {
    async initializeLocaleOptions() {
      await Promise.allSettled([
        this.loadLocationOptions(),
        this.loadLanguageOptions()
      ]);
      this.applyLanguagePreference();
      this.applyLocationPreference();
    },
    async loadLocationOptions() {
      this.locationsLoading = true;
      try {
        const response = await axios.get('/api/trends/locations');
        if (Array.isArray(response.data)) {
          this.locationOptions = this.prioritizeLocations(response.data);
        } else {
          this.locationOptions = [];
        }
      } catch (error) {
        console.error('Failed to load locations', error);
        this.locationOptions = [];
        this.$message.error(this.$t('components.trendingKeywords.messages.locationsError'));
      } finally {
        this.locationsLoading = false;
        this.initializingLocations = false;
        this.applyLocationPreference();
      }
    },
    async loadLanguageOptions() {
      this.languagesLoading = true;
      try {
        const response = await axios.get('/api/trends/languages');
        if (Array.isArray(response.data)) {
          this.languageOptions = this.prioritizeLanguages(response.data);
        } else {
          this.languageOptions = [];
        }
      } catch (error) {
        console.error('Failed to load languages', error);
        this.languageOptions = [];
        this.$message.error(this.$t('components.trendingKeywords.messages.languagesError'));
      } finally {
        this.languagesLoading = false;
        this.initializingLanguages = false;
        this.applyLanguagePreference();
      }
    },
    prioritizeLocations(list) {
      if (!Array.isArray(list)) {
        return [];
      }
      const preferred = list.filter(item =>
        item.countryCode && PREFERRED_LOCATIONS.includes(item.countryCode.toUpperCase())
      );
      if (preferred.length > 0) {
        return preferred;
      }
      return list.slice(0, 30);
    },
    prioritizeLanguages(list) {
      if (!Array.isArray(list)) {
        return [];
      }
      const preferred = list.filter(item =>
        item.languageCode && PREFERRED_LANGUAGES.includes(item.languageCode.toLowerCase())
      );
      if (preferred.length > 0) {
        return preferred;
      }
      return list.slice(0, 30);
    },
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
    handleLocationChange() {
      if (this.initializingLocations) {
        return;
      }
      this.hasManualLocationSelection = true;
      if (this.searchQuery.trim() && this.hasSearched) {
        this.fetchTrends();
      }
    },
    handleLanguageChange() {
      if (this.initializingLanguages) {
        return;
      }
      this.hasManualLanguageSelection = true;
      if (this.searchQuery.trim() && this.hasSearched) {
        this.fetchTrends();
      }
    },
    handleLimitChange(value) {
      if (value === undefined || value === null || Number.isNaN(value)) {
        this.keywordLimit = 10;
        return;
      }
      if (value < 1) {
        this.keywordLimit = 1;
      } else if (value > 50) {
        this.keywordLimit = 50;
      }
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

      if (!this.selectedLocation || !this.selectedLanguage) {
        this.$message.warning(this.$t('components.trendingKeywords.messages.invalidLocale'));
        return;
      }

      this.loading = true;
      this.errorMessage = '';
      this.hasSearched = true;

      try {
        const response = await axios.get('/api/trends/search', {
          params: {
            query: this.searchQuery.trim(),
            location: this.selectedLocation,
            language: this.selectedLanguage,
            num: this.keywordLimit
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
            location: this.selectedLocation,
            locationLabel: this.getLocationLabel(this.selectedLocation),
            language: this.selectedLanguage,
            languageLabel: this.getLanguageLabel(this.selectedLanguage),
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
    },
    applyLanguagePreference() {
      if (this.hasManualLanguageSelection) {
        return;
      }
      const desired = (this.pendingLanguageFromProp || 'en').toLowerCase();
      if (!this.languageOptions.length) {
        this.selectedLanguage = desired;
        return;
      }
      const option = this.languageOptions.find(lang => lang.languageCode.toLowerCase() === desired)
        || this.languageOptions[0];
      if (option) {
        this.selectedLanguage = option.languageCode.toLowerCase();
      }
    },
    applyLocationPreference() {
      if (this.hasManualLocationSelection) {
        return;
      }
      if (!this.locationOptions.length) {
        this.selectedLocation = this.resolveLocationFromLanguage(this.pendingLanguageFromProp);
        return;
      }
      const preferred = this.resolveLocationFromLanguage(this.pendingLanguageFromProp);
      const option = this.locationOptions.find(loc => loc.countryCode === preferred)
        || this.locationOptions.find(loc => loc.countryCode === 'VN')
        || this.locationOptions[0];
      if (option) {
        this.selectedLocation = option.countryCode;
      }
    },
    resolveLocationFromLanguage(language) {
      const normalized = (language || '').toLowerCase();
      if (normalized.startsWith('vi')) return 'VN';
      if (normalized.startsWith('en')) return 'US';
      if (normalized.startsWith('ja')) return 'JP';
      if (normalized.startsWith('ko')) return 'KR';
      if (normalized.startsWith('zh')) return 'CN';
      if (normalized.startsWith('th')) return 'TH';
      if (normalized.startsWith('fr')) return 'FR';
      return 'US';
    },
    formatLocationOption(location) {
      if (!location) {
        return '';
      }
      return `${location.countryName || location.countryCode} (${location.countryCode})`;
    },
    formatLanguageOption(language) {
      if (!language) {
        return '';
      }
      return `${language.languageName || language.languageCode} (${language.languageCode})`;
    },
    getLocationLabel(code) {
      const option = this.locationOptions.find(loc => loc.countryCode === code);
      return option ? `${option.countryName} (${option.countryCode})` : code;
    },
    getLanguageLabel(code) {
      const option = this.languageOptions.find(lang => lang.languageCode === code);
      return option ? `${option.languageName} (${option.languageCode})` : code;
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
