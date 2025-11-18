<template>
  <div class="personas-page">
    <!-- Page Header -->
    <a-page-header
      :title="$t('personas.title')"
      :sub-title="$t('personas.subtitle')"
    >
      <template #extra>
        <a-space>
          <a-input-search
            v-model:value="searchQuery"
            :placeholder="$t('personas.searchPlaceholder')"
            style="width: 250px"
            @search="handleSearch"
            allow-clear
          />
          <a-button type="primary" @click="showCreateModal = true">
            <template #icon><plus-outlined /></template>
            {{ $t('personas.createPersona') }}
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- Stats Cards -->
    <a-row :gutter="16" class="stats-section">
      <a-col :xs="24" :sm="12" :md="6">
        <a-card>
          <a-statistic
            :title="$t('personas.total')"
            :value="personas.length"
            :prefix="'👥'"
          />
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card>
          <a-statistic
            :title="$t('personas.recent')"
            :value="recentPersonasCount"
            :prefix="'🆕'"
            :value-style="{ color: '#3f8600' }"
          />
          <template #extra>
            <span class="stat-extra">{{ $t('personas.stats.last7Days') }}</span>
          </template>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card>
          <a-statistic
            :title="$t('personas.mostCommonTone')"
            :value="mostCommonTone"
            :prefix="toneEmoji"
          />
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card>
          <a-statistic
            :title="$t('personas.quota')"
            :value="quotaPercentage"
            suffix="%"
            :value-style="quotaPercentage > 80 ? { color: '#cf1322' } : {}"
          />
          <template #extra>
            <span class="stat-extra">{{ personas.length }} / 100</span>
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- Personas Grid -->
    <a-card class="personas-grid-card" :loading="loading">
      <template #title>
        <span>{{ `${$t('personas.title')} (${filteredPersonas.length})` }}</span>
      </template>

      <template #extra>
        <a-space>
          <a-select
            v-model:value="sortBy"
            style="width: 150px"
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
        </a-space>
      </template>

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
    </a-card>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:visible="showCreateModal"
      :title="editingPersona ? $t('personas.editPersona') : $t('personas.createPersona')"
      :footer="null"
      :width="900"
      :destroy-on-close="true"
    >
      <PersonaForm
        :persona="editingPersona"
        :loading="submitting"
        @submit="handleSubmitPersona"
        @cancel="handleCancelForm"
      />
    </a-modal>

    <!-- View Details Modal -->
    <a-modal
      v-model:visible="showViewModal"
      :title="$t('personas.viewPersona')"
      :footer="null"
      :width="700"
    >
      <PersonaCard
        v-if="viewingPersona"
        :persona="viewingPersona"
        :show-full-details="true"
        :max-display-items="100"
        preview-mode
      />
      <template #footer>
        <a-space>
          <a-button @click="showViewModal = false">{{ $t('personas.actions.close') }}</a-button>
          <a-button type="primary" @click="handleEditFromView">
            <edit-outlined /> {{ $t('personas.actions.edit') }}
          </a-button>
        </a-space>
      </template>
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
    async handleSubmitPersona(personaData) {
      this.submitting = true
      try {
        if (this.editingPersona?.id) {
          await api.personas.update(this.editingPersona.id, personaData)
          this.$message.success(this.$t('personas.updatedSuccess'))
        } else {
          await api.personas.create(personaData)
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
.personas-page {
  padding: 24px;
}

.stats-section {
  margin-bottom: 24px;
}

.stat-extra {
  font-size: 12px;
  color: #8c8c8c;
}

.personas-grid-card {
  margin-top: 16px;
}

.pagination-container {
  margin-top: 24px;
  text-align: center;
}

.list-interests {
  color: #8c8c8c;
  font-size: 12px;
}

/* Responsive */
@media (max-width: 768px) {
  .personas-page {
    padding: 16px;
  }

  .stats-section {
    margin-bottom: 16px;
  }
}
</style>
