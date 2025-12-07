<template>
  <div class="personas-view">
    <section class="hero-card surface-card">
      <div class="hero-text">
        <h1>{{ $t('personas.title') }}</h1>
        <p class="hero-description">{{ $t('personas.subtitle') }}</p>
        <div class="hero-actions">     
          <a-button type="primary" size="large" @click="showCreateModal = true">
            <template #icon><plus-outlined /></template>
            {{ $t('personas.createPersona') }}
          </a-button>
        </div>
      </div>
    </section>

    <section class="surface-card section-card">
      <div class="section-heading">
        <div>
          <h2>{{ $t('personas.sections.listTitle') }}</h2>
        </div>
        <div class="section-controls">
          <a-select
            v-model:value="sortBy"
            style="width: 180px"
            @change="handleSortChange"
          >
            <a-select-option value="createdAt,desc">{{ $t('personas.sortNewest') }}</a-select-option>
            <a-select-option value="createdAt,asc">{{ $t('personas.sortOldest') }}</a-select-option>
            <a-select-option value="name,asc">{{ $t('personas.sortNameAZ') }}</a-select-option>
            <a-select-option value="name,desc">{{ $t('personas.sortNameZA') }}</a-select-option>
          </a-select>
          <a-radio-group v-model:value="viewMode" button-style="solid">
            <a-radio-button value="grid">
              <appstore-outlined />
            </a-radio-button>
            <a-radio-button value="list">
              <unordered-list-outlined />
            </a-radio-button>
          </a-radio-group>
        </div>
      </div>

      <!-- Empty State -->
      <a-empty
        v-if="!loading && personas.length === 0"
        :description="$t('personas.noPersonas')"
      >
        <template #image>
          <span style="font-size: 64px">👥</span>
        </template>
        <a-button type="primary" @click="showCreateModal = true">
          {{ $t('personas.createFirst') }}
        </a-button>
      </a-empty>

      <!-- Grid View -->
      <a-row v-else-if="viewMode === 'grid'" :gutter="[16, 16]">
        <a-col
          v-for="persona in paginatedPersonas"
          :key="persona.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <PersonaCard
            :persona="persona"
            @view="handleViewPersona"
            @edit="handleEditPersona"
            @select="handleSelectPersona"
            @delete="handleDeletePersona"
          />
        </a-col>
      </a-row>

      <!-- List View -->
      <a-list
        v-else
        :data-source="paginatedPersonas"
        item-layout="horizontal"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <template #actions>
              <a-button size="small" @click="handleViewPersona(item)">
                <eye-outlined />
              </a-button>
              <a-button size="small" @click="handleEditPersona(item)">
                <edit-outlined />
              </a-button>
              <a-button size="small" type="primary" @click="handleSelectPersona(item)">
                <check-outlined />
              </a-button>
              <a-popconfirm
                :title="$t('personas.deleteConfirm')"
                @confirm="handleDeletePersona(item)"
              >
                <a-button size="small" danger>
                  <delete-outlined />
                </a-button>
              </a-popconfirm>
            </template>
            <a-list-item-meta>
              <template #avatar>
                <a-avatar :style="{ background: getRandomColor(item.name) }">
                  {{ item.name[0] }}
                </a-avatar>
              </template>
              <template #title>
                {{ item.name }}
              </template>
              <template #description>
                <a-space>
                  <a-tag>{{ $t('personas.card.age', { age: item.age }) }}</a-tag>
                  <a-tag>{{ formatGender(item.gender) }}</a-tag>
                  <a-tag>{{ formatTone(item.tone) }}</a-tag>
                  <span class="list-interests">
                    {{ item.interests.slice(0, 3).join(', ') }}
                    <span v-if="item.interests.length > 3">...</span>
                  </span>
                </a-space>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>

      <!-- Pagination -->
      <div v-if="filteredPersonas.length > pageSize" class="pagination-container">
        <a-pagination
          v-model:current="currentPage"
          v-model:page-size="pageSize"
          :total="filteredPersonas.length"
          :show-size-changer="true"
          :show-total="total => $t('personas.paginationTotal', { total })"
          :page-size-options="['12', '24', '48', '96']"
        />
      </div>
    </section>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:visible="showCreateModal"
      :title="editingPersona ? $t('personas.editPersona') : $t('personas.createPersona')"
      :footer="null"
      :width="900"
      :destroy-on-close="true"
      wrap-class-name="surface-modal"
      :body-style="{ padding: 0 }"
    >
      <div class="modal-content">
        <PersonaForm
          :persona="editingPersona"
          :loading="submitting"
          @submit="handleSubmitPersona"
          @cancel="handleCancelForm"
        />
      </div>
    </a-modal>

    <!-- View Details Modal -->
    <a-modal
      v-model:visible="showViewModal"
      :title="$t('personas.viewPersona')"
      :footer="null"
      :width="700"
      wrap-class-name="surface-modal"
      :body-style="{ padding: 0 }"
    >
      <div class="modal-content">
        <PersonaCard
          v-if="viewingPersona"
          :persona="viewingPersona"
          :show-full-details="true"
          :max-display-items="100"
          preview-mode
        />
        <div class="modal-actions">
          <a-button @click="showViewModal = false">{{ $t('personas.actions.close') }}</a-button>
          <a-button type="primary" @click="handleEditFromView">
            <edit-outlined /> {{ $t('personas.actions.edit') }}
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import {
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckOutlined,
  AppstoreOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import PersonaCard from '@/components/PersonaCard.vue'
import PersonaForm from '@/components/PersonaForm.vue'
import api from '@/services/api'
import { sanitizePromptInput } from '@/utils/promptSanitizer'

export default {
  name: 'PersonasView',
  components: {
    PlusOutlined,
    EyeOutlined,
    EditOutlined,
    DeleteOutlined,
    CheckOutlined,
    AppstoreOutlined,
    UnorderedListOutlined,
    PersonaCard,
    PersonaForm
  },
  data() {
    return {
      personas: [],
      filteredPersonas: [],
      loading: false,
      submitting: false,
      showCreateModal: false,
      showViewModal: false,
      editingPersona: null,
      viewingPersona: null,
      searchQuery: '',
      sortBy: 'createdAt,desc',
      viewMode: 'grid',
      currentPage: 1,
      pageSize: 12
    }
  },
  computed: {
    paginatedPersonas() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredPersonas.slice(start, end)
    },
    recentPersonasCount() {
      const sevenDaysAgo = new Date()
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
      return this.personas.filter(p => new Date(p.createdAt) >= sevenDaysAgo).length
    },
    mostCommonToneKey() {
      if (this.personas.length === 0) return null
      const toneCounts = {}
      this.personas.forEach(p => {
        toneCounts[p.tone] = (toneCounts[p.tone] || 0) + 1
      })
      const maxTone = Object.keys(toneCounts).reduce((a, b) =>
        toneCounts[a] > toneCounts[b] ? a : b
      )
      return maxTone
    },
    mostCommonTone() {
      if (!this.mostCommonToneKey) return this.$t('personas.stats.notAvailable')
      return this.formatTone(this.mostCommonToneKey)
    },
    toneEmoji() {
      const emojiMap = {
        professional: '💼',
        casual: '😊',
        funny: '😄',
        inspirational: '✨',
        friendly: '🤝',
        formal: '🏛️',
        enthusiastic: '🚀'
      }
      return emojiMap[this.mostCommonToneKey] || '📝'
    },
    quotaPercentage() {
      return Math.round((this.personas.length / 100) * 100)
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
        this.filteredPersonas = [...this.personas]
        this.applySorting()
      } catch (error) {
        this.$message.error(this.$t('personas.messages.loadError', { error: error.message }))
      } finally {
        this.loading = false
      }
    },
    handleSearch(value) {
      if (!value) {
        this.filteredPersonas = [...this.personas]
      } else {
        const query = value.toLowerCase()
        this.filteredPersonas = this.personas.filter(p =>
          p.name.toLowerCase().includes(query) ||
          p.interests.some(i => i.toLowerCase().includes(query)) ||
          p.painPoints.some(pp => pp.toLowerCase().includes(query))
        )
      }
      this.currentPage = 1
    },
    handleSortChange() {
      this.applySorting()
    },
    applySorting() {
      const [field, order] = this.sortBy.split(',')
      this.filteredPersonas.sort((a, b) => {
        let comparison = 0
        if (field === 'name') {
          comparison = a.name.localeCompare(b.name)
        } else if (field === 'createdAt') {
          comparison = new Date(a.createdAt) - new Date(b.createdAt)
        }
        return order === 'desc' ? -comparison : comparison
      })
    },
    handleViewPersona(persona) {
      this.viewingPersona = persona
      this.showViewModal = true
    },
    handleEditPersona(persona) {
      this.editingPersona = { ...persona }
      this.showCreateModal = true
    },
    handleEditFromView() {
      this.showViewModal = false
      this.handleEditPersona(this.viewingPersona)
    },
    handleSelectPersona(persona) {
      // Store selected persona and navigate to ad creation
      this.$store.commit('adCreation/UPDATE_FORM_DATA', { personaId: persona.id })
      this.$router.push('/ads/create')
      this.$message.success(this.$t('personas.messages.selectedForAd', { name: persona.name }))
    },
    async handleDeletePersona(persona) {
      try {
        await api.personas.delete(persona.id)
        this.$message.success(this.$t('personas.deletedSuccess'))
        this.loadPersonas()
      } catch (error) {
        this.$message.error(this.$t('personas.messages.deleteError', { error: error.message }))
      }
    },
    sanitizePersonaPayload(data) {
      if (!data || typeof data !== 'object') {
        return data
      }
      const safePayload = { ...data }
      const sanitizeField = value => (value ? sanitizePromptInput(value) : value)
      const sanitizeArray = list => Array.isArray(list)
        ? list
            .map(item => sanitizeField(item))
            .filter(item => item && item.length)
        : []
      safePayload.name = sanitizeField(data.name)
      safePayload.description = sanitizeField(data.description)
      safePayload.desiredOutcome = sanitizeField(data.desiredOutcome)
      safePayload.interests = sanitizeArray(data.interests)
      safePayload.painPoints = sanitizeArray(data.painPoints)
      return safePayload
    },
    async handleSubmitPersona(personaData) {
      this.submitting = true
      try {
        const sanitizedPayload = this.sanitizePersonaPayload(personaData)
        if (this.editingPersona?.id) {
          await api.personas.update(this.editingPersona.id, sanitizedPayload)
          this.$message.success(this.$t('personas.updatedSuccess'))
        } else {
          await api.personas.create(sanitizedPayload)
          this.$message.success(this.$t('personas.createdSuccess'))
        }
        this.showCreateModal = false
        this.editingPersona = null
        this.loadPersonas()
      } catch (error) {
        this.$message.error(this.$t('personas.messages.saveError', { error: error.message }))
      } finally {
        this.submitting = false
      }
    },
    handleCancelForm() {
      this.showCreateModal = false
      this.editingPersona = null
    },
    getRandomColor(str) {
      const colors = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#87d068', '#ff85c0']
      let hash = 0
      for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
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
      if (!tone) return ''
      const toneKey = tone.toLowerCase()
      const labelMap = {
        professional: this.$t('personas.toneLabels.professional'),
        casual: this.$t('personas.toneLabels.casual'),
        funny: this.$t('personas.toneLabels.funny'),
        friendly: this.$t('personas.toneLabels.friendly'),
        formal: this.$t('personas.toneLabels.formal'),
        inspirational: this.$t('personas.toneLabels.inspirational'),
        enthusiastic: this.$t('personas.toneLabels.enthusiastic')
      }
      return labelMap[toneKey] || tone
    }
  }
}
</script>

<style scoped>
.personas-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.surface-card {
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 32px;
  padding: 32px;
  margin-bottom: 24px;
}

.hero-text h1 {
  margin: 8px 0 12px;
  font-size: 28px;
  color: #0f172a;
}

.eyebrow {
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.hero-description {
  margin: 0 0 20px;
  color: #475569;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-actions :deep(.ant-input-search) {
  min-width: 240px;
}

.hero-meta {
  display: grid;
  gap: 16px;
}

.hero-stat {
  padding: 16px 20px;
  border-radius: 16px;
  background: #f8fafc;
}

.hero-stat-label {
  margin: 0;
  font-size: 13px;
  color: #475569;
}

.hero-stat-value {
  margin: 4px 0 0;
  font-size: 28px;
  font-weight: 600;
  color: #0f172a;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 20px;
}

.stat-label {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.stat-value {
  font-size: 30px;
  font-weight: 600;
  color: #0f172a;
  margin: 8px 0;
}

.stat-note {
  color: #94a3b8;
  font-size: 12px;
}

.section-card {
  padding: 24px;
  margin-bottom: 24px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.section-heading h2 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.section-subtitle {
  margin: 4px 0 0;
  color: #64748b;
}

.section-controls {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination-container {
  margin-top: 24px;
  text-align: center;
}

.list-interests {
  color: #8c8c8c;
  font-size: 12px;
}

:deep(.surface-modal .ant-modal-content) {
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
}

:deep(.surface-modal .ant-modal-header) {
  border-bottom: 1px solid #e2e8f0;
  border-radius: 24px 24px 0 0;
  padding: 20px 24px;
}

:deep(.surface-modal .ant-modal-body) {
  padding: 0;
}

.modal-content {
  padding: 24px;
}

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .hero-card {
    flex-direction: column;
    padding: 24px;
  }

  .hero-actions {
    flex-direction: column;
    width: 100%;
  }

  .section-controls {
    width: 100%;
    flex-direction: column;
  }
}
</style>
