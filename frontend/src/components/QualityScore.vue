<template>
  <div class="quality-score-card" :class="{ compact: compact }">
    <div class="score-header">
      <h3 v-if="!compact">{{ $t('qualityScore.title') }}</h3>
      <div class="total-score" :class="getScoreClass(score.totalScore)">
        <span class="score-number">{{ score.totalScore?.toFixed(1) || 0 }}</span>
        <span class="score-max">{{ $t('qualityScore.totalMax') }}</span>
      </div>
      <div class="grade-badge" :class="getGradeClass(score.grade)">
        {{ score.grade || $t('qualityScore.gradeNA') }}
      </div>
    </div>

    <div v-if="!compact" class="score-breakdown">
      <div class="score-item">
        <div class="score-label">
          <span class="label-text">{{ $t('qualityScore.breakdown.compliance') }}</span>
          <span class="score-value">{{ score.complianceScore?.toFixed(1) || 0 }}{{ $t('qualityScore.breakdown.complianceMax') }}</span>
        </div>
        <a-progress
          :percent="(score.complianceScore / 30) * 100"
          :show-info="false"
          :stroke-color="getProgressColor(score.complianceScore, 30)"
        />
      </div>

      <div class="score-item">
        <div class="score-label">
          <span class="label-text">{{ $t('qualityScore.breakdown.linguisticQuality') }}</span>
          <span class="score-value">{{ score.linguisticScore?.toFixed(1) || 0 }}{{ $t('qualityScore.breakdown.linguisticMax') }}</span>
        </div>
        <a-progress
          :percent="(score.linguisticScore / 30) * 100"
          :show-info="false"
          :stroke-color="getProgressColor(score.linguisticScore, 30)"
        />
      </div>

      <div class="score-item">
        <div class="score-label">
          <span class="label-text">{{ $t('qualityScore.breakdown.persuasiveness') }}</span>
          <span class="score-value">{{ score.persuasivenessScore?.toFixed(1) || 0 }}{{ $t('qualityScore.breakdown.persuasivenessMax') }}</span>
        </div>
        <a-progress
          :percent="(score.persuasivenessScore / 20) * 100"
          :show-info="false"
          :stroke-color="getProgressColor(score.persuasivenessScore, 20)"
        />
      </div>

      <div class="score-item">
        <div class="score-label">
          <span class="label-text">{{ $t('qualityScore.breakdown.completeness') }}</span>
          <span class="score-value">{{ score.completenessScore?.toFixed(1) || 0 }}{{ $t('qualityScore.breakdown.completenessMax') }}</span>
        </div>
        <a-progress
          :percent="(score.completenessScore / 20) * 100"
          :show-info="false"
          :stroke-color="getProgressColor(score.completenessScore, 20)"
        />
      </div>
    </div>

    <div v-if="!compact && localizedSuggestions.length > 0" class="suggestions">
      <h4>{{ $t('qualityScore.suggestions.title') }}</h4>
      <ul>
        <li v-for="(suggestion, index) in localizedSuggestions" :key="index">
          {{ suggestion }}
        </li>
      </ul>
    </div>

    <!-- Compact mode: show top suggestion only -->
    <div v-if="compact && localizedSuggestions.length > 0" class="compact-suggestion">
      💡 {{ localizedSuggestions[0] }}
    </div>
  </div>
</template>

<script>
import { Progress } from 'ant-design-vue';

export default {
  name: 'QualityScore',
  components: {
    'a-progress': Progress
  },
  props: {
    score: {
      type: Object,
      default: () => ({
        totalScore: 0,
        complianceScore: 0,
        linguisticScore: 0,
        persuasivenessScore: 0,
        completenessScore: 0,
        grade: 'N/A',
        suggestions: []
      })
    },
    compact: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    localizedSuggestions() {
      if (!this.score || !Array.isArray(this.score.suggestions)) {
        return []
      }
      return this.score.suggestions
        .map(suggestion => this.translateSuggestion(suggestion))
        .filter(Boolean)
    }
  },
  methods: {
    getScoreClass(score) {
      if (score >= 80) return 'excellent';
      if (score >= 60) return 'good';
      if (score >= 40) return 'average';
      return 'poor';
    },
    getGradeClass(grade) {
      if (!grade) return 'grade-na';
      if (grade.startsWith('A')) return 'grade-a';
      if (grade.startsWith('B')) return 'grade-b';
      if (grade.startsWith('C')) return 'grade-c';
      if (grade.startsWith('D')) return 'grade-d';
      return 'grade-f';
    },
    getProgressColor(score, max) {
      const percentage = (score / max) * 100;
      if (percentage >= 80) return '#52c41a'; // Green
      if (percentage >= 60) return '#faad14'; // Yellow
      return '#f5222d'; // Red
    },
    translateSuggestion(text) {
      if (!text) return ''

      const patterns = [
        {
          regex: /^Headline is too long \((\d+) chars, max (\d+)\)/i,
          key: 'qualityScore.suggestions.headlineTooLong',
          params: match => ({ current: match[1], max: match[2] })
        },
        {
          regex: /^Description is too long \((\d+) chars, max (\d+)\)/i,
          key: 'qualityScore.suggestions.descriptionTooLong',
          params: match => ({ current: match[1], max: match[2] })
        },
        {
          regex: /^Primary text is too long \((\d+) chars, max (\d+)\)/i,
          key: 'qualityScore.suggestions.primaryTextTooLong',
          params: match => ({ current: match[1], max: match[2] })
        },
        {
          regex: /^Avoid using prohibited word: '([^']+)'/i,
          key: 'qualityScore.suggestions.prohibitedWord',
          params: match => ({ word: match[1] })
        },
        {
          regex: /^Improve readability/i,
          key: 'qualityScore.suggestions.improveReadability'
        },
        {
          regex: /^Add a clear call-to-action/i,
          key: 'qualityScore.suggestions.addCTA'
        },
        {
          regex: /^Use more power words/i,
          key: 'qualityScore.suggestions.usePowerWords'
        },
        {
          regex: /^Add a headline/i,
          key: 'qualityScore.suggestions.addHeadline'
        },
        {
          regex: /^Add a description/i,
          key: 'qualityScore.suggestions.addDescription'
        },
        {
          regex: /^Add primary text/i,
          key: 'qualityScore.suggestions.addPrimaryText'
        }
      ]

      for (const pattern of patterns) {
        const match = text.match(pattern.regex)
        if (match) {
          const params = pattern.params ? pattern.params(match) : undefined
          return this.$t(pattern.key, params)
        }
      }

      return text
    }
  }
};
</script>

<style scoped lang="scss">
.quality-score-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.score-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #262626;
  }

  .total-score {
    display: flex;
    align-items: baseline;
    font-weight: 700;

    .score-number {
      font-size: 36px;
    }

    .score-max {
      font-size: 18px;
      color: #8c8c8c;
      margin-left: 4px;
    }

    &.excellent .score-number {
      color: #52c41a;
    }

    &.good .score-number {
      color: #faad14;
    }

    &.average .score-number {
      color: #fa8c16;
    }

    &.poor .score-number {
      color: #f5222d;
    }
  }

  .grade-badge {
    padding: 6px 16px;
    border-radius: 16px;
    font-weight: 600;
    font-size: 14px;

    &.grade-a {
      background: #f6ffed;
      color: #52c41a;
      border: 1px solid #b7eb8f;
    }

    &.grade-b {
      background: #fffbe6;
      color: #faad14;
      border: 1px solid #ffe58f;
    }

    &.grade-c {
      background: #fff7e6;
      color: #fa8c16;
      border: 1px solid #ffd591;
    }

    &.grade-d,
    &.grade-f {
      background: #fff1f0;
      color: #f5222d;
      border: 1px solid #ffa39e;
    }

    &.grade-na {
      background: #f5f5f5;
      color: #8c8c8c;
      border: 1px solid #d9d9d9;
    }
  }
}

.score-breakdown {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .score-item {
    .score-label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .label-text {
        font-size: 14px;
        color: #595959;
        font-weight: 500;
      }

      .score-value {
        font-size: 14px;
        color: #262626;
        font-weight: 600;
      }
    }
  }
}

.suggestions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;

  h4 {
    font-size: 14px;
    font-weight: 600;
    color: #262626;
    margin-bottom: 12px;
  }

  ul {
    margin: 0;
    padding-left: 20px;

    li {
      font-size: 13px;
      color: #595959;
      margin-bottom: 8px;
      line-height: 1.6;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

/* Compact mode styles */
.quality-score-card.compact {
  padding: 12px;
  margin-bottom: 0;
  box-shadow: none;
  background: #fafafa;
  border: 1px solid #f0f0f0;

  .score-header {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;

    .total-score .score-number {
      font-size: 24px;
    }

    .total-score .score-max {
      font-size: 14px;
    }

    .grade-badge {
      padding: 4px 12px;
      font-size: 12px;
    }
  }
}

.compact-suggestion {
  margin-top: 12px;
  padding: 8px;
  background: #e6f7ff;
  border-left: 3px solid #1890ff;
  border-radius: 4px;
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
}
</style>
