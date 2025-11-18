<template>
  <a-card :title="isEditing ? $t('personas.editPersona') : $t('personas.createPersona')" class="persona-form-card">
    <a-form
      ref="formRef"
      :model="localPersona"
      layout="vertical"
      @finish="handleSubmit"
    >
      <!-- Name -->
      <a-form-item
        :label="$t('personas.name')"
        name="name"
        :rules="[
          { required: true, message: $t('personas.form.validation.nameRequired') },
          { min: 2, max: 100, message: $t('personas.form.validation.nameLength') },
          { pattern: namePattern, message: $t('personas.form.validation.nameInvalid') }
        ]"
      >
        <a-input
          v-model:value="localPersona.name"
          :placeholder="$t('personas.form.namePlaceholder')"
          :maxlength="100"
          show-count
        />
      </a-form-item>

      <!-- Age and Gender Row -->
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item
            :label="$t('personas.age')"
            name="age"
            :rules="[
              { required: true, message: $t('personas.form.validation.ageRequired') },
              { type: 'number', min: 13, max: 120, message: $t('personas.form.validation.ageRange') }
            ]"
          >
            <a-input-number
              v-model:value="localPersona.age"
              :min="13"
              :max="120"
              :placeholder="$t('personas.form.agePlaceholder')"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="12">
          <a-form-item
            :label="$t('personas.gender')"
            name="gender"
            :rules="[{ required: true, message: $t('personas.form.validation.genderRequired') }]"
          >
            <a-select v-model:value="localPersona.gender" :placeholder="$t('personas.form.genderPlaceholder')">
              <a-select-option value="MALE">{{ $t('personas.male') }}</a-select-option>
              <a-select-option value="FEMALE">{{ $t('personas.female') }}</a-select-option>
              <a-select-option value="ALL">{{ $t('personas.all') }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- Interests -->
      <a-form-item
        :label="$t('personas.interests')"
        name="interests"
        :rules="[
          { required: true, message: $t('personas.form.validation.interestsRequired') },
          { validator: validateInterests }
        ]"
      >
        <template #extra>
          <span class="field-hint">{{ $t('personas.form.interestsHint') }}</span>
        </template>
        <a-select
          v-model:value="localPersona.interests"
          mode="tags"
          :placeholder="$t('personas.form.interestsPlaceholder')"
          :max-tag-count="5"
          :max-tag-text-length="20"
        />
      </a-form-item>

      <!-- Tone -->
      <a-form-item
        :label="$t('personas.tone')"
        name="tone"
        :rules="[{ required: true, message: $t('personas.form.validation.toneRequired') }]"
      >
        <a-radio-group v-model:value="localPersona.tone">
          <a-radio value="professional">
            <span class="tone-option">
              💼 {{ $t('personas.toneLabels.professional') }}
              <small>{{ $t('personas.toneDescriptions.professional') }}</small>
            </span>
          </a-radio>
          <a-radio value="casual">
            <span class="tone-option">
              😊 {{ $t('personas.toneLabels.casual') }}
              <small>{{ $t('personas.toneDescriptions.casual') }}</small>
            </span>
          </a-radio>
          <a-radio value="funny">
            <span class="tone-option">
              😄 {{ $t('personas.toneLabels.funny') }}
              <small>{{ $t('personas.toneDescriptions.funny') }}</small>
            </span>
          </a-radio>
          <a-radio value="friendly">
            <span class="tone-option">
              🤝 {{ $t('personas.toneLabels.friendly') }}
              <small>{{ $t('personas.toneDescriptions.friendly') }}</small>
            </span>
          </a-radio>
          <a-radio value="formal">
            <span class="tone-option">
              🏛️ {{ $t('personas.toneLabels.formal') }}
              <small>{{ $t('personas.toneDescriptions.formal') }}</small>
            </span>
          </a-radio>
          <a-radio value="enthusiastic">
            <span class="tone-option">
              🚀 {{ $t('personas.toneLabels.enthusiastic') }}
              <small>{{ $t('personas.toneDescriptions.enthusiastic') }}</small>
            </span>
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- Pain Points -->
      <a-form-item
        :label="$t('personas.painPoints')"
        name="painPoints"
        :rules="[
          { required: true, message: $t('personas.form.validation.painPointsRequired') },
          { validator: validatePainPoints }
        ]"
      >
        <template #extra>
          <span class="field-hint">{{ $t('personas.form.painPointsHint') }}</span>
        </template>
        <a-select
          v-model:value="localPersona.painPoints"
          mode="tags"
          :placeholder="$t('personas.form.painPointsPlaceholder')"
          :max-tag-count="5"
          :max-tag-text-length="50"
        />
      </a-form-item>

      <!-- Desired Outcome -->
      <a-form-item
        :label="$t('personas.desiredOutcome')"
        name="desiredOutcome"
      >
        <template #extra>
          <span class="field-hint">{{ $t('personas.form.desiredOutcomeHint') }}</span>
        </template>
        <a-textarea
          v-model:value="localPersona.desiredOutcome"
          :placeholder="$t('personas.form.desiredOutcomePlaceholder')"
          :maxlength="500"
          :rows="3"
          show-count
        />
      </a-form-item>

      <!-- Description -->
      <a-form-item
        :label="$t('personas.description')"
        name="description"
      >
        <template #extra>
          <span class="field-hint">{{ $t('personas.form.descriptionHint') }}</span>
        </template>
        <a-textarea
          v-model:value="localPersona.description"
          :placeholder="$t('personas.form.descriptionPlaceholder')"
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
            {{ isEditing ? $t('personas.actions.update') : $t('personas.actions.create') }}
          </a-button>
          <a-button @click="handleCancel">
            {{ $t('personas.actions.cancel') }}
          </a-button>
          <a-button v-if="isEditing" type="text" danger @click="handleReset">
            {{ $t('personas.actions.reset') }}
          </a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <!-- Preview Card -->
    <a-divider>{{ $t('personas.form.preview') }}</a-divider>
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
      originalPersona: null,
      namePattern: /^[\p{L}\p{M}0-9\s\-_'.]+$/u
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
        return Promise.reject(this.$t('personas.form.validation.interestsRequired'))
      }
      if (value.length > 20) {
        return Promise.reject(this.$t('personas.form.validation.interestsCount'))
      }
      // Check individual interest length
      for (const interest of value) {
        if (interest.trim().length < 2) {
          return Promise.reject(this.$t('personas.form.validation.interestTooShort'))
        }
        if (interest.trim().length > 50) {
          return Promise.reject(this.$t('personas.form.validation.interestTooLong'))
        }
      }
      return Promise.resolve()
    },
    validatePainPoints(rule, value) {
      if (!value || value.length === 0) {
        return Promise.reject(this.$t('personas.form.validation.painPointsRequired'))
      }
      if (value.length > 10) {
        return Promise.reject(this.$t('personas.form.validation.painPointsCount'))
      }
      // Check individual pain point length
      for (const painPoint of value) {
        if (painPoint.trim().length < 2) {
          return Promise.reject(this.$t('personas.form.validation.painPointTooShort'))
        }
        if (painPoint.trim().length > 200) {
          return Promise.reject(this.$t('personas.form.validation.painPointTooLong'))
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
        this.$message.error(this.$t('personas.form.validation.fixErrors'))
      })
    },
    handleCancel() {
      this.$emit('cancel')
    },
    handleReset() {
      if (this.originalPersona) {
        this.localPersona = { ...this.originalPersona }
        this.$message.info(this.$t('personas.form.validation.reset'))
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
