<template>
  <a-card title="🎭 Target Persona (Optional)" class="persona-selector-card">
    <template #extra>
      <a-button type="link" size="small" @click="navigateToPersonas">
        <plus-outlined /> Create New
      </a-button>
    </template>

    <!-- Persona Selection -->
    <a-form-item
      label="Select a persona to tailor your ad"
      :validate-status="validationStatus"
      :help="validationMessage"
    >
      <a-select
        v-model:value="selectedPersonaId"
        placeholder="Choose a target persona (optional)"
        :loading="loading"
        allow-clear
        show-search
        :filter-option="filterPersona"
        @change="handlePersonaChange"
      >
        <template #notFoundContent>
          <a-empty description="No personas found">
            <a-button type="primary" size="small" @click="navigateToPersonas">
              Create Your First Persona
            </a-button>
          </a-empty>
        </template>
        <a-select-option
          v-for="persona in personas"
          :key="persona.id"
          :value="persona.id"
        >
          <div class="persona-option">
            <span class="persona-emoji">{{ getGenderEmoji(persona.gender) }}</span>
            <span class="persona-name">{{ persona.name }}</span>
            <a-tag size="small" :color="getToneColor(persona.tone)">
              {{ persona.tone }}
            </a-tag>
          </div>
        </a-select-option>
      </a-select>
    </a-form-item>

    <!-- Selected Persona Preview -->
    <div v-if="selectedPersona" class="selected-persona-preview">
      <a-divider>Persona Preview</a-divider>

      <div class="persona-preview-content">
        <div class="preview-header">
          <div class="preview-avatar">
            <span class="avatar-emoji">{{ getGenderEmoji(selectedPersona.gender) }}</span>
            <span class="avatar-initial">{{ selectedPersona.name[0] }}</span>
          </div>
          <div class="preview-info">
            <h4 class="preview-name">{{ selectedPersona.name }}</h4>
            <div class="preview-tags">
              <a-tag color="blue">{{ selectedPersona.age }} years</a-tag>
              <a-tag :color="getGenderColor(selectedPersona.gender)">
                {{ formatGender(selectedPersona.gender) }}
              </a-tag>
              <a-tag :color="getToneColor(selectedPersona.tone)">
                {{ selectedPersona.tone }}
              </a-tag>
            </div>
          </div>
        </div>

        <!-- Quick Stats -->
        <a-row :gutter="[8, 8]" class="preview-stats">
          <a-col :span="12">
            <div class="stat-item">
              <heart-outlined class="stat-icon" />
              <span class="stat-label">{{ selectedPersona.interests.length }} Interests</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="stat-item">
              <alert-outlined class="stat-icon" />
              <span class="stat-label">{{ selectedPersona.painPoints.length }} Pain Points</span>
            </div>
          </a-col>
        </a-row>

        <!-- Key Interests -->
        <div v-if="selectedPersona.interests.length" class="preview-section">
          <div class="section-label">
            <heart-outlined /> Top Interests
          </div>
          <div class="tag-list">
            <a-tag
              v-for="(interest, index) in selectedPersona.interests.slice(0, 5)"
              :key="index"
              color="cyan"
            >
              {{ interest }}
            </a-tag>
            <a-tag v-if="selectedPersona.interests.length > 5" class="more-tag">
              +{{ selectedPersona.interests.length - 5 }} more
            </a-tag>
          </div>
        </div>

        <!-- Key Pain Points -->
        <div v-if="selectedPersona.painPoints.length" class="preview-section">
          <div class="section-label">
            <alert-outlined /> Main Challenges
          </div>
          <ul class="pain-points-preview">
            <li v-for="(painPoint, index) in selectedPersona.painPoints.slice(0, 3)" :key="index">
              {{ painPoint }}
            </li>
            <li v-if="selectedPersona.painPoints.length > 3" class="more-item">
              +{{ selectedPersona.painPoints.length - 3 }} more...
            </li>
          </ul>
        </div>

        <!-- Desired Outcome -->
        <div v-if="selectedPersona.desiredOutcome" class="preview-section">
          <div class="section-label">
            <rocket-outlined /> Goal
          </div>
          <p class="preview-text">{{ selectedPersona.desiredOutcome }}</p>
        </div>

        <!-- Actions -->
        <div class="preview-actions">
          <a-space>
            <a-button size="small" @click="viewFullPersona">
              <eye-outlined /> View Full Details
            </a-button>
            <a-button size="small" type="text" danger @click="clearSelection">
              <close-outlined /> Clear Selection
            </a-button>
          </a-space>
        </div>
      </div>

      <!-- Prompt Enhancement Info -->
      <a-alert
        type="info"
        show-icon
        class="enhancement-alert"
      >
        <template #message>
          <strong>AI Prompt Enhancement:</strong>
          Your ad prompt will be automatically enriched with this persona's profile to create more targeted content.
        </template>
      </a-alert>
    </div>

    <!-- Empty State Help -->
    <a-alert
      v-else
      type="info"
      show-icon
      class="help-alert"
    >
      <template #message>
        <strong>Tip:</strong> Select a persona to automatically tailor your ad content to specific audience characteristics.
        This helps create more relevant and engaging ads.
      </template>
    </a-alert>
  </a-card>
</template>

<script>
import {
  PlusOutlined,
  HeartOutlined,
  AlertOutlined,
  RocketOutlined,
  EyeOutlined,
  CloseOutlined
} from '@ant-design/icons-vue'
import api from '@/services/api'

export default {
  name: 'PersonaSelector',
  components: {
    PlusOutlined,
    HeartOutlined,
    AlertOutlined,
    RocketOutlined,
    EyeOutlined,
    CloseOutlined
  },
  props: {
    modelValue: {
      type: Number,
      default: null
    }
  },
  emits: ['update:modelValue', 'persona-selected'],
  data() {
    return {
      personas: [],
      selectedPersonaId: this.modelValue,
      loading: false,
      validationStatus: '',
      validationMessage: ''
    }
  },
  computed: {
    selectedPersona() {
      if (!this.selectedPersonaId) return null
      return this.personas.find(p => p.id === this.selectedPersonaId)
    }
  },
  watch: {
    modelValue(newVal) {
      this.selectedPersonaId = newVal
    }
  },
  mounted() {
    this.loadPersonas()
  },
  methods: {
    async loadPersonas() {
      this.loading = true
      try {
        const response = await api.personas.getAll()
        this.personas = response.data

        // If there's a pre-selected persona, validate it exists
        if (this.selectedPersonaId) {
          const exists = this.personas.find(p => p.id === this.selectedPersonaId)
          if (!exists) {
            this.selectedPersonaId = null
            this.$emit('update:modelValue', null)
          }
        }
      } catch (error) {
        this.$message.error('Failed to load personas: ' + error.message)
      } finally {
        this.loading = false
      }
    },
    handlePersonaChange(value) {
      this.$emit('update:modelValue', value)

      const persona = this.personas.find(p => p.id === value)
      this.$emit('persona-selected', persona)

      if (persona) {
        this.$message.success(`Persona "${persona.name}" selected`)
      }
    },
    filterPersona(input, option) {
      const persona = this.personas.find(p => p.id === option.value)
      if (!persona) return false

      const searchText = input.toLowerCase()
      return (
        persona.name.toLowerCase().includes(searchText) ||
        persona.interests.some(i => i.toLowerCase().includes(searchText)) ||
        persona.tone.toLowerCase().includes(searchText)
      )
    },
    clearSelection() {
      this.selectedPersonaId = null
      this.$emit('update:modelValue', null)
      this.$emit('persona-selected', null)
      this.$message.info('Persona selection cleared')
    },
    viewFullPersona() {
      if (this.selectedPersona) {
        // Open in new tab or modal - for now, navigate to personas page
        this.$router.push(`/personas?highlight=${this.selectedPersona.id}`)
      }
    },
    navigateToPersonas() {
      this.$router.push('/personas')
    },
    getGenderEmoji(gender) {
      const emojiMap = {
        MALE: '👨',
        FEMALE: '👩',
        ALL: '👤'
      }
      return emojiMap[gender] || '👤'
    },
    getGenderColor(gender) {
      const colorMap = {
        MALE: 'blue',
        FEMALE: 'pink',
        ALL: 'default'
      }
      return colorMap[gender] || 'default'
    },
    getToneColor(tone) {
      const colorMap = {
        professional: 'blue',
        casual: 'green',
        funny: 'orange',
        inspirational: 'purple'
      }
      return colorMap[tone] || 'default'
    },
    formatGender(gender) {
      const genderMap = {
        MALE: 'Male',
        FEMALE: 'Female',
        ALL: 'All'
      }
      return genderMap[gender] || gender
    }
  }
}
</script>

<style scoped>
.persona-selector-card {
  margin-bottom: 16px;
}

.persona-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.persona-emoji {
  font-size: 16px;
}

.persona-name {
  flex: 1;
  font-weight: 500;
}

.selected-persona-preview {
  margin-top: 16px;
}

.persona-preview-content {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.dark .persona-preview-content {
  background: #1f1f1f;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.preview-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}

.avatar-initial {
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.avatar-emoji {
  position: absolute;
  bottom: -2px;
  right: -2px;
  font-size: 20px;
  background: white;
  border-radius: 50%;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-info {
  flex: 1;
}

.preview-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preview-stats {
  margin: 16px 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  background: white;
  border-radius: 6px;
  font-size: 13px;
}

.dark .stat-item {
  background: #141414;
}

.stat-icon {
  color: #1890ff;
}

.stat-label {
  font-weight: 500;
}

.preview-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e0e0e0;
}

.dark .preview-section {
  border-top-color: #303030;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 13px;
  color: #262626;
}

.dark .section-label {
  color: #e0e0e0;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.more-tag {
  background: #f0f0f0;
  color: #595959;
  border: none;
}

.pain-points-preview {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #595959;
}

.dark .pain-points-preview {
  color: #a0a0a0;
}

.pain-points-preview li {
  margin-bottom: 4px;
}

.more-item {
  color: #8c8c8c;
  font-style: italic;
}

.preview-text {
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin: 0;
}

.dark .preview-text {
  color: #a0a0a0;
}

.preview-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e0e0e0;
}

.dark .preview-actions {
  border-top-color: #303030;
}

.enhancement-alert {
  margin-top: 16px;
}

.help-alert {
  margin-top: 12px;
}

/* Responsive */
@media (max-width: 768px) {
  .preview-header {
    flex-direction: column;
    text-align: center;
  }

  .preview-avatar {
    margin: 0 auto;
  }
}
</style>
