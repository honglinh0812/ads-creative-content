<template>
  <a-card :title="isEditing ? 'Edit Persona' : 'Create New Persona'" class="persona-form-card">
    <a-form
      ref="formRef"
      :model="localPersona"
      layout="vertical"
      @finish="handleSubmit"
    >
      <!-- Name -->
      <a-form-item
        label="Persona Name"
        name="name"
        :rules="[
          { required: true, message: 'Please enter a persona name' },
          { min: 2, max: 100, message: 'Name must be between 2-100 characters' },
          { pattern: /^[a-zA-Z0-9\s\-_'.]+$/, message: 'Only letters, numbers, spaces, hyphens, underscores, and apostrophes allowed' }
        ]"
      >
        <a-input
          v-model:value="localPersona.name"
          placeholder="e.g., Eco-Conscious Millennial"
          :maxlength="100"
          show-count
        />
      </a-form-item>

      <!-- Age and Gender Row -->
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item
            label="Age"
            name="age"
            :rules="[
              { required: true, message: 'Please enter age' },
              { type: 'number', min: 13, max: 120, message: 'Age must be between 13-120' }
            ]"
          >
            <a-input-number
              v-model:value="localPersona.age"
              :min="13"
              :max="120"
              placeholder="e.g., 28"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="12">
          <a-form-item
            label="Gender"
            name="gender"
            :rules="[{ required: true, message: 'Please select gender' }]"
          >
            <a-select v-model:value="localPersona.gender" placeholder="Select gender">
              <a-select-option value="MALE">Male</a-select-option>
              <a-select-option value="FEMALE">Female</a-select-option>
              <a-select-option value="ALL">All / Other</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- Interests -->
      <a-form-item
        label="Interests"
        name="interests"
        :rules="[
          { required: true, message: 'Please add at least one interest' },
          { validator: validateInterests }
        ]"
      >
        <template #extra>
          <span class="field-hint">Add up to 20 interests (press Enter or use commas to separate)</span>
        </template>
        <a-select
          v-model:value="localPersona.interests"
          mode="tags"
          placeholder="e.g., Sustainability, Fitness, Outdoor Activities"
          :max-tag-count="5"
          :max-tag-text-length="20"
        />
      </a-form-item>

      <!-- Tone -->
      <a-form-item
        label="Preferred Communication Tone"
        name="tone"
        :rules="[{ required: true, message: 'Please select a tone' }]"
      >
        <a-radio-group v-model:value="localPersona.tone">
          <a-radio value="professional">
            <span class="tone-option">
              💼 Professional
              <small>Formal and business-oriented</small>
            </span>
          </a-radio>
          <a-radio value="casual">
            <span class="tone-option">
              😊 Casual
              <small>Friendly and conversational</small>
            </span>
          </a-radio>
          <a-radio value="funny">
            <span class="tone-option">
              😄 Funny
              <small>Humorous and lighthearted</small>
            </span>
          </a-radio>
          <a-radio value="inspirational">
            <span class="tone-option">
              ✨ Inspirational
              <small>Motivating and uplifting</small>
            </span>
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- Pain Points -->
      <a-form-item
        label="Pain Points"
        name="painPoints"
        :rules="[
          { required: true, message: 'Please add at least one pain point' },
          { validator: validatePainPoints }
        ]"
      >
        <template #extra>
          <span class="field-hint">Add up to 10 pain points or challenges this persona faces</span>
        </template>
        <a-select
          v-model:value="localPersona.painPoints"
          mode="tags"
          placeholder="e.g., Plastic waste concerns, Want affordable sustainable products"
          :max-tag-count="5"
          :max-tag-text-length="50"
        />
      </a-form-item>

      <!-- Desired Outcome -->
      <a-form-item
        label="Desired Outcome"
        name="desiredOutcome"
      >
        <template #extra>
          <span class="field-hint">What does this persona want to achieve? (Optional)</span>
        </template>
        <a-textarea
          v-model:value="localPersona.desiredOutcome"
          placeholder="e.g., Reduce environmental impact while staying active"
          :maxlength="500"
          :rows="3"
          show-count
        />
      </a-form-item>

      <!-- Description -->
      <a-form-item
        label="Description"
        name="description"
      >
        <template #extra>
          <span class="field-hint">Additional context about this persona (Optional)</span>
        </template>
        <a-textarea
          v-model:value="localPersona.description"
          placeholder="e.g., Urban professional who values sustainability and wellness..."
          :maxlength="1000"
          :rows="4"
          show-count
        />
      </a-form-item>

      <!-- Form Actions -->
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit" :loading="loading">
            <template #icon><save-outlined /></template>
            {{ isEditing ? 'Update Persona' : 'Create Persona' }}
          </a-button>
          <a-button @click="handleCancel">
            Cancel
          </a-button>
          <a-button v-if="isEditing" type="text" danger @click="handleReset">
            Reset to Original
          </a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <!-- Preview Card -->
    <a-divider>Preview</a-divider>
    <div class="persona-preview">
      <PersonaCard :persona="previewPersona" preview-mode />
    </div>
  </a-card>
</template>

<script>
import { SaveOutlined } from '@ant-design/icons-vue'
import PersonaCard from './PersonaCard.vue'

export default {
  name: 'PersonaForm',
  components: {
    SaveOutlined,
    PersonaCard
  },
  props: {
    persona: {
      type: Object,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['submit', 'cancel'],
  data() {
    return {
      localPersona: this.getInitialPersona(),
      originalPersona: null
    }
  },
  computed: {
    isEditing() {
      return !!this.persona?.id
    },
    previewPersona() {
      return {
        ...this.localPersona,
        id: this.localPersona.id || 0,
        createdAt: this.localPersona.createdAt || new Date().toISOString()
      }
    }
  },
  watch: {
    persona: {
      deep: true,
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.localPersona = { ...newVal }
          this.originalPersona = { ...newVal }
        }
      }
    }
  },
  methods: {
    getInitialPersona() {
      return this.persona ? { ...this.persona } : {
        name: '',
        age: 25,
        gender: 'ALL',
        interests: [],
        tone: 'casual',
        painPoints: [],
        desiredOutcome: '',
        description: ''
      }
    },
    validateInterests(rule, value) {
      if (!value || value.length === 0) {
        return Promise.reject('Please add at least one interest')
      }
      if (value.length > 20) {
        return Promise.reject('Maximum 20 interests allowed')
      }
      // Check individual interest length
      for (const interest of value) {
        if (interest.trim().length < 2) {
          return Promise.reject('Each interest must be at least 2 characters')
        }
        if (interest.trim().length > 50) {
          return Promise.reject('Each interest must not exceed 50 characters')
        }
      }
      return Promise.resolve()
    },
    validatePainPoints(rule, value) {
      if (!value || value.length === 0) {
        return Promise.reject('Please add at least one pain point')
      }
      if (value.length > 10) {
        return Promise.reject('Maximum 10 pain points allowed')
      }
      // Check individual pain point length
      for (const painPoint of value) {
        if (painPoint.trim().length < 2) {
          return Promise.reject('Each pain point must be at least 2 characters')
        }
        if (painPoint.trim().length > 200) {
          return Promise.reject('Each pain point must not exceed 200 characters')
        }
      }
      return Promise.resolve()
    },
    handleSubmit() {
      this.$refs.formRef.validate().then(() => {
        // Clean and sanitize data
        const sanitizedPersona = {
          ...this.localPersona,
          interests: this.localPersona.interests.map(i => i.trim()).filter(i => i),
          painPoints: this.localPersona.painPoints.map(p => p.trim()).filter(p => p),
          desiredOutcome: this.localPersona.desiredOutcome?.trim() || null,
          description: this.localPersona.description?.trim() || null
        }
        this.$emit('submit', sanitizedPersona)
      }).catch(() => {
        this.$message.error('Please fix validation errors before submitting')
      })
    },
    handleCancel() {
      this.$emit('cancel')
    },
    handleReset() {
      if (this.originalPersona) {
        this.localPersona = { ...this.originalPersona }
        this.$message.info('Form reset to original values')
      }
    }
  }
}
</script>

<style scoped>
.persona-form-card {
  max-width: 900px;
  margin: 0 auto;
}

.field-hint {
  font-size: 12px;
  color: #8c8c8c;
}

.tone-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tone-option small {
  color: #8c8c8c;
  font-size: 11px;
}

.persona-preview {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

/* Dark mode support */
.dark .persona-preview {
  background: #1f1f1f;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .tone-option {
    font-size: 12px;
  }
}
</style>
