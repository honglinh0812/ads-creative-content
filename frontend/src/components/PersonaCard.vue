<template>
  <a-card
    :hoverable="!previewMode"
    class="persona-card"
    :class="{ 'preview-mode': previewMode, selected: isSelected }"
  >
    <!-- Card Header with Avatar -->
    <template #cover>
      <div class="persona-cover" :style="{ background: getGradient() }">
        <div class="persona-avatar">
          <span class="avatar-initial">{{ getInitial() }}</span>
          <span class="avatar-emoji">{{ getGenderEmoji() }}</span>
        </div>
      </div>
    </template>

    <!-- Card Body -->
    <a-card-meta :title="persona.name">
      <template #description>
        <div class="persona-basic-info">
          <a-tag color="blue">{{ $t('personas.card.age', { age: persona.age }) }}</a-tag>
          <a-tag :color="getGenderColor()">{{ formatGender(persona.gender) }}</a-tag>
          <a-tag color="purple">{{ formatTone(persona.tone) }}</a-tag>
        </div>
      </template>
    </a-card-meta>

    <!-- Interests Section -->
    <div v-if="persona.interests && persona.interests.length" class="persona-section">
      <div class="section-title">
        <heart-outlined /> {{ $t('personas.interests') }}
      </div>
      <div class="tag-container">
        <a-tag
          v-for="(interest, index) in displayedInterests"
          :key="index"
          color="cyan"
          class="interest-tag"
        >
          {{ interest }}
        </a-tag>
        <a-tag v-if="hasMoreInterests" class="more-tag">
          {{ $t('personas.card.more', { count: persona.interests.length - maxDisplayItems }) }}
        </a-tag>
      </div>
    </div>

    <!-- Pain Points Section -->
    <div v-if="persona.painPoints && persona.painPoints.length" class="persona-section">
      <div class="section-title">
        <alert-outlined /> {{ $t('personas.painPoints') }}
      </div>
      <ul class="pain-points-list">
        <li v-for="(painPoint, index) in displayedPainPoints" :key="index">
          {{ painPoint }}
        </li>
        <li v-if="hasMorePainPoints" class="more-item">
          {{ $t('personas.card.moreEllipsis', { count: persona.painPoints.length - maxDisplayItems }) }}
        </li>
      </ul>
    </div>

    <!-- Desired Outcome -->
    <div v-if="persona.desiredOutcome" class="persona-section">
      <div class="section-title">
        <rocket-outlined /> {{ $t('personas.goal') }}
      </div>
      <p class="desired-outcome">{{ persona.desiredOutcome }}</p>
    </div>

    <!-- Description -->
    <div v-if="persona.description && showFullDetails" class="persona-section">
      <div class="section-title">
        <file-text-outlined /> {{ $t('personas.description') }}
      </div>
      <p class="description">{{ persona.description }}</p>
    </div>

    <!-- Card Actions -->
    <template v-if="!previewMode" #actions>
      <a-tooltip :title="$t('personas.actions.view')">
        <eye-outlined key="view" @click="handleView" />
      </a-tooltip>
      <a-tooltip :title="$t('personas.actions.edit')">
        <edit-outlined key="edit" @click="handleEdit" />
      </a-tooltip>
      <a-tooltip :title="$t('personas.selectForAd')">
        <check-circle-outlined
          key="select"
          :class="{ 'selected-icon': isSelected }"
          @click="handleSelect"
        />
      </a-tooltip>
      <a-popconfirm
        :title="$t('personas.deleteConfirm')"
        :ok-text="$t('common.action.yes')"
        :cancel-text="$t('common.action.no')"
        @confirm="handleDelete"
      >
        <a-tooltip :title="$t('personas.actions.delete')">
          <delete-outlined key="delete" class="delete-action" />
        </a-tooltip>
      </a-popconfirm>
    </template>

    <!-- Created Date Footer -->
    <template v-if="!previewMode" #extra>
      <span class="created-date">
        <clock-circle-outlined />
        {{ formatDate(persona.createdAt) }}
      </span>
    </template>
  </a-card>
</template>

<script>
import {
  HeartOutlined,
  AlertOutlined,
  RocketOutlined,
  FileTextOutlined,
  EyeOutlined,
  EditOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'

export default {
  name: 'PersonaCard',
  components: {
    HeartOutlined,
    AlertOutlined,
    RocketOutlined,
    FileTextOutlined,
    EyeOutlined,
    EditOutlined,
    CheckCircleOutlined,
    DeleteOutlined,
    ClockCircleOutlined
  },
  props: {
    persona: {
      type: Object,
      required: true
    },
    previewMode: {
      type: Boolean,
      default: false
    },
    isSelected: {
      type: Boolean,
      default: false
    },
    showFullDetails: {
      type: Boolean,
      default: false
    },
    maxDisplayItems: {
      type: Number,
      default: 3
    }
  },
  emits: ['view', 'edit', 'select', 'delete'],
  computed: {
    displayedInterests() {
      return this.persona.interests.slice(0, this.maxDisplayItems)
    },
    hasMoreInterests() {
      return this.persona.interests.length > this.maxDisplayItems
    },
    displayedPainPoints() {
      return this.persona.painPoints.slice(0, this.maxDisplayItems)
    },
    hasMorePainPoints() {
      return this.persona.painPoints.length > this.maxDisplayItems
    }
  },
  methods: {
    getInitial() {
      return this.persona.name ? this.persona.name[0].toUpperCase() : '?'
    },
    getGenderEmoji() {
      const emojiMap = {
        MALE: '👨',
        FEMALE: '👩',
        ALL: '👤'
      }
      return emojiMap[this.persona.gender] || '👤'
    },
    getGenderColor() {
      const colorMap = {
        MALE: 'blue',
        FEMALE: 'pink',
        ALL: 'default'
      }
      return colorMap[this.persona.gender] || 'default'
    },
    getGradient() {
      // Generate gradient based on name hash for consistency
      const hash = this.hashCode(this.persona.name || '')
      const gradients = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
        'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
        'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
      ]
      return gradients[Math.abs(hash) % gradients.length]
    },
    hashCode(str) {
      let hash = 0
      for (let i = 0; i < str.length; i++) {
        const char = str.charCodeAt(i)
        hash = ((hash << 5) - hash) + char
        hash = hash & hash
      }
      return hash
    },
    formatGender(gender) {
      const genderMap = {
        MALE: this.$t('personas.male'),
        FEMALE: this.$t('personas.female'),
        ALL: this.$t('personas.all')
      }
      return genderMap[gender] || gender
    },
    formatTone(tone) {
      const toneKey = tone ? tone.toLowerCase() : ''
      return toneKey ? this.$t(`personas.toneLabels.${toneKey}`, tone) : ''
    },
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      const now = new Date()
      const diffTime = Math.abs(now - date)
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

      if (diffDays === 0) return this.$t('personas.card.date.today')
      if (diffDays === 1) return this.$t('personas.card.date.yesterday')
      if (diffDays < 7) return this.$t('personas.card.date.daysAgo', { count: diffDays })
      if (diffDays < 30) return this.$t('personas.card.date.weeksAgo', { count: Math.floor(diffDays / 7) })
      if (diffDays < 365) return this.$t('personas.card.date.monthsAgo', { count: Math.floor(diffDays / 30) })
      return date.toLocaleDateString()
    },
    handleView() {
      this.$emit('view', this.persona)
    },
    handleEdit() {
      this.$emit('edit', this.persona)
    },
    handleSelect() {
      this.$emit('select', this.persona)
    },
    handleDelete() {
      this.$emit('delete', this.persona)
    }
  }
}
</script>

<style scoped>
.persona-card {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.persona-card:hover:not(.preview-mode) {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.persona-card.selected {
  border: 2px solid #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1);
}

.persona-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.persona-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
  color: #1890ff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  position: relative;
}

.avatar-initial {
  font-size: 32px;
  font-weight: 600;
}

.avatar-emoji {
  position: absolute;
  bottom: -5px;
  right: -5px;
  font-size: 24px;
  background: white;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.persona-basic-info {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.persona-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.dark .persona-section {
  border-top-color: #303030;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 8px;
  font-size: 13px;
}

.dark .section-title {
  color: #e0e0e0;
}

.tag-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.interest-tag {
  font-size: 12px;
}

.more-tag {
  background: #f0f0f0;
  color: #595959;
  border: none;
  font-size: 12px;
}

.pain-points-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #595959;
}

.dark .pain-points-list {
  color: #a0a0a0;
}

.pain-points-list li {
  margin-bottom: 4px;
}

.more-item {
  color: #8c8c8c;
  font-style: italic;
}

.desired-outcome,
.description {
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin: 0;
}

.dark .desired-outcome,
.dark .description {
  color: #a0a0a0;
}

.created-date {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.delete-action {
  color: #ff4d4f;
}

.selected-icon {
  color: #1890ff;
}

/* Responsive */
@media (max-width: 768px) {
  .persona-avatar {
    width: 60px;
    height: 60px;
  }

  .avatar-initial {
    font-size: 24px;
  }

  .avatar-emoji {
    width: 24px;
    height: 24px;
    font-size: 18px;
  }
}
</style>
