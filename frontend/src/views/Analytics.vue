<template>
  <div class="analytics-lite">
    <!-- Hero Section -->
    <div class="lite-card hero" v-if="!loading">
      <div class="hero-content">
        <div class="hero-text">
          <p class="eyebrow">{{ t('analyticsLite.summaryLabel') }}</p>
          <h1 class="hero-number">{{ insights?.summary?.totalAds ?? '—' }}</h1>
          <p class="subtitle">{{ t('analyticsLite.summarySubtitle') }}</p>
          <p class="generated-at" v-if="insights?.generatedAt">
            {{ t('analyticsLite.generatedAt', { time: formatDate(insights.generatedAt) }) }}
          </p>
        </div>
        <a-space class="hero-actions" :size="12">
          <a-button @click="loadInsights" :loading="loading" size="large">
            <template #icon><ReloadOutlined /></template>
            {{ t('analyticsLite.actions.refresh') }}
          </a-button>
          <a-button type="primary" @click="$router.push('/ads')" size="large">
            <template #icon><AppstoreOutlined /></template>
            {{ t('analyticsLite.actions.goToAds') }}
          </a-button>
        </a-space>
      </div>
    </div>

    <a-spin v-if="loading" size="large" class="page-spinner" tip="Đang tải dữ liệu..." />

    <div v-else>
      <div class="stat-grid" v-if="statCards.length">
        <div v-for="card in statCards" :key="card.label" class="lite-card stat-card">
          <p class="label">{{ card.label }}</p>
          <p class="value">{{ card.value }}</p>
          <p class="hint">{{ card.hint }}</p>
        </div>
      </div>

      <div class="lite-card panel leaderboard-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">{{ t('analyticsLite.leaderboard.title') }}</p>
            <h3>{{ t('analyticsLite.leaderboard.subtitle') }}</h3>
          </div>
          <div class="leaderboard-filters">
            <a-select v-model:value="leaderboardEntity" size="small">
              <a-select-option value="ads">
                {{ t('analyticsLite.leaderboard.entities.ads') }}
              </a-select-option>
              <a-select-option value="campaigns">
                {{ t('analyticsLite.leaderboard.entities.campaigns') }}
              </a-select-option>
            </a-select>
            <a-select v-model:value="leaderboardMetric" size="small">
              <a-select-option value="ctr">
                {{ t('analyticsLite.leaderboard.metrics.ctr') }}
              </a-select-option>
              <a-select-option value="cpc">
                {{ t('analyticsLite.leaderboard.metrics.cpc') }}
              </a-select-option>
            </a-select>
            <a-select v-model:value="leaderboardRange" size="small">
              <a-select-option value="7d">
                {{ t('analyticsLite.leaderboard.ranges.7d') }}
              </a-select-option>
              <a-select-option value="30d">
                {{ t('analyticsLite.leaderboard.ranges.30d') }}
              </a-select-option>
              <a-select-option value="90d">
                {{ t('analyticsLite.leaderboard.ranges.90d') }}
              </a-select-option>
            </a-select>
          </div>
        </div>
        <div class="leaderboard-body">
          <a-spin v-if="leaderboardLoading" />
          <template v-else>
            <div v-if="leaderboardSnapshot.top.length" class="leaderboard-content">
              <div class="leaderboard-meta">
                <p>
                  {{ t('analyticsLite.leaderboard.avgLabel', { metric: leaderboardMetricLabel }) }}
                  · {{ formatMetricValue(leaderboardSnapshot.average) }}
                </p>
              </div>
              <div class="leaderboard-columns">
                <div class="leaderboard-column">
                  <p class="eyebrow">{{ t('analyticsLite.leaderboard.topTitle') }}</p>
                  <div
                    v-for="item in leaderboardSnapshot.top"
                    :key="`top-${item.id}`"
                    class="leaderboard-item"
                  >
                    <div>
                      <p class="item-name">{{ item.name }}</p>
                      <p class="item-subtitle">{{ item.subtitle }}</p>
                    </div>
                    <div class="item-metric">
                      <span>{{ formatMetricValue(item.value) }}</span>
                    </div>
                  </div>
                </div>
                <div class="leaderboard-column attention">
                  <p class="eyebrow">{{ t('analyticsLite.leaderboard.bottomTitle') }}</p>
                  <div
                    v-for="item in leaderboardSnapshot.bottom"
                    :key="`bottom-${item.id}`"
                    class="leaderboard-item"
                  >
                    <div>
                      <p class="item-name">{{ item.name }}</p>
                      <p class="item-subtitle">{{ item.subtitle }}</p>
                    </div>
                    <div class="item-metric">
                      <span>{{ formatMetricValue(item.value) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <p v-else class="empty-text">{{ t('analyticsLite.leaderboard.empty') }}</p>
          </template>
        </div>
      </div>
      <a-alert
        v-if="leaderboardError"
        type="warning"
        show-icon
        :message="leaderboardError"
        class="leaderboard-alert"
      />

      <div class="insight-panels">
        <div class="lite-card panel">
          <div class="panel-header">
            <div>
              <p class="eyebrow">{{ t('analyticsLite.copy.title') }}</p>
              <h3>{{ t('analyticsLite.copy.subtitle') }}</h3>
            </div>
          </div>

          <div class="metrics">
            <div v-for="metric in copyMetrics" :key="metric.label">
              <p class="label">{{ metric.label }}</p>
              <p class="value">{{ metric.value }}</p>
            </div>
          </div>

          <div class="keywords" v-if="insights?.copyMetrics?.keywordHighlights?.length">
            <p class="label">{{ t('analyticsLite.copy.keywords') }}</p>
            <a-tag
              v-for="keyword in insights.copyMetrics.keywordHighlights"
              :key="keyword.keyword"
              color="blue"
            >
              {{ keyword.keyword }} · {{ keyword.count }}
            </a-tag>
          </div>
        </div>

        <div class="lite-card panel">
          <div class="panel-header">
            <div>
              <p class="eyebrow">{{ t('analyticsLite.cta.title') }}</p>
              <h3>{{ t('analyticsLite.cta.subtitle') }}</h3>
            </div>
          </div>

          <div v-if="ctaUsage.length" class="cta-list">
            <div v-for="cta in ctaUsage" :key="cta.cta" class="cta-item">
              <div>
                <p class="value">{{ cta.cta }}</p>
                <p class="hint">{{ t('analyticsLite.cta.percent', { value: cta.percentageValue.toFixed(1) }) }}</p>
              </div>
              <p class="count">{{ cta.count }}</p>
            </div>
          </div>
          <p v-else class="empty-text">{{ t('analyticsLite.cta.empty') }}</p>
        </div>
      </div>

      <div class="lite-card panel recent-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">{{ t('analyticsLite.recent.title') }}</p>
            <h3>{{ t('analyticsLite.recent.subtitle') }}</h3>
          </div>
        </div>

        <div v-if="recentAds.length" class="recent-list">
          <div
            v-for="ad in recentAds"
            :key="ad.id"
            class="recent-item"
          >
            <div class="recent-meta">
              <h4>{{ ad.name }}</h4>
              <p class="hint">
                {{ ad.campaignName || t('analyticsLite.recent.uncategorized') }}
                · {{ formatDate(ad.createdDate) }}
              </p>
              <p class="excerpt">{{ ad.excerpt }}</p>
            </div>
            <div class="tag-group">
              <a-tag>{{ ad.adType || '—' }}</a-tag>
              <a-tag :color="ad.hasMedia ? 'green' : 'orange'">
                {{ ad.hasMedia ? t('analyticsLite.recent.media') : t('analyticsLite.recent.noMedia') }}
              </a-tag>
            </div>
          </div>
        </div>
        <p v-else class="empty-text">{{ t('analyticsLite.recent.empty') }}</p>
      </div>
    </div>

    <a-alert
      v-if="error"
      type="error"
      show-icon
      :message="t('analyticsLite.error.title')"
      :description="error"
      class="error-alert"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ReloadOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import api from '@/services/api'

export default {
  name: 'AnalyticsLiteView',
  components: {
    ReloadOutlined,
    AppstoreOutlined
  },
  setup() {
    const { t } = useI18n()
    const insights = ref(null)
    const loading = ref(false)
    const error = ref(null)
    const fallbackRange = '30d'
    const contentInsightsSupported = ref(true)
    const analyticsData = ref(null)
    const leaderboardRange = ref('30d')
    const leaderboardMetric = ref('ctr')
    const leaderboardEntity = ref('ads')
    const leaderboardLoading = ref(false)
    const leaderboardError = ref(null)

    const formatNumber = (value) => new Intl.NumberFormat().format(value || 0)
    const formatDecimal = (value, digits = 2) => {
      const numeric = Number(value) || 0
      return new Intl.NumberFormat(undefined, {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
      }).format(numeric)
    }

    const metricConfig = {
      ctr: {
        key: 'ctr',
        formatter: (value) => `${formatDecimal(value)}%`,
        higherIsBetter: true
      },
      cpc: {
        key: 'cpc',
        formatter: (value) => formatDecimal(value, 2),
        higherIsBetter: false
      }
    }

    const statCards = computed(() => {
      if (!insights.value?.summary) return []
      const summary = insights.value.summary
      return [
        {
          label: t('analyticsLite.stats.textAds'),
          value: formatNumber(summary.textAds),
          hint: summary.totalAds ? `${Math.round((summary.textAds / summary.totalAds) * 100)}%` : '—'
        },
        {
          label: t('analyticsLite.stats.mediaAds'),
          value: formatNumber(summary.mediaAds),
          hint: summary.totalAds ? `${Math.round((summary.mediaAds / summary.totalAds) * 100)}%` : '—'
        },
        {
          label: t('analyticsLite.stats.campaigns'),
          value: formatNumber(summary.uniqueCampaigns),
          hint: t('analyticsLite.stats.campaignHint')
        }
      ]
    })

    const copyMetrics = computed(() => {
      const metrics = insights.value?.copyMetrics
      if (!metrics) return []
      return [
        {
          label: t('analyticsLite.copy.headline'),
          value: `${Math.round(metrics.averageHeadlineLength)} ${t('analyticsLite.copy.characters')}`
        },
        {
          label: t('analyticsLite.copy.primary'),
          value: `${Math.round(metrics.averagePrimaryTextLength)} ${t('analyticsLite.copy.characters')}`
        },
        {
          label: t('analyticsLite.copy.description'),
          value: `${Math.round(metrics.averageDescriptionLength)} ${t('analyticsLite.copy.characters')}`
        }
      ]
    })

    const normalizeNumber = (value) => {
      if (typeof value === 'number' && Number.isFinite(value)) return value
      const parsed = Number(value)
      return Number.isFinite(parsed) ? parsed : 0
    }

    const ctaUsage = computed(() => {
      if (!insights.value?.ctaUsage) return []
      return insights.value.ctaUsage.slice(0, 4).map(item => {
        const countNumber = normalizeNumber(item.count)
        const percentageNumber = normalizeNumber(item.percentage)
        return {
          cta: item.cta,
          count: formatNumber(countNumber),
          percentageValue: percentageNumber
        }
      })
    })

    const recentAds = computed(() => insights.value?.recentAds || [])

    const leaderboardMetricLabel = computed(() =>
      t(`analyticsLite.leaderboard.metrics.${leaderboardMetric.value}`)
    )

    const leaderboardSnapshot = computed(() => {
      const config = metricConfig[leaderboardMetric.value]
      if (!analyticsData.value || !config) {
        return { top: [], bottom: [], average: 0 }
      }

      const source = leaderboardEntity.value === 'campaigns'
        ? analyticsData.value.campaignAnalytics || []
        : analyticsData.value.adAnalytics || []

      const mapped = source
        .map(item => {
          const value = Number(item?.[config.key]) || 0
          const name = leaderboardEntity.value === 'campaigns'
            ? (item.campaignName || '—')
            : (item.adName || '—')
          const subtitle = leaderboardEntity.value === 'campaigns'
            ? (item.objective || item.status || '')
            : (item.campaignName || t('analyticsLite.recent.uncategorized'))
          return {
            id: item.adId || item.campaignId || item.id || name,
            name,
            subtitle,
            value
          }
        })
        .filter(item => Number.isFinite(item.value))

      if (!mapped.length) {
        return { top: [], bottom: [], average: 0 }
      }

      const average = mapped.reduce((sum, entry) => sum + entry.value, 0) / mapped.length
      const sortAsc = (a, b) => a.value - b.value
      const sortDesc = (a, b) => b.value - a.value
      const bestSorted = [...mapped].sort(config.higherIsBetter ? sortDesc : sortAsc)
      const worstSorted = [...mapped].sort(config.higherIsBetter ? sortAsc : sortDesc)

      return {
        top: bestSorted.slice(0, 5),
        bottom: worstSorted.slice(0, 5),
        average
      }
    })

    const formatMetricValue = (value) => {
      const config = metricConfig[leaderboardMetric.value]
      if (!config) return formatDecimal(value)
      return config.formatter(value)
    }

    const shouldUseFallback = (err) => {
      const status = err?.response?.status
      return status === 404 || status === 501 || status === 503
    }

    const loadInsights = async () => {
      loading.value = true
      error.value = null
      try {
        if (contentInsightsSupported.value) {
          const { data } = await api.analyticsAPI.getContentInsights()
          insights.value = data.data
          loading.value = false
          return
        }
        await loadFallbackInsights()
      } catch (err) {
        console.error(err)
        if (shouldUseFallback(err)) {
          contentInsightsSupported.value = false
          await loadFallbackInsights()
        } else {
          error.value = err?.message || 'Unable to load insights'
        }
      } finally {
        loading.value = false
      }
    }

    const loadFallbackInsights = async () => {
      try {
        const { data } = await api.analyticsAPI.getDashboard(fallbackRange)
        analyticsData.value = data.data
        insights.value = buildInsightsFromDashboard(data.data)
      } catch (dashboardErr) {
        console.error(dashboardErr)
        error.value = dashboardErr?.message || 'Unable to load fallback insights'
      }
    }

    const hasCopy = (ad = {}) => {
      return [ad.primaryText, ad.headline, ad.description].some(text =>
        typeof text === 'string' && text.trim().length > 0
      )
    }

    const hasMedia = (ad = {}) => Boolean(ad.imageUrl || ad.videoUrl)

    const averageLength = (texts = []) => {
      const valid = texts
        .filter(text => typeof text === 'string')
        .map(text => text.trim().length)
        .filter(length => length > 0)
      if (!valid.length) return 0
      return valid.reduce((sum, length) => sum + length, 0) / valid.length
    }

    const extractKeywords = (ads = []) => {
      const counts = new Map()
      ads.forEach(ad => {
        const text = [ad.primaryText, ad.headline, ad.description]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        const tokens = text.split(/[^a-zA-Z0-9À-ỹ]+/).filter(word => word.length > 3)
        tokens.forEach(token => {
          counts.set(token, (counts.get(token) || 0) + 1)
        })
      })
      return Array.from(counts.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 6)
        .map(([keyword, count]) => ({ keyword, count }))
    }

    const buildCtaUsage = (ads = []) => {
      if (!ads.length) return []
      const counts = new Map()
      ads.forEach(ad => {
        const value = ad.callToAction || 'NONE'
        counts.set(value, (counts.get(value) || 0) + 1)
      })
      return Array.from(counts.entries()).map(([cta, count]) => ({
        cta,
        count,
        percentage: (count / ads.length) * 100
      }))
    }

    const buildAdTypeBreakdown = (ads = []) => {
      return ads.reduce((acc, ad) => {
        const key = ad.adType || 'UNKNOWN'
        acc[key] = (acc[key] || 0) + 1
        return acc
      }, {})
    }

    const buildRecentAds = (ads = []) => {
      return [...ads]
        .sort((a, b) => {
          const aDate = new Date(a.createdDate || 0).getTime()
          const bDate = new Date(b.createdDate || 0).getTime()
          return bDate - aDate
        })
        .slice(0, 5)
        .map(ad => ({
          id: ad.adId || ad.id,
          name: ad.adName || ad.name || t('analyticsLite.recent.uncategorized'),
          campaignName: ad.campaignName,
          adType: ad.adType,
          hasMedia: hasMedia(ad),
          excerpt: buildExcerpt(ad),
          createdDate: ad.createdDate
        }))
    }

    const buildExcerpt = (ad = {}) => {
      const source = ad.primaryText || ad.headline || ''
      if (source.length <= 140) {
        return source
      }
      return `${source.slice(0, 137).trim()}...`
    }

    const buildInsightsFromDashboard = (dashboardData = {}) => {
      const ads = Array.isArray(dashboardData.adAnalytics) ? dashboardData.adAnalytics : []
      const summary = {
        totalAds: ads.length,
        textAds: ads.filter(hasCopy).length,
        mediaAds: ads.filter(hasMedia).length,
        uniqueCampaigns: new Set(ads.map(ad => ad.campaignName).filter(Boolean)).size
      }

      const copyMetrics = {
        averageHeadlineLength: averageLength(ads.map(ad => ad.headline)),
        averagePrimaryTextLength: averageLength(ads.map(ad => ad.primaryText)),
        averageDescriptionLength: averageLength(ads.map(ad => ad.description)),
        keywordHighlights: extractKeywords(ads)
      }

      const ctaUsage = buildCtaUsage(ads)
      const adTypeBreakdown = buildAdTypeBreakdown(ads)
      const recentAds = buildRecentAds(ads)

      return {
        summary,
        copyMetrics,
        ctaUsage,
        adTypeBreakdown,
        recentAds,
        generatedAt: new Date().toISOString()
      }
    }

    const formatDate = (value) => {
      if (!value) return '—'
      return new Intl.DateTimeFormat(undefined, {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }).format(new Date(value))
    }

    const loadLeaderboard = async () => {
      leaderboardLoading.value = true
      leaderboardError.value = null
      try {
        const { data } = await api.analyticsAPI.getDashboard(leaderboardRange.value)
        analyticsData.value = data.data
      } catch (err) {
        leaderboardError.value = err?.message || 'Unable to fetch performance data'
      } finally {
        leaderboardLoading.value = false
      }
    }

    onMounted(() => {
      loadInsights()
      loadLeaderboard()
    })

    watch(leaderboardRange, () => {
      loadLeaderboard()
    })

    return {
      insights,
      loading,
      error,
      statCards,
      copyMetrics,
      ctaUsage,
      recentAds,
      loadInsights,
      formatDate,
      t,
      leaderboardRange,
      leaderboardMetric,
      leaderboardEntity,
      leaderboardSnapshot,
      leaderboardMetricLabel,
      formatMetricValue,
      leaderboardLoading,
      leaderboardError
    }
  }
}
</script>

<style scoped>
.analytics-lite {
  padding: clamp(16px, 4vw, 32px);
  background: #f5f6fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: clamp(16px, 3vw, 24px);
}

.lite-card {
  background: white;
  border-radius: 8px;
  padding: clamp(20px, 3vw, 24px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.hero {
  border-radius: 8px;
}

.hero-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.hero-text {
  flex: 1;
}

.hero-number {
  font-size: clamp(48px, 8vw, 64px);
  font-weight: 700;
  margin: 8px 0 12px;
  color: #0f172a;
  line-height: 1;
}

.hero-actions {
  flex-shrink: 0;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 4px;
}

.subtitle {
  color: #475569;
  margin-top: 8px;
  font-size: 14px;
}

.generated-at {
  color: #94a3b8;
  margin-top: 8px;
  font-size: 12px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 240px), 1fr));
  gap: clamp(12px, 2vw, 16px);
}

.stat-card .label {
  color: #64748b;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
}

.stat-card .value {
  font-size: clamp(28px, 5vw, 32px);
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.stat-card .hint {
  color: #94a3b8;
  font-size: 12px;
}

.insight-panels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 320px), 1fr));
  gap: clamp(12px, 2vw, 16px);
}

.leaderboard-panel {
  margin-top: 16px;
}

.leaderboard-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.leaderboard-body {
  min-height: 120px;
}

.leaderboard-meta {
  margin-bottom: 12px;
  color: #475569;
  font-size: 13px;
}

.leaderboard-columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.leaderboard-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.leaderboard-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.leaderboard-item:last-child {
  border-bottom: none;
}

.leaderboard-column.attention .leaderboard-item {
  border-color: #ffe4e6;
}

.item-name {
  font-weight: 600;
  margin: 0;
}

.item-subtitle {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.item-metric span {
  font-weight: 600;
  color: #0f172a;
}

.leaderboard-alert {
  margin-top: 8px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.panel h3 {
  margin: 4px 0 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.metrics .label {
  color: #475569;
}

.metrics .value {
  font-size: 20px;
  font-weight: 600;
}

.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cta-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}

.cta-item .count {
  font-weight: 600;
  color: #0f172a;
}

.recent-panel .recent-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.recent-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.recent-meta h4 {
  margin: 0;
  font-size: 18px;
}

.hint {
  color: #94a3b8;
  margin: 4px 0;
}

.excerpt {
  color: #0f172a;
  margin: 0;
}

.tag-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-text {
  color: #94a3b8;
}

.error-alert {
  max-width: 520px;
}

.page-spinner {
  display: flex;
  justify-content: center;
  padding: 64px 0;
}

/* Responsive Design */
@media (max-width: 768px) {
  .analytics-lite {
    padding: 16px;
    gap: 16px;
  }

  .hero-content {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.ant-space-item) {
    flex: 1;
  }

  .hero-actions :deep(.ant-btn) {
    width: 100%;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .insight-panels {
    grid-template-columns: 1fr;
  }

  .recent-item {
    flex-direction: column;
    gap: 12px;
  }

  .tag-group {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .scores {
    flex-wrap: wrap;
    gap: 16px;
  }
}

/* Tablet specific */
@media (min-width: 769px) and (max-width: 1024px) {
  .insight-panels {
    grid-template-columns: 1fr;
  }

  .leaderboard-filters {
    width: 100%;
  }
}

/* Print styles */
@media print {
  .hero-actions,
  .generated-at {
    display: none;
  }

  .analytics-lite {
    background: white;
    padding: 0;
  }

  .lite-card {
    box-shadow: none;
    border: 1px solid #e2e8f0;
    page-break-inside: avoid;
  }
}
</style>
