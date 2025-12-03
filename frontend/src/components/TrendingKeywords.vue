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
    <div class="search-row">
      <div class="search-field keyword-field">
        <a-form-item
          :validate-status="validationStatus.searchQuery"
          :help="validationMessages.searchQuery"
        >
          <a-input-search
            v-model:value="searchQuery"
            :placeholder="$t('components.trendingKeywords.search.placeholder')"
            :aria-label="$t('components.trendingKeywords.search.label')"
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
      </div>
      <div class="search-field location-field">
        <a-form-item>
          <a-select
            v-model:value="selectedLocation"
            :placeholder="$t('components.trendingKeywords.location.placeholder')"
            :aria-label="$t('components.trendingKeywords.location.label')"
            style="width: 100%"
            :disabled="locationOptions.length === 0"
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
      </div>
      <div class="search-field language-field">
        <a-form-item>
          <a-select
            v-model:value="selectedLanguage"
            :placeholder="$t('components.trendingKeywords.language.placeholder')"
            :aria-label="$t('components.trendingKeywords.language.label')"
            style="width: 100%"
            :disabled="languageOptions.length === 0"
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
      </div>
      <div class="search-field limit-field">
        <a-form-item>
          <a-input-number
            v-model:value="keywordLimit"
            :min="1"
            :max="50"
            :placeholder="$t('components.trendingKeywords.limit.placeholder')"
            :aria-label="$t('components.trendingKeywords.limit.label')"
            style="width: 100%"
            @change="handleLimitChange"
          />
        </a-form-item>
      </div>
    </div>

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
import { PlusOutlined, InfoCircleOutlined } from '@ant-design/icons-vue';
import axios from 'axios';
import { TRENDING_LOCATIONS, TRENDING_LANGUAGES } from '@/constants/trendingLocales';

export default {
  name: 'TrendingKeywords',
  components: {
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
      locationOptions: [...TRENDING_LOCATIONS],
      languageOptions: [...TRENDING_LANGUAGES],
      hasManualLocationSelection: false,
      hasManualLanguageSelection: false,
      pendingLanguageFromProp: this.language || 'vi',
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
        this.pendingLanguageFromProp = newLanguage || 'vi';
        this.applyLanguagePreference();
        this.applyLocationPreference();
      }
    }
  },
  mounted() {
    this.applyLanguagePreference();
    this.applyLocationPreference();
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
    handleLocationChange() {
      this.hasManualLocationSelection = true;
      if (this.searchQuery.trim() && this.hasSearched) {
        this.fetchTrends();
      }
    },
    handleLanguageChange() {
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
      const desired = (this.pendingLanguageFromProp || 'vi').toLowerCase();
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
      return 'VN';
    },
    resolveCountryName(location) {
      if (!location) return '';
      return location.countryName || location.country_name || location.countryCode || '';
    },
    resolveLanguageName(language) {
      if (!language) return '';
      return language.languageName || language.language_name || language.languageCode || '';
    },
    formatLocationOption(location) {
      if (!location) {
        return '';
      }
      return `${this.resolveCountryName(location)} (${location.countryCode || ''})`;
    },
    formatLanguageOption(language) {
      if (!language) {
        return '';
      }
      return `${this.resolveLanguageName(language)} (${language.languageCode || ''})`;
    },
    getLocationLabel(code) {
      const option = this.locationOptions.find(loc => loc.countryCode === code);
      return option ? `${this.resolveCountryName(option)} (${code})` : code;
    },
    getLanguageLabel(code) {
      const option = this.languageOptions.find(lang => lang.languageCode === code);
      return option ? `${this.resolveLanguageName(option)} (${code})` : code;
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
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 16px;
}

.search-field {
  flex: 1 1 180px;
}

.keyword-field {
  flex: 2 1 320px;
}

.limit-field {
  flex: 0 0 140px;
}

.search-row :deep(.ant-form-item) {
  margin-bottom: 0;
  width: 100%;
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
