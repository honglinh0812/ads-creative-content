<template>
  <a-card title="🎯 Audience Targeting" class="audience-segment-card">
    <a-form layout="vertical" ref="audienceForm">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            label="Gender"
            :validate-status="validationStatus.gender"
            :help="validationMessages.gender"
          >
            <a-select
              v-model:value="localSegment.gender"
              placeholder="Select target gender"
              @change="handleFieldChange('gender')"
            >
              <a-select-option value="ALL">All</a-select-option>
              <a-select-option value="MALE">Male</a-select-option>
              <a-select-option value="FEMALE">Female</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item
            label="Location"
            :validate-status="validationStatus.location"
            :help="validationMessages.location"
          >
            <a-input
              v-model:value="localSegment.location"
              placeholder="e.g., United States, Vietnam"
              :maxlength="500"
              show-count
              @change="handleFieldChange('location')"
              @blur="validateField('location')"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item
            label="Age Range"
            :validate-status="validationStatus.ageRange"
            :help="validationMessages.ageRange"
          >
            <a-slider
              v-model:value="ageRange"
              range
              :min="13"
              :max="65"
              :marks="ageMarks"
              @change="handleAgeChange"
            />
            <div class="age-display">
              {{ ageRange[0] }} - {{ ageRange[1] }} years old
            </div>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item
            label="Interests (Optional)"
            :validate-status="validationStatus.interests"
            :help="validationMessages.interests"
          >
            <a-input
              v-model:value="localSegment.interests"
              placeholder="e.g., Technology, Fashion, Sports"
              :maxlength="1000"
              show-count
              @change="handleFieldChange('interests')"
              @blur="validateField('interests')"
            />
            <div class="field-hint">Separate multiple interests with commas</div>
          </a-form-item>
        </a-col>
      </a-row>

      <a-alert
        v-if="hasValidationErrors"
        type="error"
        show-icon
        class="validation-alert"
        message="Please fix the validation errors above"
      />

      <a-alert
        v-else-if="localSegment.gender || localSegment.location"
        type="info"
        show-icon
        class="preview-alert"
      >
        <template #message>
          <strong>Targeting Preview:</strong>
          {{ getTargetingPreview() }}
        </template>
      </a-alert>

      <a-alert
        v-if="estimatedReach"
        type="success"
        show-icon
        class="reach-alert"
      >
        <template #message>
          <strong>Estimated Reach:</strong>
          {{ estimatedReach }}
        </template>
      </a-alert>
    </a-form>
  </a-card>
</template>

<script>
export default {
  name: 'AudienceSegmentForm',
  props: {
    modelValue: {
      type: Object,
      default: () => ({
        gender: 'ALL',
        minAge: 18,
        maxAge: 65,
        location: '',
        interests: ''
      })
    }
  },
  emits: ['update:modelValue', 'validation-change'],
  data() {
    return {
      localSegment: { ...this.modelValue },
      ageRange: [this.modelValue.minAge || 18, this.modelValue.maxAge || 65],
      ageMarks: {
        13: '13',
        25: '25',
        45: '45',
        65: '65+'
      },
      validationStatus: {
        gender: '',
        location: '',
        ageRange: '',
        interests: ''
      },
      validationMessages: {
        gender: '',
        location: '',
        ageRange: '',
        interests: ''
      },
      estimatedReach: ''
    };
  },
  computed: {
    hasValidationErrors() {
      return Object.values(this.validationStatus).some(status => status === 'error');
    }
  },
  watch: {
    modelValue: {
      deep: true,
      handler(newVal) {
        this.localSegment = { ...newVal };
        this.ageRange = [newVal.minAge || 18, newVal.maxAge || 65];
      }
    },
    localSegment: {
      deep: true,
      handler() {
        this.calculateEstimatedReach();
      }
    }
  },
  methods: {
    handleAgeChange(value) {
      this.localSegment.minAge = value[0];
      this.localSegment.maxAge = value[1];
      this.validateField('ageRange');
      this.emitChange();
    },
    handleFieldChange(fieldName) {
      this.validateField(fieldName);
      this.emitChange();
    },
    validateField(fieldName) {
      // Reset validation for this field
      this.validationStatus[fieldName] = '';
      this.validationMessages[fieldName] = '';

      switch (fieldName) {
        case 'gender':
          if (!this.localSegment.gender) {
            this.validationStatus.gender = 'error';
            this.validationMessages.gender = 'Please select a gender';
            return false;
          }
          break;

        case 'location':
          if (this.localSegment.location) {
            const location = this.localSegment.location.trim();
            if (location.length < 2) {
              this.validationStatus.location = 'error';
              this.validationMessages.location = 'Location must be at least 2 characters';
              return false;
            }
            if (location.length > 500) {
              this.validationStatus.location = 'error';
              this.validationMessages.location = 'Location must not exceed 500 characters';
              return false;
            }
            // Check for valid characters (letters, spaces, commas, hyphens)
            if (!/^[a-zA-Z\s,-]+$/.test(location)) {
              this.validationStatus.location = 'error';
              this.validationMessages.location = 'Location can only contain letters, spaces, commas, and hyphens';
              return false;
            }
          }
          break;

        case 'ageRange':
          if (this.localSegment.minAge < 13 || this.localSegment.minAge > 65) {
            this.validationStatus.ageRange = 'error';
            this.validationMessages.ageRange = 'Minimum age must be between 13 and 65';
            return false;
          }
          if (this.localSegment.maxAge < 13 || this.localSegment.maxAge > 65) {
            this.validationStatus.ageRange = 'error';
            this.validationMessages.ageRange = 'Maximum age must be between 13 and 65';
            return false;
          }
          if (this.localSegment.minAge > this.localSegment.maxAge) {
            this.validationStatus.ageRange = 'error';
            this.validationMessages.ageRange = 'Minimum age cannot be greater than maximum age';
            return false;
          }
          if (this.localSegment.maxAge - this.localSegment.minAge < 1) {
            this.validationStatus.ageRange = 'warning';
            this.validationMessages.ageRange = 'Age range is very narrow. Consider widening for better reach';
          }
          break;

        case 'interests':
          if (this.localSegment.interests) {
            const interests = this.localSegment.interests.trim();
            if (interests.length > 1000) {
              this.validationStatus.interests = 'error';
              this.validationMessages.interests = 'Interests must not exceed 1000 characters';
              return false;
            }
            // Check if interests are comma-separated
            const interestList = interests.split(',').map(i => i.trim()).filter(i => i);
            if (interestList.length > 25) {
              this.validationStatus.interests = 'warning';
              this.validationMessages.interests = 'Too many interests may limit your reach. Consider focusing on top interests';
            }
            // Validate each interest
            for (const interest of interestList) {
              if (interest.length < 2) {
                this.validationStatus.interests = 'error';
                this.validationMessages.interests = 'Each interest must be at least 2 characters';
                return false;
              }
            }
          }
          break;
      }

      this.emitValidation();
      return true;
    },
    validateAll() {
      const fields = ['gender', 'location', 'ageRange', 'interests'];
      let isValid = true;

      fields.forEach(field => {
        if (!this.validateField(field)) {
          isValid = false;
        }
      });

      return isValid;
    },
    emitChange() {
      this.$emit('update:modelValue', this.localSegment);
      this.emitValidation();
    },
    emitValidation() {
      this.$emit('validation-change', {
        valid: !this.hasValidationErrors,
        data: this.localSegment
      });
    },
    getTargetingPreview() {
      const parts = [];
      if (this.localSegment.gender && this.localSegment.gender !== 'ALL') {
        parts.push(this.localSegment.gender.toLowerCase() + 's');
      }
      parts.push(`aged ${this.ageRange[0]}-${this.ageRange[1]}`);
      if (this.localSegment.location) {
        parts.push(`in ${this.localSegment.location}`);
      }
      if (this.localSegment.interests) {
        parts.push(`interested in ${this.localSegment.interests}`);
      }
      return 'Targeting ' + parts.join(', ');
    },
    calculateEstimatedReach() {
      // Simple estimation based on age range and location
      const ageSpan = this.localSegment.maxAge - this.localSegment.minAge;
      let baseReach = 1000000; // 1M base

      // Adjust by age range
      baseReach = baseReach * (ageSpan / 52); // 52 is max age span (65-13)

      // Adjust by gender
      if (this.localSegment.gender !== 'ALL') {
        baseReach = baseReach * 0.5;
      }

      // Adjust by interests (more specific = smaller reach)
      if (this.localSegment.interests) {
        const interestCount = this.localSegment.interests.split(',').filter(i => i.trim()).length;
        baseReach = baseReach * Math.max(0.1, 1 - (interestCount * 0.05));
      }

      // Format the number
      if (baseReach >= 1000000) {
        this.estimatedReach = `${(baseReach / 1000000).toFixed(1)}M people`;
      } else if (baseReach >= 1000) {
        this.estimatedReach = `${(baseReach / 1000).toFixed(0)}K people`;
      } else {
        this.estimatedReach = `${Math.floor(baseReach)} people`;
      }
    }
  },
  mounted() {
    // Initial validation
    this.validateAll();
    this.calculateEstimatedReach();
  }
};
</script>

<style scoped>
.audience-segment-card {
  margin-bottom: 16px;
}

.age-display {
  text-align: center;
  margin-top: 8px;
  font-weight: 500;
  color: #1890ff;
}

.field-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.validation-alert {
  margin-top: 16px;
}

.preview-alert {
  margin-top: 16px;
}

.reach-alert {
  margin-top: 12px;
}
</style>
