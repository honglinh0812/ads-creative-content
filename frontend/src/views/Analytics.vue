<template>
  <div class="analytics-lite">
    <div class="lite-card hero" v-if="!loading">
      <div>
        <p class="eyebrow">{{ t('analyticsLite.summaryLabel') }}</p>
        <h1>{{ insights?.summary?.totalAds ?? '—' }}</h1>
        <p class="subtitle">{{ t('analyticsLite.summarySubtitle') }}</p>
        <p class="generated-at" v-if="insights?.generatedAt">
          {{ t('analyticsLite.generatedAt', { time: formatDate(insights.generatedAt) }) }}
        </p>
      </div>
      <a-space>
        <a-button @click="loadInsights" :loading="loading">
          {{ t('analyticsLite.actions.refresh') }}
        </a-button>
        <a-button type="primary" @click="$router.push('/ads')">
          {{ t('analyticsLite.actions.goToAds') }}
        </a-button>
      </a-space>
    </div>

    <a-spin v-if="loading" size="large" class="page-spinner" />

    <div v-else>
      <div class="stat-grid" v-if="statCards.length">
        <div v-for="card in statCards" :key="card.label" class="lite-card stat-card">
          <p class="label">{{ card.label }}</p>
          <p class="value">{{ card.value }}</p>
          <p class="hint">{{ card.hint }}</p>
        </div>
      </div>

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
                <p class="hint">{{ t('analyticsLite.cta.percent', { value: cta.percentage }) }}</p>
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
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'

export default {
  name: 'AnalyticsLiteView',
  setup() {
    const { t } = useI18n()
    const insights = ref(null)
    const loading = ref(false)
    const error = ref(null)

    const formatNumber = (value) => new Intl.NumberFormat().format(value || 0)

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

    const ctaUsage = computed(() => {
      if (!insights.value?.ctaUsage) return []
      return insights.value.ctaUsage.slice(0, 4).map(item => ({
        cta: item.cta,
        count: formatNumber(item.count),
        percentage: item.percentage?.toFixed(1)
      }))
    })

    const recentAds = computed(() => insights.value?.recentAds || [])

    const loadInsights = async () => {
      loading.value = true
      error.value = null
      try {
        const { data } = await api.analyticsAPI.getContentInsights()
        insights.value = data.data
      } catch (err) {
        console.error(err)
        error.value = err?.message || 'Unable to load insights'
      } finally {
        loading.value = false
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

    onMounted(() => {
      loadInsights()
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
      t
    }
  }
}
</script>

<style scoped>
.analytics-lite {
  padding: 32px;
  background: #f5f6fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.lite-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.08);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #64748b;
  font-size: 12px;
}

.subtitle {
  color: #475569;
  margin-top: 8px;
}

.generated-at {
  color: #94a3b8;
  margin-top: 4px;
  font-size: 12px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.stat-card .label {
  color: #475569;
  margin-bottom: 8px;
}

.stat-card .value {
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
}

.stat-card .hint {
  color: #94a3b8;
}

.insight-panels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.panel h3 {
  margin: 4px 0 0;
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

@media (max-width: 768px) {
  .analytics-lite {
    padding: 16px;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .recent-item {
    flex-direction: column;
  }

  .tag-group {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
