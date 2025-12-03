<template>
  <div class="ad-create-page">
    <div class="ad-create-content">

      <!-- Page Header -->
      <a-page-header
        class="ad-create-page-header"
        :title="$t('adCreate.pageHeader.title')"
        :sub-title="$t('adCreate.pageHeader.subtitle')"
      >
        <template #extra>
          <a-space size="small">
            <a-button @click="$router.push('/ad/learn')">
              <template #icon><bulb-outlined /></template>
              {{ $t('adCreate.pageHeader.learnAds') }}
            </a-button>
            <a-button type="default" @click="$router.push('/dashboard')">
              <template #icon><arrow-left-outlined /></template>
              {{ $t('adCreate.pageHeader.backToDashboard') }}
            </a-button>
          </a-space>
        </template>
      </a-page-header>

      <!-- Creative Progress Steps -->
      <div class="creative-progress-container">
        <div class="progress-header">
          <h2 class="progress-title">{{ $t('adCreate.progress.headerTitle') }}</h2>
          <div class="progress-subtitle">{{ $t('adCreate.progress.stepOf', {step: currentStep, total: 3}) }}</div>
        </div>

        <div class="creative-progress-tracker">
          <div class="progress-line">
            <div class="progress-fill" :style="{ width: (currentStep / 3 * 100) + '%' }"></div>
          </div>

          <div class="progress-steps">
            <div
              v-for="(step, index) in progressSteps"
              :key="index"
              class="progress-step"
              :class="{
                active: currentStep === index + 1,
                completed: currentStep > index + 1,
                current: currentStep === index + 1
              }"
            >
              <div class="step-circle">
                <div v-if="currentStep > index + 1" class="step-check">✓</div>
                <div v-else class="step-number">{{ index + 1 }}</div>
              </div>
              <div class="step-content">
                <div class="step-title">{{ step.title }}</div>
                <div class="step-desc">{{ step.description }}</div>
                <div v-if="currentStep === index + 1" class="step-emoji">{{ step.emoji }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Fun Progress Indicator -->
        <div class="fun-progress">
          <div class="progress-character">
            <span v-if="currentStep === 1"></span>
            <span v-else-if="currentStep === 2"></span>
            <span v-else></span>
          </div>
          <div class="progress-message">
            <span v-if="currentStep === 1">{{ $t('adCreate.progress.step1Message') }}</span>
            <span v-else-if="currentStep === 2">{{ $t('adCreate.progress.step2Message') }}</span>
            <span v-else>{{ $t('adCreate.progress.step3Message') }}</span>
          </div>
        </div>
      </div>

      <!-- Step 1: Basic Information -->
      <div v-if="currentStep === 1" class="step-content">
        <a-card :title="$t('adCreate.step1.cardTitle')" class="enhanced-card">
          <a-form layout="vertical">
            <!-- Campaign Selection -->
            <a-form-item :label="$t('adCreate.step1.campaign.label')" required>
              <a-select
                v-model:value="formData.campaignId"
                :placeholder="$t('adCreate.step1.campaign.placeholder')"
                :loading="loadingCampaigns"
                show-search
                :filter-option="false"
                @search="loadCampaigns"
                @change="handleCampaignChange"
              >
                <a-select-option
                  v-for="campaign in campaigns"
                  :key="campaign.id"
                  :value="campaign.id"
                >
                  {{ campaign.name }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-card
              v-if="selectedCampaign && selectedCampaign.targetAudience"
              class="campaign-audience-card"
              style="margin-bottom: 24px; background: linear-gradient(135deg, #f0f7ff 0%, #e6f4ff 100%); border: 1px solid #91caff;"
            >
              <template #title>
                <span style="color: #1890ff;">{{ $t('adCreate.step1.targetAudience.title') }}</span>
              </template>
              <div style="padding: 12px;">
                <div style="font-size: 14px; color: #262626; margin-bottom: 8px; white-space: pre-wrap;">
                  {{ selectedCampaign.targetAudience }}
                </div>
                <div style="font-size: 12px; color: #8c8c8c; margin-top: 12px;">
                  {{ $t('adCreate.step1.targetAudience.info') }}
                </div>
              </div>
            </a-card>

            <!-- Ad Type Selection -->
            <a-form-item class="creative-style-item">
              <template #label>
                <span>{{ $t('adCreate.step1.adType.label') }}</span>
                <a-button type="text" size="small" @click="showAdTypeHelp = true">
                  <template #icon><question-circle-outlined /></template>
                </a-button>
              </template>
              <a-radio-group v-model:value="formData.adType" @change="onAdTypeChange">
                <a-radio
                  v-for="type in adTypes"
                  :key="type.value"
                  :value="type.value"
                  class="ad-type-radio"
                >
                  {{ type.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>

            <!-- Ad Name -->
            <a-form-item :label="$t('adCreate.step1.adName.label')" required>
              <a-input
                v-model:value="formData.name"
                :placeholder="$t('adCreate.step1.adName.placeholder')"
                :maxlength="100"
                show-count
              />
            </a-form-item>

            <!-- Number of Variations -->
            <a-form-item :label="$t('adCreate.step1.variations.label')">
              <a-input-number
                v-model:value="formData.numberOfVariations"
                :min="1"
                :max="5"
                style="width: 100%"
              />
            </a-form-item>

            <!-- Language -->
            <a-form-item :label="$t('adCreate.step1.language.label')">
              <a-select v-model:value="formData.language" :placeholder="$t('adCreate.step1.language.placeholder')">
                <a-select-option value="vi">{{ $t('adCreate.step1.language.vietnamese') }}</a-select-option>
                <a-select-option value="en">{{ $t('adCreate.step1.language.english') }}</a-select-option>
              </a-select>
            </a-form-item>

            <!-- Issue #8: Creative Style/Tone -->
            <a-form-item>
              <template #label>
                <span>{{ $t('adCreate.step1.creativeStyle.label') }}</span>
                <a-tooltip>
                  <template #title>
                    {{ $t('adCreate.step1.creativeStyle.tooltip') }}
                  </template>
                  <question-circle-outlined style="margin-left: 4px; color: #999;" />
                </a-tooltip>
              </template>
              <a-select
                v-model:value="formData.adStyle"
                :placeholder="$t('adCreate.step1.creativeStyle.placeholder')"
                allow-clear
              >
                <a-select-option
                  v-for="style in adStyleOptions"
                  :key="style.value"
                  :value="style.value"
                >
                  <div class="style-option">
                    <div class="style-label">{{ style.label }}</div>
                    <div class="style-description">{{ style.description }}</div>
                  </div>
                </a-select-option>
              </a-select>

              <!-- Preview selected style -->
              <div v-if="formData.adStyle && stylePreviewDescription" class="style-preview">
                <div class="preview-icon"></div>
                <div class="preview-content">
                  <div class="preview-title">{{ $t('adCreate.step1.creativeStyle.previewTitle') }}</div>
                  <div class="preview-desc">{{ stylePreviewDescription }}</div>
                </div>
              </div>
            </a-form-item>

            <!-- Call to Action -->
            <a-form-item :label="$t('adCreate.step1.callToAction.label')">
              <a-select
                v-model:value="formData.callToAction"
                :placeholder="$t('adCreate.step1.callToAction.placeholder')"
                :loading="loadingCTAs"
              >
                <a-select-option
                  v-for="cta in standardCTAs"
                  :key="cta.value"
                  :value="cta.value"
                >
                  {{ cta.label }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <!-- Website URL (for website conversion ads) -->
            <a-form-item
              v-if="formData.adType === 'website_conversion'"
              :label="$t('adCreate.step1.websiteUrl.label')"
              required
            >
              <a-input
                v-model:value="formData.websiteUrl"
                :placeholder="$t('adCreate.step1.websiteUrl.placeholder')"
                type="url"
              />
            </a-form-item>

            <!-- Lead Form Questions (for lead generation ads) -->
            <a-form-item
              v-if="formData.adType === 'lead_generation'"
              :label="$t('adCreate.step1.leadFormQuestions.label')"
            >
              <div class="lead-form-questions">
                <div
                  v-for="(question, index) in formData.leadFormQuestions"
                  :key="index"
                  class="question-item"
                >
                  <a-input
                    v-model:value="formData.leadFormQuestions[index]"
                    :placeholder="$t('adCreate.step1.leadFormQuestions.placeholder')"
                  >
                    <template #suffix>
                      <a-button
                        type="text"
                        size="small"
                        @click="removeLeadFormQuestion(index)"
                        danger
                      >
                        <template #icon><delete-outlined /></template>
                      </a-button>
                    </template>
                  </a-input>
                </div>
                <a-button
                  type="dashed"
                  @click="addLeadFormQuestion"
                  block
                  style="margin-top: 8px"
                >
                  <template #icon><plus-outlined /></template>
                  {{ $t('adCreate.step1.leadFormQuestions.addButton') }}
                </a-button>
              </div>
            </a-form-item>

            <!-- Prompt -->
            <a-form-item>
              <template #label>
                <span>{{ $t('adCreate.step1.prompt.label') }}</span>
                <a-button type="text" size="small" @click="showHelpDialog = true">
                  <template #icon><question-circle-outlined /></template>
                </a-button>
              </template>
              <a-textarea
                v-model:value="formData.prompt"
                :placeholder="$t('adCreate.step1.prompt.placeholder')"
                :rows="4"
                :maxlength="2000"
                show-count
              />
            </a-form-item>

            <!-- Persona Selector -->
            <PersonaSelector
              v-model="formData.personaId"
              @persona-selected="handlePersonaSelected"
            />

            <!-- Trending Keywords -->
            <TrendingKeywords
              :language="formData.language"
              @keywords-selected="handleKeywordsSelected"
            />
          </a-form>

          <!-- Error Display -->
          <FieldError :error="generateError" />

          <!-- Navigation -->
          <div class="step-navigation">
            <a-button
              type="primary"
              @click="nextStep"
              :disabled="!validateStep1()"
              size="large"
            >
              {{ $t('adCreate.step1.navigation.next') }}
              <template #icon><arrow-right-outlined /></template>
            </a-button>
          </div>
        </a-card>
      </div>

      <!-- Step 2: AI Configuration -->
      <div v-if="currentStep === 2" class="step-content">
        <a-card :title="$t('adCreate.step2.cardTitle')" class="enhanced-card">
          <div class="ai-config-container">
            <div class="ai-section-header">
              <h2 class="section-title-magic">{{ $t('adCreate.step2.sectionTitle') }}</h2>
              <p class="section-subtitle-magic">{{ $t('adCreate.step2.sectionSubtitle') }}</p>
            </div>

            <!-- Image Upload Info Message -->
            <a-alert
              v-if="hasUploadedImage"
              :message="$t('adCreate.step2.imageUpload.message')"
              :description="$t('adCreate.step2.imageUpload.descriptionV2')"
              type="info"
              show-icon
              closable
              style="margin-bottom: 24px;"
            />

            <!-- Provider Selection Table -->
            <div class="provider-selection-table">
              <a-table
                :columns="providerTableColumns"
                :data-source="formData.variations"
                :pagination="false"
                :expandable="{
                  expandedRowRender: (record, index) => null,
                  expandRowByClick: false
                }"
                bordered
                size="middle"
              >
                <template #headerCell="{ column }">
                  <template v-if="column.key === 'variation'">
                    <span style="font-weight: 600;">{{ column.title }}</span>
                  </template>
                  <template v-else-if="column.key === 'textProvider'">
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span>✍️</span>
                      <span style="font-weight: 600;">{{ column.title }}</span>
                    </div>
                  </template>
                  <template v-else-if="column.key === 'imageProvider'">
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span>🎨</span>
                      <span style="font-weight: 600;">{{ column.title }}</span>
                    </div>
                  </template>
                </template>

                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.key === 'variation'">
                    <div class="variation-label">
                      <span class="variation-number">{{ index + 1 }}</span>
                      <span class="variation-text">{{ $t('adCreate.step2.table.variation') }} {{ index + 1 }}</span>
                      <!-- Visual indicators for upload status -->
                      <a-tag v-if="record.uploadedFileUrl" color="green" size="small" style="margin-left: 8px;">
                        <file-image-outlined /> {{ $t('adCreate.step2.uploadStatus.uploaded') }}
                      </a-tag>
                      <a-tag v-else-if="record.imageProvider" color="blue" size="small" style="margin-left: 8px;">
                        <robot-outlined /> {{ $t('adCreate.step2.uploadStatus.ai') }}
                      </a-tag>
                    </div>
                  </template>

                  <template v-else-if="column.key === 'textProvider'">
                    <a-select
                      v-model:value="record.textProvider"
                      :placeholder="$t('adCreate.step2.table.selectTextProvider')"
                      style="width: 100%;"
                      size="large"
                    >
                      <a-select-option
                        v-for="provider in enabledTextProviders"
                        :key="provider.value"
                        :value="provider.value"
                      >
                        <div class="provider-option">
                          <div class="provider-option-name">{{ provider.name }}</div>
                          <div class="provider-option-description">{{ provider.description }}</div>
                        </div>
                      </a-select-option>
                    </a-select>
                  </template>

                  <template v-else-if="column.key === 'imageProvider'">
                    <div>
                      <a-select
                        v-model:value="record.imageProvider"
                        :disabled="!!record.uploadedFileUrl"
                        :placeholder="record.uploadedFileUrl
                          ? $t('adCreate.step2.imageProvider.disabledHint')
                          : $t('adCreate.step2.table.selectImageProvider')"
                        style="width: 100%;"
                        size="large"
                      >
                        <a-select-option
                          v-for="provider in enabledImageProviders"
                          :key="provider.value"
                          :value="provider.value"
                        >
                          <div class="provider-option">
                            <div class="provider-option-name">{{ provider.name }}</div>
                            <div class="provider-option-description">{{ provider.description }}</div>
                          </div>
                        </a-select-option>
                      </a-select>
                      <div v-if="record.uploadedFileUrl" class="text-xs text-gray-500 mt-1">
                        <info-circle-outlined /> {{ $t('adCreate.step2.imageProvider.removeToEnable') }}
                      </div>
                    </div>
                  </template>
                </template>

                <!-- Expandable Row Content: Per-Variation Upload -->
                <template #expandedRowRender="{ record, index }">
                  <div class="variation-upload-section">
                    <h4 class="font-semibold mb-3" style="font-size: 14px; color: #333;">
                      {{ $t('adCreate.step2.variationUpload.title', { index: index + 1 }) }}
                    </h4>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                      <!-- Upload Section -->
                      <div class="upload-section" style="padding: 12px; background: white; border-radius: 4px; border: 1px solid #e8e8e8;">
                        <div style="font-weight: 500; margin-bottom: 8px; font-size: 13px;">
                          {{ $t('adCreate.step2.variationUpload.uploadOption') }}
                        </div>
                        <a-upload
                          :file-list="record.uploadedFiles || []"
                          :before-upload="(file) => handleVariationFileUpload(file, record)"
                          @remove="() => removeVariationFile(record)"
                          accept="image/*,video/*"
                          list-type="picture-card"
                          :max-count="1"
                        >
                          <div v-if="!record.uploadedFiles || record.uploadedFiles.length === 0">
                            <plus-outlined />
                            <div style="margin-top: 8px; font-size: 12px;">{{ $t('adCreate.step2.variationUpload.uploadButton') }}</div>
                          </div>
                        </a-upload>
                        <div v-if="record.uploadedFileUrl" style="margin-top: 8px; color: #52c41a; font-size: 12px;">
                          ✓ {{ $t('adCreate.step2.variationUpload.uploadSuccess') }}
                        </div>
                        <div v-if="record.uploadInProgress" style="margin-top: 8px; color: #1890ff; font-size: 12px;">
                          <loading-outlined /> Uploading...
                        </div>
                      </div>

                      <!-- AI Provider Info -->
                      <div class="ai-info-section" :style="{ padding: '12px', background: 'white', borderRadius: '4px', border: '1px solid #e8e8e8', opacity: record.uploadedFileUrl ? 0.5 : 1 }">
                        <div style="font-weight: 500; margin-bottom: 8px; font-size: 13px;">
                          {{ $t('adCreate.step2.variationUpload.aiOption') }}
                        </div>
                        <div v-if="record.uploadedFileUrl" style="color: #8c8c8c; font-size: 12px;">
                          {{ $t('adCreate.step2.variationUpload.removeToUseAI') }}
                        </div>
                        <div v-else-if="record.imageProvider" style="color: #1890ff; font-size: 12px;">
                          <robot-outlined /> {{ getProviderName(record.imageProvider) }} {{ $t('adCreate.step2.variationUpload.willGenerate') }}
                        </div>
                        <div v-else style="color: #fa8c16; font-size: 12px;">
                          <warning-outlined /> {{ $t('adCreate.step2.variationUpload.selectProvider') }}
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </a-table>
            </div>

            <!-- Provider Info Cards -->
            <div class="provider-info-section">
              <a-collapse v-model:activeKey="activeInfoPanels" ghost>
                <a-collapse-panel key="text" :header="$t('adCreate.step2.info.textProvidersTitle')">
                  <div class="provider-info-grid">
                    <div
                      v-for="provider in textProviders"
                      :key="provider.value"
                      class="provider-info-card"
                      :class="{ disabled: !provider.enabled }"
                    >
                      <div class="info-header">
                        <h4 class="info-name">{{ provider.name }}</h4>
                      </div>
                      <p class="info-description">{{ provider.description }}</p>
                    </div>
                  </div>
                </a-collapse-panel>

                <a-collapse-panel key="image" :header="$t('adCreate.step2.info.imageProvidersTitle')">
                  <div class="provider-info-grid">
                    <div
                      v-for="provider in imageProviders"
                      :key="provider.value"
                      class="provider-info-card"
                      :class="{ disabled: !provider.enabled }"
                    >
                      <div class="info-header">
                        <h4 class="info-name">{{ provider.name }}</h4>
                      </div>
                      <p class="info-description">{{ provider.description }}</p>
                    </div>
                  </div>
                </a-collapse-panel>
              </a-collapse>
            </div>
          </div>
        </a-card>

          <!-- Navigation -->
          <div class="step-navigation">
            <a-button @click="prevStep" size="large">
              <template #icon><arrow-left-outlined /></template>
              {{ $t('adCreate.navigation.previous') }}
            </a-button>
            <a-button
              type="primary"
              @click="generateAd"
              :loading="isGenerating"
              :disabled="!validateStep2()"
              size="large"
            >
              <template #icon><thunderbolt-outlined /></template>
              {{ $t('adCreate.step2.navigation.generateAd') }}
            </a-button>
          </div>
        </div>
      </div>

      <!-- Step 3: Preview & Save -->
      <div v-if="currentStep === 3" class="step-content">
        <a-card :title="$t('adCreate.step3.cardTitle')" class="enhanced-card">
          <div v-if="adVariations.length > 0" class="ad-preview-container">
            <p class="preview-instruction">
              <span>{{ $t('adCreate.step3.instruction') }}</span>
              <a-tooltip :title="$t('adCreate.step3.helpDialog.tooltip')">
                <a-button
                  type="text"
                  size="small"
                  class="instruction-help-btn"
                  @click="showQualityHelp = true"
                >
                  <template #icon><question-circle-outlined /></template>
                </a-button>
              </a-tooltip>
            </p>

            <!-- Quality Score Summary Card -->
            <div v-if="bestQualityScore" class="quality-score-summary">
              <div class="summary-card">
                <span class="summary-icon">⭐</span>
                <div class="summary-content">
                  <div class="summary-label">{{ $t('adCreate.step3.qualityScore.bestLabel') }}</div>
                  <div class="summary-value">{{ bestQualityScore }}</div>
                </div>
              </div>
              <div v-if="loadingQualityScores" class="summary-loading">
                <a-spin size="small" /> {{ $t('adCreate.step3.qualityScore.analyzing') }}
              </div>
            </div>

            <div class="ad-preview-grid">
              <div
                v-for="variation in adVariations"
                :key="getVariationIdentifier(variation)"
                class="ad-preview-card"
                :class="{
                  selected: isVariationSelected(variation),
                  'best-quality': variation.qualityScore?.totalScore === bestQualityScoreValue,
                  'unsaved-highlight': highlightUnsavedVariations
                }"
                @click="selectVariation(variation)"
              >
                <!-- Best Quality Badge -->
                <div
                  v-if="variation.qualityScore?.totalScore === bestQualityScoreValue && bestQualityScoreValue >= 80"
                  class="best-quality-badge"
                >
                  {{ $t('adCreate.step3.qualityScore.bestBadge') }}
                </div>

                <!-- Needs Review Warning -->
                <a-tag v-if="variation.needsReview" color="orange" class="needs-review-tag">
                  <warning-outlined /> {{ $t('adCreate.step3.needsReview') }}
                </a-tag>

                <div class="ad-preview-content">
                  <div
                    v-if="variation.imageUrl"
                    class="ad-preview-image"
                    @click.stop="openMediaModal(variation.imageUrl)"
                  >
                    <img
                      :src="variation.imageUrl"
                      :alt="variation.headline"
                      @error="handleImageError($event, variation)"
                    />
                  </div>
                  <div class="ad-preview-text">
                    <h3 class="ad-preview-headline">{{ variation.headline }}</h3>
                    <p class="ad-preview-primary-text">{{ variation.primaryText }}</p>
                    <p class="ad-preview-description">{{ variation.description }}</p>
                    <div class="ad-preview-cta">{{ getCTALabel(variation.callToAction) }}</div>
                  </div>
                </div>

                <!-- Quality Score Component -->
                <div v-if="variation.qualityScore" class="quality-score-wrapper">
                  <QualityScore :score="variation.qualityScore" :compact="true" />
                </div>
                <div v-else-if="loadingQualityScores" class="quality-score-loading">
                  <a-spin size="small" /> {{ $t('adCreate.step3.qualityScore.loading') }}
                </div>

                <div class="ad-preview-actions">
                  <a-button
                    type="text"
                    @click.stop="editVariation(variation)"
                    size="small"
                  >
                    <template #icon><edit-outlined /></template>
                    {{ $t('adCreate.step3.actions.edit') }}
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <a-empty v-else :description="$t('adCreate.step3.emptyState')" />

          <!-- Error Display -->
          <FieldError :error="saveError" />

          <!-- Navigation -->
          <div class="step-navigation">
            <a-button @click="prevStep" size="large">
              <template #icon><arrow-left-outlined /></template>
              {{ $t('adCreate.navigation.previous') }}
            </a-button>
            <a-button
              type="primary"
              @click="saveAd"
              :disabled="selectedVariations.length === 0"
              :loading="isSaving"
              size="large"
            >
              <template #icon><save-outlined /></template>
              {{ $t('adCreate.step3.navigation.saveAd') }}
            </a-button>
          </div>
        </a-card>
      </div>

      <!-- Edit Modal -->
    <a-modal
      v-model:open="showEditModal"
      :title="$t('adCreate.modals.edit.title')"
      width="600px"
      @ok="saveEdit"
      @cancel="cancelEdit"
    >
      <a-form v-if="editingVariation" layout="vertical">
        <a-form-item :label="$t('adCreate.modals.edit.headline.label')">
          <a-input v-model:value="editingVariation.headline" />
        </a-form-item>
        <a-form-item :label="$t('adCreate.modals.edit.primaryText.label')">
          <a-textarea v-model:value="editingVariation.primaryText" :rows="3" />
        </a-form-item>
        <a-form-item :label="$t('adCreate.modals.edit.description.label')">
          <a-textarea v-model:value="editingVariation.description" :rows="2" />
        </a-form-item>
        <a-form-item :label="$t('adCreate.modals.edit.callToAction.label')">
          <a-select
            v-model:value="editingVariation.callToAction"
            :placeholder="$t('adCreate.modals.edit.callToAction.placeholder')"
            :loading="loadingCTAs"
          >
            <a-select-option
              v-for="cta in standardCTAs"
              :key="cta.value"
              :value="cta.value"
            >
              {{ cta.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Confirm Save Modal -->
    <a-modal
      v-model:open="showConfirmModal"
      :title="$t('adCreate.modals.confirmSave.title')"
      @ok="confirmSave"
      @cancel="showConfirmModal = false"
    >
      <p>{{ $t('adCreate.modals.confirmSave.message') }}</p>
      <div v-if="selectedVariation" class="confirm-preview">
        <h4>{{ selectedVariation.headline }}</h4>
        <p>{{ selectedVariation.primaryText }}</p>
      </div>
    </a-modal>

    <!-- Unsaved Variations Warning Modal -->
    <a-modal
      v-model:open="showUnsavedLeaveModal"
      :title="$t('adCreate.modals.unsavedVariations.title')"
      :ok-text="$t('adCreate.modals.unsavedVariations.confirm')"
      :cancel-text="$t('adCreate.modals.unsavedVariations.cancel')"
      :closable="false"
      :mask-closable="false"
      @ok="handleLeaveModalConfirm"
      @cancel="handleLeaveModalCancel"
    >
      <p>{{ $t('adCreate.modals.unsavedVariations.message') }}</p>
      <p class="unsaved-tip">{{ $t('adCreate.modals.unsavedVariations.highlightHint') }}</p>
    </a-modal>

    <!-- Extract Error Dialog -->
    <a-modal
      v-model:open="showExtractErrorDialog"
      :title="$t('adCreate.modals.extractionFailed.title')"
      @ok="onExtractErrorYes"
      @cancel="onExtractErrorNo"
      :ok-text="$t('adCreate.modals.extractionFailed.continueButton')"
      :cancel-text="$t('adCreate.modals.extractionFailed.goBackButton')"
    >
      <p>{{ $t('adCreate.modals.extractionFailed.message') }}</p>
    </a-modal>

    <!-- Media Modal -->
    <a-modal
      v-model:open="showMediaModal"
      :title="$t('adCreate.modals.mediaPreview.title')"
      :footer="null"
      width="80%"
      centered
    >
      <div class="media-modal-content">
        <img
          v-if="selectedMediaUrl"
          :src="selectedMediaUrl"
          :alt="$t('adCreate.modals.mediaPreview.altText')"
          style="max-width: 100%; height: auto;"
        />
      </div>
    </a-modal>

    <!-- Ad Type Help Dialog -->
    <a-modal
      v-model:open="showAdTypeHelp"
      :title="$t('adCreate.modals.adTypesGuide.title')"
      :footer="null"
      width="700px"
    >
      <div class="ad-types-help">
        <div v-for="type in adTypes" :key="type.value" class="ad-type-help-item">
          <h3 class="ad-type-title">{{ type.label }}</h3>
          <p class="ad-type-desc">{{ type.description }}</p>
          <ul class="ad-type-features">
            <li v-for="feature in type.features" :key="feature">{{ feature }}</li>
          </ul>
        </div>
      </div>
    </a-modal>

    <!-- Help Dialog -->
    <a-modal
      v-model:open="showHelpDialog"
      :title="$t('adCreate.modals.helpDialog.title')"
      :footer="null"
      width="700px"
    >
      <div class="help-content">
        <div class="help-section">
          <h3 class="help-title">{{ $t('adCreate.modals.helpDialog.whatIsPrompt.title') }}</h3>
          <p class="help-text">
            {{ $t('adCreate.modals.helpDialog.whatIsPrompt.description') }}
          </p>
        </div>

        <div class="help-section">
          <h3 class="help-title">{{ $t('adCreate.modals.helpDialog.tips.title') }}</h3>
          <div class="help-steps">
            <div class="help-step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h4>{{ $t('adCreate.modals.helpDialog.tips.tip1.title') }}</h4>
                <p>{{ $t('adCreate.modals.helpDialog.tips.tip1.description') }}</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h4>{{ $t('adCreate.modals.helpDialog.tips.tip2.title') }}</h4>
                <p>{{ $t('adCreate.modals.helpDialog.tips.tip2.description') }}</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h4>{{ $t('adCreate.modals.helpDialog.tips.tip3.title') }}</h4>
                <p>{{ $t('adCreate.modals.helpDialog.tips.tip3.description') }}</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">4</div>
              <div class="step-content">
                <h4>{{ $t('adCreate.modals.helpDialog.tips.tip4.title') }}</h4>
                <p>{{ $t('adCreate.modals.helpDialog.tips.tip4.description') }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- Quality Score Help Dialog -->
    <a-modal
      v-model:open="showQualityHelp"
      :title="$t('adCreate.step3.helpDialog.title')"
      :footer="null"
      width="720px"
    >
      <div class="quality-help-content">
        <p class="help-intro">{{ $t('adCreate.step3.helpDialog.intro') }}</p>

        <div class="help-section">
          <h3>{{ $t('adCreate.step3.helpDialog.criteria.compliance.title') }}</h3>
          <p>{{ $t('adCreate.step3.helpDialog.criteria.compliance.description') }}</p>
          <ul>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.compliance.headlineLimit') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.compliance.descriptionLimit') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.compliance.primaryTextLimit') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.compliance.prohibitedWords') }}</li>
          </ul>
        </div>

        <div class="help-section">
          <h3>{{ $t('adCreate.step3.helpDialog.criteria.linguistic.title') }}</h3>
          <p>{{ $t('adCreate.step3.helpDialog.criteria.linguistic.description') }}</p>
          <ul>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.linguistic.keywordDensity') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.linguistic.readability') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.linguistic.grammar') }}</li>
          </ul>
        </div>

        <div class="help-section">
          <h3>{{ $t('adCreate.step3.helpDialog.criteria.persuasiveness.title') }}</h3>
          <p>{{ $t('adCreate.step3.helpDialog.criteria.persuasiveness.description') }}</p>
          <ul>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.persuasiveness.cta') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.persuasiveness.powerWords') }}</li>
          </ul>
        </div>

        <div class="help-section">
          <h3>{{ $t('adCreate.step3.helpDialog.criteria.completeness.title') }}</h3>
          <p>{{ $t('adCreate.step3.helpDialog.criteria.completeness.description') }}</p>
          <ul>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.completeness.headline') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.completeness.descriptionField') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.criteria.completeness.primaryText') }}</li>
          </ul>
        </div>

        <div class="help-section">
          <h3>{{ $t('adCreate.step3.helpDialog.usage.title') }}</h3>
          <ul>
            <li>{{ $t('adCreate.step3.helpDialog.usage.compare') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.usage.focus') }}</li>
            <li>{{ $t('adCreate.step3.helpDialog.usage.language') }}</li>
          </ul>
        </div>

        <p class="help-footnote">{{ $t('adCreate.step3.helpDialog.languageNote') }}</p>
      </div>
    </a-modal>

    <!-- Extracted Content Preview Modal -->
    <ExtractedContentPreview
      v-model:visible="showExtractPreviewModal"
      :extracted-data="extractedDataForPreview"
      :summary="extractionSummary"
      @accept="handleExtractedContentAccept"
      @cancel="handleExtractedContentCancel"
      @skip="handleExtractedContentSkip"
    />

    <!-- Async Progress Modal for Preview Generation -->
    <a-modal
      v-model:visible="showAsyncProgressModal"
      :title="$t('adCreate.async.modalTitle')"
      :closable="false"
      :footer="null"
      :maskClosable="false"
      :width="500"
    >
      <div class="async-progress-container">
        <!-- Progress Icon -->
        <div class="progress-icon">
          <a-spin size="large" />
        </div>
        <div v-if="asyncJobStatus" class="progress-status">
          <span class="status-pill">{{ getAsyncStatusLabel(asyncJobStatus) }}</span>
        </div>

        <!-- Current Step -->
        <div class="progress-info">
          <h3 style="font-size: 16px; margin-bottom: 10px; color: #1890ff;">
            {{ asyncJobCurrentStep || $t('adCreate.async.initializing') }}
          </h3>

          <!-- Progress Bar -->
          <a-progress
            :percent="asyncJobProgress"
            :status="asyncJobStatus === 'FAILED' ? 'exception' : 'active'"
            :stroke-color="{
              '0%': '#2d5aa0',
              '100%': '#1890ff'
            }"
            :show-info="true"
          />

          <!-- Details -->
          <p class="progress-details">
            {{ $t('adCreate.async.hint') }}
          </p>

          <!-- Job ID (for debugging) -->
          <p v-if="asyncJobId" class="progress-jobid">
            {{ $t('adCreate.async.jobIdLabel', { id: asyncJobId }) }}
          </p>
        </div>

        <!-- Cancel Button -->
        <div class="progress-actions">
          <a-button
            @click="cancelAsyncJob"
            danger
            :loading="asyncJobStatus === 'CANCELLING'"
          >
            {{ $t('adCreate.async.cancel') }}
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  QuestionCircleOutlined,
  DeleteOutlined,
  PlusOutlined,
  ThunderboltOutlined,
  EditOutlined,
  SaveOutlined,
  FileImageOutlined,
  RobotOutlined,
  InfoCircleOutlined,
  LoadingOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import api from '@/services/api'
import PersonaSelector from '@/components/PersonaSelector.vue'
import TrendingKeywords from '@/components/TrendingKeywords.vue'
import FieldError from '@/components/FieldError.vue'
import QualityScore from '@/components/QualityScore.vue'
import ExtractedContentPreview from '@/components/ExtractedContentPreview.vue'
import qualityApi from '@/services/qualityApi'
import { detectLanguage, i18nTemplates, getKeywordSuccessMessage } from '@/utils/languageDetector'

export default {
  name: 'AdCreate',
  components: {
    ArrowLeftOutlined,
    ArrowRightOutlined,
    QuestionCircleOutlined,
    DeleteOutlined,
    PlusOutlined,
    ThunderboltOutlined,
    EditOutlined,
    SaveOutlined,
    FileImageOutlined,
    RobotOutlined,
    InfoCircleOutlined,
    LoadingOutlined,
    WarningOutlined,
    PersonaSelector,
    TrendingKeywords,
    FieldError,
    QualityScore,
    ExtractedContentPreview
  },
  data() {
    return {
      currentStep: 1,
      formData: {
        campaignId: null,
        adType: 'website_conversion',
        name: '',
        numberOfVariations: 3,
        language: 'vi',
        adStyle: null, // Issue #8: Creative style/tone for ad content
        callToAction: '',
        websiteUrl: '',
        leadFormQuestions: [],
        prompt: '',
        // Legacy fields kept for backward compatibility
        textProvider: 'openai',
        imageProvider: 'gemini',
        // New per-variation provider selection
        variations: [
          { textProvider: 'openai', imageProvider: 'gemini', uploadedFiles: [], uploadedFileUrl: '' },
          { textProvider: 'openai', imageProvider: 'gemini', uploadedFiles: [], uploadedFileUrl: '' },
          { textProvider: 'openai', imageProvider: 'gemini', uploadedFiles: [], uploadedFileUrl: '' }
        ],
        enhancementOptions: []
        // Issue #9: audienceSegment removed - now at campaign level
      },
      steps: [
        { title: this.$t('adCreate.steps.step1.title'), description: this.$t('adCreate.steps.step1.description') },
        { title: this.$t('adCreate.steps.step2.title'), description: this.$t('adCreate.steps.step2.description') },
        { title: this.$t('adCreate.steps.step3.title'), description: this.$t('adCreate.steps.step3.description') }
      ],
      adTypes: [
        {
          value: 'website_conversion',
          label: this.$t('adCreate.adTypes.websiteConversion.label'),
          description: this.$t('adCreate.adTypes.websiteConversion.description'),
          features: [
            this.$t('adCreate.adTypes.websiteConversion.features.0'),
            this.$t('adCreate.adTypes.websiteConversion.features.1'),
            this.$t('adCreate.adTypes.websiteConversion.features.2')
          ]
        },
        {
          value: 'lead_generation',
          label: this.$t('adCreate.adTypes.leadGeneration.label'),
          description: this.$t('adCreate.adTypes.leadGeneration.description'),
          features: [
            this.$t('adCreate.adTypes.leadGeneration.features.0'),
            this.$t('adCreate.adTypes.leadGeneration.features.1'),
            this.$t('adCreate.adTypes.leadGeneration.features.2')
          ]
        },
        {
          value: 'page_post',
          label: this.$t('adCreate.adTypes.pagePost.label'),
          description: this.$t('adCreate.adTypes.pagePost.description'),
          features: [
            this.$t('adCreate.adTypes.pagePost.features.0'),
            this.$t('adCreate.adTypes.pagePost.features.1'),
            this.$t('adCreate.adTypes.pagePost.features.2')
          ]
        }
      ],
      campaigns: [],
      selectedCampaign: null, // Issue #9: Track selected campaign for audience display
      textProviders: [
        {
          value: 'openai',
          name: this.$t('adCreate.providers.text.openai.name'),
          description: this.$t('adCreate.providers.text.openai.description'),
          enabled: true
        },
        {
          value: 'claude',
          name: this.$t('adCreate.providers.text.claude.name'),
          description: this.$t('adCreate.providers.text.claude.description'),
          enabled: true
        }
      ],
      imageProviders: [
        {
          value: 'openai',
          name: this.$t('adCreate.providers.image.openai.name'),
          description: this.$t('adCreate.providers.image.openai.description'),
          enabled: true
        },
        {
          value: 'gemini',
          name: this.$t('adCreate.providers.image.gemini.name'),
          description: this.$t('adCreate.providers.image.gemini.description'),
          enabled: true
        },
        {
          value: 'fal-ai',
          name: this.$t('adCreate.providers.image.falai.name'),
          description: this.$t('adCreate.providers.image.falai.description'),
          enabled: false
        },
        {
          value: 'stable-diffusion',
          name: this.$t('adCreate.providers.image.stableDiffusion.name'),
          description: this.$t('adCreate.providers.image.stableDiffusion.description'),
          enabled: false
        }
      ],
      adVariations: [],
      highlightUnsavedVariations: false,
      highlightUnsavedTimeoutId: null,
      selectedVariations: [], // Changed from selectedVariation to support multiple selection
      editingVariation: null,
      adLinks: [''],
      uploadedFiles: [],
      uploadedFileUrl: '',
      selectedMediaUrl: '',
      showUnsavedLeaveModal: false,
      pendingRouteLeaveResolve: null,
      hasSavedAd: false,
      
      // Loading states
      loadingCampaigns: false,
      loadingCTAs: false,
      isGenerating: false,
      isSaving: false,
      extracting: false,
      
      // Modal states
      showEditModal: false,
      showConfirmModal: false,
      showExtractErrorDialog: false,
      showMediaModal: false,
      showAdTypeHelp: false,
      showHelpDialog: false,
      showQualityHelp: false,
      showExtractPreviewModal: false,

      // Other states
      showValidation: false,
      adId: null,
      savedPrompts: [],
      enhancedImages: [],
      isEnhancing: false,
      generateError: null,
      saveError: null,

      // Extraction states
      extractedContentCache: null,
      extractedDataForPreview: [],
      extractionSummary: null,

      // Quality scoring states
      loadingQualityScores: false,
      bestQualityScore: null,
      bestQualityScoreValue: null,

      // Async preview generation states
      useAsyncGeneration: true, // Default: use async for preview generation
      asyncJobId: null,
      asyncJobStatus: null,
      asyncJobProgress: 0,
      asyncJobCurrentStep: '',
      showAsyncProgressModal: false,
      pollingInterval: null,
      asyncHealthy: true,
      pollingStartTime: null, // Track when polling started for timeout
      pollingRetryCount: 0, // Track retry attempts

      // Provider selection UI states
      activeInfoPanels: [],
      providerTableColumns: [
        {
          title: this.$t('adCreate.step2.table.variationColumn'),
          key: 'variation',
          width: '150px'
        },
        {
          title: this.$t('adCreate.step2.table.textProviderColumn'),
          key: 'textProvider',
          width: '40%'
        },
        {
          title: this.$t('adCreate.step2.table.imageProviderColumn'),
          key: 'imageProvider',
          width: '40%'
        }
      ]
    }
  },
  computed: {
    ...mapGetters('cta', {
      allCTAs: 'allCTAs',
      ctaLoaded: 'isLoaded'
    }),

    // Use Vuex CTAs instead of local state
    standardCTAs() {
      return this.allCTAs
    },

    progressSteps() {
      return [
        {
          title: this.$t('adCreate.progressSteps.step1.title'),
          description: this.$t('adCreate.progressSteps.step1.description'),
          emoji: this.$t('adCreate.progressSteps.step1.emoji')
        },
        {
          title: this.$t('adCreate.progressSteps.step2.title'),
          description: this.$t('adCreate.progressSteps.step2.description'),
          emoji: this.$t('adCreate.progressSteps.step2.emoji')
        },
        {
          title: this.$t('adCreate.progressSteps.step3.title'),
          description: this.$t('adCreate.progressSteps.step3.description'),
          emoji: this.$t('adCreate.progressSteps.step3.emoji')
        }
      ]
    },
    hasUploadedImage() {
      // Check if user has uploaded an image
      return this.uploadedFileUrl && this.uploadedFileUrl.trim() !== ''
    },

    // Issue #8: Reactive multilingual ad style options
    adStyleOptions() {
      return [
        {
          value: 'PROFESSIONAL',
          label: this.$t('adCreate.adStyles.professional.label'),
          description: this.$t('adCreate.adStyles.professional.description')
        },
        {
          value: 'CASUAL',
          label: this.$t('adCreate.adStyles.casual.label'),
          description: this.$t('adCreate.adStyles.casual.description')
        },
        {
          value: 'HUMOROUS',
          label: this.$t('adCreate.adStyles.humorous.label'),
          description: this.$t('adCreate.adStyles.humorous.description')
        },
        {
          value: 'URGENT',
          label: this.$t('adCreate.adStyles.urgent.label'),
          description: this.$t('adCreate.adStyles.urgent.description')
        },
        {
          value: 'LUXURY',
          label: this.$t('adCreate.adStyles.luxury.label'),
          description: this.$t('adCreate.adStyles.luxury.description')
        },
        {
          value: 'EDUCATIONAL',
          label: this.$t('adCreate.adStyles.educational.label'),
          description: this.$t('adCreate.adStyles.educational.description')
        },
        {
          value: 'INSPIRATIONAL',
          label: this.$t('adCreate.adStyles.inspirational.label'),
          description: this.$t('adCreate.adStyles.inspirational.description')
        },
        {
          value: 'MINIMALIST',
          label: this.$t('adCreate.adStyles.minimalist.label'),
          description: this.$t('adCreate.adStyles.minimalist.description')
        }
      ]
    },

    // Issue #8: Preview display for selected style
    stylePreviewTitle() {
      return this.$t('adCreate.step1.selectedStyle')
    },

    stylePreviewDescription() {
      if (!this.formData.adStyle) return null
      const selectedStyle = this.adStyleOptions.find(s => s.value === this.formData.adStyle)
      return selectedStyle ? selectedStyle.description : null
    },

    // New computed properties for provider selection
    enabledTextProviders() {
      return this.textProviders.filter(p => p.enabled)
    },

    enabledImageProviders() {
      return this.imageProviders.filter(p => p.enabled)
    }
  },
  watch: {
    // Sync variations array when numberOfVariations changes
    'formData.numberOfVariations'(newCount, oldCount) {
      const currentLength = this.formData.variations.length

      if (newCount > currentLength) {
        // Add new variations with default providers
        for (let i = currentLength; i < newCount; i++) {
          this.formData.variations.push({
            textProvider: 'openai',
            imageProvider: 'gemini',
            uploadedFiles: [],      // Per-variation upload
            uploadedFileUrl: ''     // Per-variation upload URL
          })
        }
      } else if (newCount < currentLength) {
        // Check if removing variations with uploaded images
        const removedVariations = this.formData.variations.slice(newCount)
        const hasUploads = removedVariations.some(v => v.uploadedFileUrl)

        if (hasUploads) {
          this.$confirm({
            title: this.$t('adCreate.messages.warning.removeUploadedVariations.title'),
            content: this.$t('adCreate.messages.warning.removeUploadedVariations.content'),
            onOk: () => {
              this.formData.variations.splice(newCount)
            },
            onCancel: () => {
              // Revert count change
              this.formData.numberOfVariations = oldCount
            }
          })
        } else {
          // No uploads, safe to remove
          this.formData.variations.splice(newCount)
        }
      }
    }
  },
  async mounted() {
    await this.loadData()

    // Check async service health
    await this.checkAsyncServiceHealth()
  },
  beforeUnmount() {
    // Clean up polling interval
    this.stopJobPolling()
    if (this.highlightUnsavedTimeoutId) {
      clearTimeout(this.highlightUnsavedTimeoutId)
      this.highlightUnsavedTimeoutId = null
    }
  },
  beforeRouteLeave() {
    if (this.shouldPreventRouteLeave()) {
      return new Promise(resolve => {
        this.pendingRouteLeaveResolve = resolve
        this.showUnsavedLeaveModal = true
      })
    }
  },
  methods: {
    ...mapActions('cta', ['loadCTAs']),

    // Issue #9: Handle campaign selection to display target audience
    handleCampaignChange(campaignId) {
      if (campaignId) {
        this.selectedCampaign = this.campaigns.find(c => c.id === campaignId)
      } else {
        this.selectedCampaign = null
      }
    },

    shouldPreventRouteLeave() {
      return (
        this.currentStep === 3 &&
        this.adVariations.length > 0 &&
        !this.hasSavedAd &&
        !this.showUnsavedLeaveModal
      )
    },

    handleLeaveModalConfirm() {
      this.showUnsavedLeaveModal = false
      if (this.pendingRouteLeaveResolve) {
        const resolve = this.pendingRouteLeaveResolve
        this.pendingRouteLeaveResolve = null
        resolve(true)
      }
    },

    handleLeaveModalCancel() {
      this.showUnsavedLeaveModal = false
      if (this.pendingRouteLeaveResolve) {
        const resolve = this.pendingRouteLeaveResolve
        this.pendingRouteLeaveResolve = null
        resolve(false)
      }
      this.highlightUnsavedVariationCards()
    },

    highlightUnsavedVariationCards() {
      if (this.highlightUnsavedTimeoutId) {
        clearTimeout(this.highlightUnsavedTimeoutId)
      }
      this.highlightUnsavedVariations = true
      this.highlightUnsavedTimeoutId = setTimeout(() => {
        this.highlightUnsavedVariations = false
        this.highlightUnsavedTimeoutId = null
      }, 2000)
    },

    /**
     * Get CTA display label from enum value
     * @param {string} value - CTA enum value (e.g., 'LEARN_MORE')
     * @returns {string} - Display label (e.g., 'Tìm hiểu thêm')
     */
    getCTALabel(value) {
      if (!value) return ''
      const cta = this.standardCTAs.find(c => c.value === value)
      return cta ? cta.label : value
    },

    handleKeywordsSelected(keywords) {
      // Detect language of current prompt
      const currentLanguage = detectLanguage(this.formData.prompt || '')

      // Get localized templates
      const templates = i18nTemplates.trendingKeywords[currentLanguage]

      // Format keywords
      const keywordText = keywords.map(k => `• ${k}`).join('\n')

      // Build section with appropriate language
      const trendingSection = `\n\n${templates.title}:\n${keywordText}\n\n${templates.instruction}`

      // Check if section already exists (check both languages for safety)
      const viMarker = 'TỪ KHÓA THỊNH HÀNH'
      const enMarker = 'TRENDING KEYWORDS'

      if (!this.formData.prompt.includes(viMarker) && !this.formData.prompt.includes(enMarker)) {
        // No existing section, append new one
        this.formData.prompt += trendingSection
      } else {
        // Update existing section
        this.formData.prompt = this.updateTrendingSection(
          this.formData.prompt,
          trendingSection,
          currentLanguage
        )
      }

      // Localized success message
      const successMsg = getKeywordSuccessMessage(keywords.length, currentLanguage)
      this.$message.success(successMsg)
    },

    updateTrendingSection(prompt, newSection) {
      // Find and replace existing trending section (check both languages)
      const viMarker = 'TỪ KHÓA THỊNH HÀNH'
      const enMarker = 'TRENDING KEYWORDS'

      // Try to match either Vietnamese or English version
      const viRegex = new RegExp(`\\n\\n📈 ${viMarker}:[\\s\\S]*?(?=\\n\\n|$)`, 'g')
      const enRegex = new RegExp(`\\n\\n📈 ${enMarker}:[\\s\\S]*?(?=\\n\\n|$)`, 'g')

      if (viRegex.test(prompt)) {
        return prompt.replace(viRegex, newSection)
      } else if (enRegex.test(prompt)) {
        return prompt.replace(enRegex, newSection)
      }

      // If no match found, append to end
      return prompt + newSection
    },

    handlePersonaSelected(persona) {
      // Store the persona information for later use
      if (persona) {
        // The personaId is already bound via v-model
        // We can optionally show a success message or update UI
        this.$message.success(this.$t('adCreate.messages.success.personaSelected', { name: persona.name }))

        // Note: The backend PersonaService.enrichPromptWithPersona() will automatically
        // enhance the prompt with persona details when personaId is included in the request
      } else {
        this.$message.info(this.$t('adCreate.messages.info.personaCleared'))
      }
    },

    async enhanceImages() {
      if (!this.formData.enhancementOptions.length) {
        this.$message.warning(this.$t('adCreate.messages.warning.selectEnhancementOption'));
        return;
      }
      if (!this.uploadedFiles.length) {
        this.$message.warning(this.$t('adCreate.messages.warning.uploadImagesFirst'));
        return;
      }
      this.isEnhancing = true;
      this.enhancedImages = [];
      try {
        const enhancementPromises = this.uploadedFiles.map(async (file) => {
          const formData = new FormData();
          formData.append('file', file.originFileObj || file);
          const uploadResponse = await api.post('/upload/media', formData);
          const imageUrl = uploadResponse.data.fileUrl;
          const enhanceResponse = await api.post('/ads/enhance-image', {
            imageUrl,
            provider: this.formData.imageProvider,
            enhancementTypes: this.formData.enhancementOptions
          });
          return {
            url: enhanceResponse.data.enhancedUrl,
            originalName: file.name
          };
        });
        this.enhancedImages = await Promise.all(enhancementPromises);
        this.$message.success(this.$t('adCreate.messages.success.imagesEnhanced'));
      } catch (error) {
        this.$message.error(this.$t('adCreate.messages.error.enhanceImagesFailed'));
      } finally {
        this.isEnhancing = false;
      }
    },
    async loadData() {
      await Promise.all([
        this.loadCampaigns(),
        this.loadCallToActions(),
        this.loadProviders()
      ])
    },
    
    async loadCampaigns() {
      this.loadingCampaigns = true
      try {
        const response = await api.campaigns.getAll()
        this.campaigns = response.data.content || []
      } catch (error) {
        console.error('Error loading campaigns:', error)
        this.$message.error(this.$t('adCreate.messages.error.loadCampaignsFailed'))
      } finally {
        this.loadingCampaigns = false
      }
    },
    
    async loadCallToActions() {
      this.loadingCTAs = true
      try {
        // Load CTAs from Vuex store with user's selected language
        await this.loadCTAs({ language: this.formData.language })
      } catch (error) {
        console.error('Error loading CTAs:', error)
        this.$message.error(this.$t('adCreate.messages.error.loadCTAsFailed'))
      } finally {
        this.loadingCTAs = false
      }
    },
    
    async loadProviders() {
      try {
        // Load text providers
        const textResponse = await api.providers.getTextProviders()
        this.textProviders = textResponse.data.map(provider => ({
          value: provider.id,
          name: provider.name,
          description: provider.description,
          enabled: provider.enabled !== false
        }))
        
        // Load image providers
        const imageResponse = await api.providers.getImageProviders()
        this.imageProviders = imageResponse.data.map(provider => ({
          value: provider.id,
          name: provider.name,
          description: provider.description,
          enabled: provider.enabled !== false
        }))
        
        // Set default providers if not already set
        if (this.textProviders.length > 0 && !this.formData.textProvider) {
          this.formData.textProvider = this.textProviders[0].value
        }
        if (this.imageProviders.length > 0 && !this.formData.imageProvider) {
          this.formData.imageProvider = this.imageProviders[0].value
        }
        
      } catch (error) {
        console.error('Error loading providers:', error)
        this.$message.warning(this.$t('adCreate.messages.warning.providersLoadFailed'))
        // Keep the hardcoded providers as fallback
      }
    },
    
    async nextStep() {
      if (this.currentStep < 3) {
        this.currentStep++
      }
    },

    async autoExtractContent() {
      const validLinks = this.adLinks.filter(link => link.trim())

      if (validLinks.length === 0) {
        // No links, just proceed to next step
        this.currentStep++
        return
      }

      this.extracting = true

      try {
        const response = await api.ads.extractFromLibrary({
          adLinks: validLinks
        })

        // Handle new API response format
        if (response.data.error) {
          // API returned error
          this.$message.error(response.data.message || this.$t('adCreate.messages.error.extractContentFailed'))
          this.currentStep++
          return
        }

        const { data, summary } = response.data

        if (!data || data.length === 0) {
          this.$message.warning(this.$t('adCreate.messages.warning.noAdContentFound'))
          this.currentStep++
          return
        }

        // Show preview modal
        this.extractedDataForPreview = data
        this.extractionSummary = summary
        this.showExtractPreviewModal = true

      } catch (error) {
        console.error('Auto-extraction error:', error)

        let errorMessage = this.$t('adCreate.messages.error.extractContentFailed')

        if (error.response) {
          const data = error.response.data
          errorMessage = data.message || errorMessage

          if (data.error === 'API_KEY_NOT_CONFIGURED') {
            errorMessage = this.$t('adCreate.messages.error.apiKeyNotConfigured')
          }
        }

        this.$message.warning(errorMessage)
        // Still proceed to next step on error
        this.currentStep++
      } finally {
        this.extracting = false
      }
    },

    handleExtractedContentAccept(validExtractions) {
      // Combine all extracted texts
      const allTexts = validExtractions
        .map(item => item.text)
        .filter(text => text)
        .join('\n\n---\n\n')

      // Detect language for localized section title
      const currentLanguage = detectLanguage(this.formData.prompt || '')
      const templates = i18nTemplates.adReference[currentLanguage]

      // Append to prompt with localized section title
      if (allTexts) {
        const referenceSection = `\n\n${templates.title}:\n${allTexts}\n\n${templates.instruction}`

        if (this.formData.prompt) {
          this.formData.prompt += referenceSection
        } else {
          this.formData.prompt = `${templates.title}:\n${allTexts}\n\n${templates.instruction}`
        }

        // Cache for later use in generateAd
        this.extractedContentCache = allTexts

        // Localized success message
        const successMsg = currentLanguage === 'vi'
          ? `Đã thêm nội dung từ ${validExtractions.length} quảng cáo vào prompt`
          : `Added content from ${validExtractions.length} ad(s) to prompt`

        this.$message.success(successMsg)
      }

      // Proceed to next step
      this.currentStep++
    },

    handleExtractedContentCancel() {
      // User cancelled, don't proceed to next step
      this.$message.info(this.$t('adCreate.messages.info.extractionCancelled'))
    },

    handleExtractedContentSkip() {
      // User chose to skip extraction, proceed to next step
      this.$message.info(this.$t('adCreate.messages.info.extractionSkipped'))
      this.currentStep++
    },
    
    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    },
    
    onAdTypeChange() {
      // Reset type-specific fields when ad type changes
      this.formData.websiteUrl = ''
      this.formData.leadFormQuestions = []
    },
    
    addLeadFormQuestion() {
      this.formData.leadFormQuestions.push('')
    },
    
    removeLeadFormQuestion(index) {
      this.formData.leadFormQuestions.splice(index, 1)
    },
    
    validateStep1() {
      return this.formData.campaignId && 
             this.formData.name && 
             this.formData.adType &&
             this.formData.prompt
    },
    
    validateStep2() {
      // Valid if all variations have text provider AND (image provider OR per-variation image uploaded)
      const allVariationsValid = this.formData.variations.every(variation => {
        const hasTextProvider = !!variation.textProvider
        const hasImageSource = !!(variation.imageProvider || variation.uploadedFileUrl || this.hasUploadedImage)
        return hasTextProvider && hasImageSource
      })
      return allVariationsValid
    },
    
    async handleFileUpload(file) {
      this.uploadedFiles = [file];
      try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post('/upload/media', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (response.data.success) {
          this.uploadedFileUrl = response.data.fileUrl;
          this.$message.success(this.$t('adCreate.messages.success.fileUploaded'));
        }
      } catch (error) {
        this.$message.error(this.$t('adCreate.messages.error.fileUploadFailed'));
        this.uploadedFiles = [];
      }
      return false;
    },
    
    removeFile() {
      this.uploadedFiles = []
      this.uploadedFileUrl = ''
    },

    // NEW: Per-variation file upload
    async handleVariationFileUpload(file, variation) {
      // Validation: check file size (max 10MB)
      if (file.size > 10 * 1024 * 1024) {
        this.$message.error(this.$t('adCreate.messages.error.fileTooLarge'))
        return false
      }

      // Set file list for this variation (Vue 3 direct assignment)
      variation.uploadedFiles = [file]
      variation.uploadInProgress = true

      try {
        const formData = new FormData()
        formData.append('file', file)

        const response = await api.post('/upload/media', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 30000 // 30 seconds timeout
        })

        if (response.data.success) {
          // Store URL in variation object (Vue 3 direct assignment)
          variation.uploadedFileUrl = response.data.fileUrl

          // Clear image provider (uploaded image takes priority)
          variation.imageProvider = null

          this.$message.success(this.$t('adCreate.messages.success.fileUploaded'))
        }
      } catch (error) {
        console.error('Error uploading variation file:', error)
        this.$message.error(this.$t('adCreate.messages.error.fileUploadFailed'))
        variation.uploadedFiles = []
      } finally {
        variation.uploadInProgress = false
      }

      return false // Prevent default upload behavior
    },

    // NEW: Remove uploaded file from variation
    removeVariationFile(variation) {
      variation.uploadedFiles = []
      variation.uploadedFileUrl = ''
      // Don't auto-set imageProvider, let user choose manually
    },

    handleImageError(event, variation) {
      // Log the error for debugging
      console.error(`Failed to load image for variation: ${variation.headline}`, variation.imageUrl)

      // Set fallback placeholder image using inline SVG data URI
      // This avoids dependency on external files and always works
      event.target.src = 'data:image/svg+xml,' + encodeURIComponent(`
        <svg width="400" height="300" xmlns="http://www.w3.org/2000/svg">
          <rect width="400" height="300" fill="#f0f0f0"/>
          <text x="50%" y="45%" text-anchor="middle" fill="#999" font-size="16" font-family="Arial, sans-serif">
            ${this.$t('adCreate.messages.error.imageLoadError')}
          </text>
          <text x="50%" y="55%" text-anchor="middle" fill="#ccc" font-size="12" font-family="Arial, sans-serif">
            ${variation.headline || this.$t('adCreate.messages.error.imageUnavailable')}
          </text>
        </svg>
      `)

      // Show a warning toast to user (only once per session to avoid spam)
      if (!this.$root.imageErrorShown) {
        this.$message.warning(this.$t('adCreate.messages.warning.imageLoadFailed'))
        this.$root.imageErrorShown = true
      }
    },

    formatFileSize(bytes) {
      if (bytes === 0) return '0 Bytes'
      const k = 1024
      const sizes = ['Bytes', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    async generateAd() {
      this.isGenerating = true
      this.generateError = null

      try {
        // Validate required fields
        if (!this.formData.prompt) {
          this.$message.error(this.$t('adCreate.messages.error.promptRequired'))
          this.currentStep = 1
          this.showValidation = true
          return
        }

        // Use cached extracted content if available (from auto-extraction)
        const extractedContent = this.extractedContentCache || ''

        // Filter valid ad links (non-empty)
        const validLinks = this.adLinks.filter(link => link.trim())

        // Generate ad preview
        let promptWithCTA = this.formData.prompt || ''
        if (promptWithCTA.indexOf('Call to Action') === -1) {
          const ctaList = this.standardCTAs.map(cta => cta.label.split(' - ')[0]).join(', ')
          promptWithCTA += `\n\nLưu ý: Chỉ sử dụng một trong các Call to Action sau cho quảng cáo: ${ctaList}. Không tạo CTA khác.`
        }

        // Calculate most frequent providers from variations array
        const textProviderCounts = {}
        const imageProviderCounts = {}

        this.formData.variations.forEach(variation => {
          textProviderCounts[variation.textProvider] = (textProviderCounts[variation.textProvider] || 0) + 1
          imageProviderCounts[variation.imageProvider] = (imageProviderCounts[variation.imageProvider] || 0) + 1
        })

        const mostFrequentTextProvider = Object.keys(textProviderCounts).reduce((a, b) =>
          textProviderCounts[a] > textProviderCounts[b] ? a : b, 'openai'
        )

        const mostFrequentImageProvider = Object.keys(imageProviderCounts).reduce((a, b) =>
          imageProviderCounts[a] > imageProviderCounts[b] ? a : b, 'gemini'
        )

        const requestData = {
          campaignId: this.formData.campaignId,
          // Issue #13: Pass full campaign object for better context
          campaign: this.selectedCampaign,
          adType: this.formData.adType,
          name: this.formData.name,
          prompt: promptWithCTA,
          language: this.formData.language,
          adLinks: validLinks,
          // Send variations array with per-variation uploads (new format)
          variations: this.formData.variations.map(v => ({
            textProvider: v.textProvider,
            imageProvider: v.imageProvider,
            uploadedFileUrl: v.uploadedFileUrl || null // Per-variation upload
          })),
          // Also send most-frequent providers as fallback (backward compatibility)
          textProvider: mostFrequentTextProvider,
          imageProvider: mostFrequentImageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          mediaFileUrl: this.uploadedFileUrl || null, // Deprecated global upload
          callToAction: this.formData.callToAction,
          extractedContent: extractedContent,
          personaId: this.formData.personaId || null,
          adStyle: this.formData.adStyle || null, // Issue #8: Creative style/tone
          websiteUrl: this.formData.websiteUrl || null,
          ...(this.formData.adType === 'lead_generation' && {
            leadFormQuestions: this.formData.leadFormQuestions.filter(q => q.trim())
          }),
          isPreview: true
        }

        // ASYNC-FIRST: Try async, fallback to sync
        const shouldUseAsync = this.useAsyncGeneration && this.asyncHealthy

        if (shouldUseAsync) {
          console.log('[ASYNC] Starting async preview generation')
          await this.generateAdAsync(requestData)
        } else {
          console.log('[SYNC] Using sync fallback for preview')
          await this.generateAdSync(requestData)
        }

      } catch (error) {
        console.error('Error in generateAd:', error)

        // Store entire error object for FieldError component
        this.generateError = error

        const errorMessage = error.message || this.$t('adCreate.messages.error.generateAdFailed')

        this.$message.error(errorMessage)

        if (errorMessage.includes('Please enter prompt content') || errorMessage.includes('prompt')) {
          this.currentStep = 1
          this.showValidation = true
        }
      } finally {
        this.isGenerating = false
      }
    },

    /**
     * ASYNC preview generation (NEW)
     */
    async generateAdAsync(requestData) {
      try {
        console.log('[ASYNC] Calling async generate API for preview')
        const response = await api.ads.generateAsync(requestData)

        if (!response.data.jobId) {
          throw new Error('No job ID returned from async API')
        }

        this.asyncJobId = response.data.jobId
        console.log('[ASYNC] Received jobId:', this.asyncJobId)

        // Show progress modal
        this.showAsyncProgressModal = true

        // Start polling
        this.startJobPolling()

      } catch (error) {
        console.error('[ASYNC] Failed, falling back to sync:', error)
        this.$message.warning(this.$t('adCreate.messages.warning.asyncUnavailable'))
        await this.generateAdSync(requestData)
      }
    },

    /**
     * SYNC preview generation (Fallback - existing behavior)
     */
    async generateAdSync(requestData) {
      try {
        console.log('[SYNC] Using synchronous preview generation')

        const response = await api.post('/ads/generate', requestData)

        if (response.data.status === 'success') {
          this.adVariations = this.normalizeVariations(response.data.variations)
          this.hasSavedAd = false

          this.adId = response.data.adId
          this.currentStep = 3
          this.$message.success(this.$t('adCreate.messages.success.adCreated'))

          // Load quality scores for variations
          await this.loadQualityScoresForVariations()
        } else {
          throw new Error(response.data.message)
        }

      } catch (error) {
        console.error('[SYNC] Generation failed:', error)
        throw error
      }
    },

    /**
     * Start polling for job status
     */
    startJobPolling() {
      console.log('[ASYNC] Starting job polling for', this.asyncJobId)

      // Clear any existing interval
      this.stopJobPolling()

      // Initialize polling state
      this.pollingStartTime = Date.now()
      this.pollingRetryCount = 0

      // Poll every 2 seconds
      this.pollingInterval = setInterval(async () => {
        await this.checkJobStatus()
      }, 2000)

      // Immediate first check
      this.checkJobStatus()
    },

    /**
     * Check current job status
     */
    async checkJobStatus() {
      if (!this.asyncJobId) {
        console.warn('[ASYNC] No jobId to check')
        return
      }

      // Check for timeout (10 minutes max polling duration)
      const maxPollingDuration = 10 * 60 * 1000 // 10 minutes
      const elapsedTime = Date.now() - this.pollingStartTime

      if (elapsedTime > maxPollingDuration) {
        console.warn('[ASYNC] Polling timeout after 10 minutes')
        this.stopJobPolling()
        this.$message.warning({
          content: this.$t('adCreate.async.timeout'),
          duration: 10
        })
        return
      }

      try {
        const response = await api.ads.getJobStatus(this.asyncJobId)
        const job = response.data

        console.log('[ASYNC] Job status:', job.status, `(${job.progress}%)`, job.currentStep)

        // Reset retry count on successful request
        this.pollingRetryCount = 0

        // Update UI state
        this.asyncJobStatus = job.status
        this.asyncJobProgress = job.progress || 0
        this.asyncJobCurrentStep = job.currentStep || this.$t('adCreate.async.processing')

        // Handle terminal states
        if (job.status === 'COMPLETED') {
          await this.handleJobCompleted()
        } else if (job.status === 'FAILED') {
          this.handleJobFailed(job.errorMessage)
        } else if (job.status === 'CANCELLED') {
          this.handleJobCancelled()
        }

      } catch (error) {
        console.error('[ASYNC] Error checking job status:', error)

        // Implement retry logic with exponential backoff
        const maxRetries = 3
        this.pollingRetryCount++

        if (this.pollingRetryCount <= maxRetries) {
          const retryDelay = 1000 * this.pollingRetryCount // 1s, 2s, 3s
          console.warn(`[ASYNC] Retry ${this.pollingRetryCount}/${maxRetries} after ${retryDelay}ms`)

          // Schedule a retry after delay
          setTimeout(() => {
            this.checkJobStatus()
          }, retryDelay)
        } else {
          // Exceeded max retries - stop polling and show error
          console.error('[ASYNC] Max retries exceeded, stopping polling')
          this.stopJobPolling()
          this.$message.error({
            content: this.$t('adCreate.async.statusFailed'),
            duration: 10
          })
        }
      }
    },

    /**
     * Handle job completion
     */
    async handleJobCompleted() {
      console.log('[ASYNC] Job completed successfully')
      this.stopJobPolling()

      try {
        // Fetch the result
        const response = await api.ads.getJobResult(this.asyncJobId)
        const result = response.data.result

        console.log('[ASYNC] Result:', result)

        // Extract variations
        this.adVariations = this.normalizeVariations(result)
        this.hasSavedAd = false

        // Close progress modal
        this.showAsyncProgressModal = false
        this.asyncJobId = null

        // Move to variations view
        this.currentStep = 3

        // Show success
        this.$message.success(
          this.$t('adCreate.async.completed', { count: this.adVariations.length })
        )

        // Load quality scores for variations
        await this.loadQualityScoresForVariations()

      } catch (error) {
        console.error('[ASYNC] Error fetching job result:', error)
        this.$message.error(
          this.$t('adCreate.async.fetchResultFailed', { message: error.message })
        )
        this.showAsyncProgressModal = false
      }
    },

    /**
     * Handle job failure
     */
    handleJobFailed(errorMessage) {
      console.error('[ASYNC] Job failed:', errorMessage)
      this.stopJobPolling()
      this.showAsyncProgressModal = false

      this.$message.error({
        content: errorMessage || this.$t('adCreate.async.failed'),
        duration: 5
      })

      this.asyncJobId = null
    },

    /**
     * Handle job cancellation
     */
    handleJobCancelled() {
      console.log('[ASYNC] Job was cancelled')
      this.stopJobPolling()
      this.showAsyncProgressModal = false

      this.$message.info(this.$t('adCreate.async.cancelled'))
      this.asyncJobId = null
    },

    /**
     * Stop polling interval
     */
    stopJobPolling() {
      if (this.pollingInterval) {
        console.log('[ASYNC] Stopping job polling')
        clearInterval(this.pollingInterval)
        this.pollingInterval = null
      }
    },

    /**
     * Cancel async job
     */
    async cancelAsyncJob() {
      if (!this.asyncJobId) return

      try {
        console.log('[ASYNC] Cancelling job:', this.asyncJobId)
        await api.ads.cancelJob(this.asyncJobId)
        this.handleJobCancelled()
      } catch (error) {
        console.error('[ASYNC] Error cancelling job:', error)

        // Extract error message from response
        let errorMessage = this.$t('adCreate.async.cancelFailed')
        if (error.response && error.response.data && error.response.data.error) {
          errorMessage = error.response.data.error
        } else if (error.message) {
          errorMessage = error.message
        }

        // Show appropriate message based on error
        if (errorMessage.includes('already completed')) {
          this.$message.success({
            content: this.$t('adCreate.async.alreadyCompleted'),
            duration: 5
          })
          // Attempt to fetch the result
          this.handleJobCompleted()
        } else if (errorMessage.includes('already failed')) {
          this.$message.warning({
            content: this.$t('adCreate.async.alreadyFailed'),
            duration: 5
          })
          this.showAsyncProgressModal = false
          this.stopJobPolling()
        } else if (errorMessage.includes('already been cancelled')) {
          this.$message.info({
            content: this.$t('adCreate.async.alreadyCancelled'),
            duration: 3
          })
          this.handleJobCancelled()
        } else {
          this.$message.error({
            content: errorMessage,
            duration: 5
          })
        }
      }
    },

    /**
     * Check async service health on component mount
     */
    async checkAsyncServiceHealth() {
      try {
        const response = await api.ads.checkAsyncHealth()
        this.asyncHealthy = response.data.healthy === true

        if (!this.asyncHealthy) {
          console.warn('[ASYNC] Service is unhealthy, will use sync mode')
        } else {
          console.log('[ASYNC] Service is healthy')
        }
      } catch (error) {
        console.error('[ASYNC] Health check failed:', error)
        this.asyncHealthy = false
      }
    },

    prepareQualityDetails(rawDetails) {
      if (!rawDetails || typeof rawDetails !== 'object') {
        return null
      }
      return {
        ...rawDetails,
        suggestions: Array.isArray(rawDetails.suggestions) ? rawDetails.suggestions : [],
        strengths: Array.isArray(rawDetails.strengths) ? rawDetails.strengths : []
      }
    },

    normalizeVariations(variations = []) {
      const timestamp = Date.now()
      return variations.map((variation, index) => {
        const clientId = variation.id || variation.clientId || `variation-${timestamp}-${index}`
        const qualityDetails = this.prepareQualityDetails(
          variation.qualityDetails || (typeof variation.qualityScore === 'object' ? variation.qualityScore : null)
        )

        return {
          ...variation,
          clientId,
          imageUrl: variation.imageUrl || variation.uploadedFileUrl || variation.mediaFileUrl || this.uploadedFileUrl || '',
          qualityDetails,
          qualityScore: qualityDetails || null
        }
      })
    },

    getVariationIdentifier(variation) {
      if (!variation) return null
      if (variation.id) return variation.id
      if (variation.clientId) return variation.clientId
      const generatedId = `variation-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
      variation.clientId = generatedId
      return generatedId
    },

    isVariationSelected(variation) {
      const identifier = this.getVariationIdentifier(variation)
      if (!identifier) return false
      return this.selectedVariations.some(v => this.getVariationIdentifier(v) === identifier)
    },
    
    selectVariation(variation) {
      // Toggle selection: add if not selected, remove if already selected
      const identifier = this.getVariationIdentifier(variation)
      const index = this.selectedVariations.findIndex(
        v => this.getVariationIdentifier(v) === identifier
      )
      if (index > -1) {
        // Deselect: Create new array to trigger Vue reactivity
        this.selectedVariations = this.selectedVariations.filter(
          v => this.getVariationIdentifier(v) !== identifier
        )
      } else {
        // Select: Ensure variation has correct imageUrl before adding
        // Priority: variation's own imageUrl > uploadedFileUrl (per-variation) > mediaFileUrl > global uploadedFileUrl
        const variationWithImage = {
          ...variation,
          clientId: variation.clientId || identifier,
          imageUrl: variation.imageUrl || variation.uploadedFileUrl || variation.mediaFileUrl || this.uploadedFileUrl || ''
        }
        // Create new array to trigger Vue reactivity
        this.selectedVariations = [...this.selectedVariations, variationWithImage]
      }
    },
    
    editVariation(variation) {
      this.editingVariation = { ...variation }
      this.showEditModal = true
    },
    
    cancelEdit() {
      this.editingVariation = null
      this.showEditModal = false
    },
    
    saveEdit() {
      if (this.editingVariation) {
        const identifier = this.getVariationIdentifier(this.editingVariation)
        const index = this.adVariations.findIndex(
          v => this.getVariationIdentifier(v) === identifier
        )
        if (index !== -1) {
          const updatedVariation = {
            ...this.editingVariation,
            clientId: this.editingVariation.clientId || identifier
          }
          const updatedAdVariations = [...this.adVariations]
          updatedAdVariations[index] = updatedVariation
          this.adVariations = updatedAdVariations
        }

        const selectedIndex = this.selectedVariations.findIndex(
          v => this.getVariationIdentifier(v) === identifier
        )
        if (selectedIndex !== -1) {
          const updatedSelectedVariations = [...this.selectedVariations]
          updatedSelectedVariations[selectedIndex] = {
            ...this.editingVariation,
            clientId: this.editingVariation.clientId || identifier
          }
          this.selectedVariations = updatedSelectedVariations
        }
      }
      this.showEditModal = false
      this.editingVariation = null
    },
    
    saveAd() {
      if (this.selectedVariations.length === 0) {
        this.$message.warning(this.$t('adCreate.messages.warning.selectVariations'))
        return
      }
      this.showConfirmModal = true
    },
    
    addAdLink() {
      this.adLinks.push('')
    },
    
    removeAdLink(index) {
      this.adLinks.splice(index, 1)
    },
    
    async confirmSave() {
      this.showConfirmModal = false
      this.isSaving = true
      this.saveError = null

      try{
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.formData.adType,
          name: this.formData.name,
          prompt: this.formData.prompt,
          language: this.formData.language,
          adLinks: this.adLinks.filter(link => link.trim()),
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          mediaFileUrl: this.uploadedFileUrl,
          selectedVariations: this.selectedVariations, // Changed from selectedVariation to array
          isPreview: false,
          saveExistingContent: true,
          adStyle: this.formData.adStyle || null,
          websiteUrl: this.formData.websiteUrl || null,
          ...(this.formData.adType === 'lead_generation' && {
            leadFormQuestions: this.formData.leadFormQuestions.filter(q => q.trim())
          }),
          callToAction: this.formData.callToAction || null
        }

        const response = await api.ads.saveExisting(requestData)

        if (response.data.status === 'success') {
          this.$message.success(this.$t('adCreate.messages.success.adSaved'))
          await this.$store.dispatch('dashboard/fetchDashboardData', null, { root: true })
          this.hasSavedAd = true
          this.$router.push('/dashboard')
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Error saving ad:', error)

        // Store entire error object for FieldError component
        this.saveError = error

        this.$message.error(error.message || this.$t('adCreate.messages.error.saveAdFailed'))
      } finally {
        this.isSaving = false
      }
    },
    
    async extractFromLibrary() {
      if (!this.adLinks.length || !this.adLinks[0].trim()) {
        this.$message.warning(this.$t('adCreate.messages.warning.enterAdLink'))
        return
      }

      this.extracting = true

      try {
        const validLinks = this.adLinks.filter(link => link.trim())
        const response = await api.ads.extractFromLibrary({
          adLinks: validLinks
        })

        let success = false
        let content = ''
        if (Array.isArray(response.data)) {
          success = response.data.some(this.hasAdBody)
          if (success) {
            const found = response.data.find(this.hasAdBody)
            content = found.body || (found.snapshot && found.snapshot.body) || found.text || ''
          }
        } else {
          success = this.hasAdBody(response.data)
          if (success) {
            content = response.data.body || (response.data.snapshot && response.data.snapshot.body) || response.data.text || ''
          }
        }

        if (success) {
          this.formData.prompt = content
          this.$message.success(this.$t('adCreate.messages.success.contentExtracted'))
        } else {
          this.$message.warning(response.data.message || this.$t('adCreate.messages.warning.extractContentFailed'))
        }
      } catch (error) {
        console.error('Error extracting from library:', error)
        this.$message.error(this.$t('adCreate.messages.error.extractError', {
          message: error.response?.data?.message || error.message
        }))
      } finally {
        this.extracting = false
      }
    },
    
    onExtractErrorYes() {
      this.showExtractErrorDialog = false
      this.isGenerating = true
      this.generateAdContinueWithoutExtract()
    },
    
    onExtractErrorNo() {
      this.showExtractErrorDialog = false
      this.currentStep = 1
      this.$nextTick(() => {
        if (this.$refs.adLinkInput && this.$refs.adLinkInput[0]) {
          this.$refs.adLinkInput[0].focus()
        }
      })
    },
    
    async generateAdContinueWithoutExtract() {
      try {
        const validLinks = this.adLinks.filter(link => link.trim())
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.formData.adType,
          name: this.formData.name,
          prompt: this.formData.prompt,
          language: this.formData.language,
          adLinks: validLinks,
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          mediaFileUrl: this.uploadedFileUrl,
          extractedContent: null,
          personaId: this.formData.personaId || null,
          adStyle: this.formData.adStyle || null, // Issue #8: Creative style/tone
          websiteUrl: this.formData.websiteUrl || null,
          ...(this.formData.adType === 'lead_generation' && {
            leadFormQuestions: this.formData.leadFormQuestions.filter(q => q.trim())
          }),
          callToAction: this.formData.callToAction || null,
          isPreview: true
        }
        
        const response = await api.post('/ads/generate', requestData)
        
        if (response.data.status === 'success') {
          this.adVariations = this.normalizeVariations(response.data.variations)
          this.hasSavedAd = false
          this.adId = response.data.adId
          this.currentStep = 3
          this.$message.success(this.$t('adCreate.messages.success.adCreated'))
          await this.loadQualityScoresForVariations()
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        // Store entire error object for FieldError component
        this.generateError = error

        const errorMessage = error.message || this.$t('adCreate.messages.error.generateAdFailed')
        this.$message.error(errorMessage)

        if (errorMessage.includes('Please enter prompt content') || errorMessage.includes('valid ad link')) {
          this.currentStep = 1
          this.showValidation = true
        }
      } finally {
        this.isGenerating = false
      }
    },
    
    hasAdBody(ad) {
      return (
        (ad.body && ad.body.trim() !== '') ||
        (ad.snapshot && ad.snapshot.body && ad.snapshot.body.trim() !== '') ||
        (ad.text && ad.text.trim() !== '')
      )
    },
    
    openMediaModal(url) {
      this.selectedMediaUrl = url
      this.showMediaModal = true
    },
    
    closeMediaModal() {
      this.showMediaModal = false
      this.selectedMediaUrl = ''
    },

    onPromptUpdated(improvedPrompt) {
      this.formData.prompt = improvedPrompt
      this.$message.success(this.$t('adCreate.messages.success.promptUpdated'))
    },

    // New Creative Methods for Phase 3 Implementation
    getProviderIcon(provider, type) {
      const icons = {
        text: {
          openai: '🧠',
          claude: '🎭',
          gemini: '💎',
          huggingface: '🤗'
        },
        image: {
          openai: '🎨',
          stability: '🌟',
          fal: '⚡',
          midjourney: '🎪'
        }
      }
      return icons[type]?.[provider] || (type === 'text' ? '✏️' : '🖼️')
    },

    // Get provider display name
    getProviderName(providerValue) {
      const allProviders = [...this.textProviders, ...this.imageProviders]
      const provider = allProviders.find(p => p.value === providerValue)
      return provider ? provider.name : providerValue
    },

    getProviderRating(provider) {
      const ratings = {
        openai: 5,
        claude: 4,
        gemini: 4,
        stability: 4,
        fal: 3,
        huggingface: 3
      }
      return ratings[provider] || 3
    },

    getProviderRatingText(provider) {
      const texts = {
        openai: this.$t('adCreate.providers.ratings.mostPopular'),
        claude: this.$t('adCreate.providers.ratings.reliable'),
        gemini: this.$t('adCreate.providers.ratings.fast'),
        stability: this.$t('adCreate.providers.ratings.highQuality'),
        fal: this.$t('adCreate.providers.ratings.experimental'),
        huggingface: this.$t('adCreate.providers.ratings.openSource')
      }
      return texts[provider] || this.$t('adCreate.providers.ratings.goodChoice')
    },

    getAsyncStatusLabel(status) {
      if (!status) {
        return this.$t('adCreate.async.status.pending')
      }
      const normalized = status.toLowerCase()
      const key = `adCreate.async.status.${normalized}`
      const translated = this.$t(key)
      return translated === key ? status : translated
    },

    calculateAIPowerLevel() {
      let power = 0

      // Text provider power
      const textPower = {
        openai: 35,
        claude: 30,
        gemini: 25,
        huggingface: 20
      }
      power += textPower[this.formData.textProvider] || 20

      // Image provider power
      const imagePower = {
        openai: 35,
        stability: 30,
        fal: 25,
        midjourney: 40
      }
      power += imagePower[this.formData.imageProvider] || 20

      return Math.min(power, 100)
    },

    getAIPowerMessage() {
      const level = this.calculateAIPowerLevel()
      if (level >= 80) return this.$t('adCreate.step2.powerLevel.ultra')
      if (level >= 60) return this.$t('adCreate.step2.powerLevel.great')
      if (level >= 40) return this.$t('adCreate.step2.powerLevel.solid')
      return this.$t('adCreate.step2.powerLevel.good')
    },

    async loadQualityScoresForVariations() {
      if (!this.adVariations || this.adVariations.length === 0) {
        return
      }

      const missingScores = this.adVariations.filter(v => !v.qualityScore && v.id)
      if (missingScores.length === 0) {
        this.updateBestQualityHighlights()
        return
      }

      this.loadingQualityScores = true
      try {
        const adContentIds = missingScores.map(v => v.id).filter(id => id)

        if (adContentIds.length === 0) {
          console.warn('No valid ad content IDs found for quality scoring')
          this.updateBestQualityHighlights()
          return
        }

        const response = await qualityApi.getQualityScoreBatch(adContentIds)

        const updatedVariations = this.adVariations.map(variation => {
          const scoreData = variation.id
            ? response.data.find(s => s.adContentId === variation.id)
            : null
          const qualityDetails = this.prepareQualityDetails(scoreData)
          return {
            ...variation,
            qualityDetails: qualityDetails || variation.qualityDetails || null,
            qualityScore: qualityDetails || variation.qualityScore || null
          }
        })

        this.adVariations = updatedVariations
        this.updateBestQualityHighlights()
      } catch (error) {
        console.error('Error loading quality scores:', error)
        this.$message.warning(this.$t('adCreate.messages.warning.loadQualityScoresFailed'))
      } finally {
        this.loadingQualityScores = false
      }
    },

    updateBestQualityHighlights() {
      const scores = this.adVariations
        .map(v => v.qualityScore?.totalScore)
        .filter(s => s !== null && s !== undefined)

      if (scores.length === 0) {
        this.bestQualityScoreValue = null
        this.bestQualityScore = null
        return
      }

      this.bestQualityScoreValue = Math.max(...scores)
      const bestVariation = this.adVariations.find(
        v => v.qualityScore?.totalScore === this.bestQualityScoreValue
      )

      if (bestVariation && bestVariation.qualityScore) {
        this.bestQualityScore = `${bestVariation.qualityScore.totalScore.toFixed(1)}/100 (${bestVariation.qualityScore.grade})`
      }
    }
  }
}
</script>

<style lang="scss" scoped>
/* Creative Ad Create Styles - Phase 3 Implementation */
.ad-create-page {
  padding: 20px;
  background: linear-gradient(135deg, #fafafa 0%, #f0f2f5 100%);
  min-height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
}

.ad-create-page-header {
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.ad-create-page-header .ant-page-header-heading-title {
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
}

.ad-create-page-header .ant-page-header-heading-sub-title {
  color: #475569;
  font-size: 16px;
}

/* Creative Progress Container */
.creative-progress-container {
  background: white;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 32px;
  box-shadow: 0 4px 20px rgba(45, 90, 160, 0.08);
  border: 1px solid #f0f2f5;
}

.progress-header {
  text-align: center;
  margin-bottom: 28px;
}

.progress-title {
  font-size: 28px;
  font-weight: 700;
  color: #2d5aa0;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #2d5aa0 0%, #1890ff 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.progress-subtitle {
  font-size: 16px;
  color: #8c8c8c;
  font-weight: 500;
}

.creative-progress-tracker {
  position: relative;
  margin-bottom: 24px;
}

.progress-line {
  position: absolute;
  top: 28px;
  left: 32px;
  right: 32px;
  height: 4px;
  background: #f0f2f5;
  border-radius: 2px;
  z-index: 1;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2d5aa0 0%, #1890ff 100%);
  border-radius: 2px;
  transition: width 0.6s ease;
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 3px solid #f0f2f5;
  margin-bottom: 12px;
  transition: all 0.4s ease;
  position: relative;
  z-index: 3;
}

.progress-step.completed .step-circle {
  background: #2d5aa0;
  border-color: #2d5aa0;
  color: white;
}

.progress-step.current .step-circle {
  background: #1890ff;
  border-color: #1890ff;
  color: white;
  animation: pulse-step 2s ease-in-out infinite;
}

@keyframes pulse-step {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.step-check,
.step-number {
  font-size: 18px;
  font-weight: 700;
}

.step-content {
  text-align: center;
  max-width: 120px;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.step-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.3;
  margin-bottom: 8px;
}

.step-emoji {
  font-size: 20px;
  animation: bounce-emoji 1.5s ease-in-out infinite;
}

@keyframes bounce-emoji {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.fun-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
  border-radius: 12px;
  border: 1px solid #ffd6cc;
}

.progress-character {
  font-size: 24px;
  animation: character-float 2s ease-in-out infinite;
}

@keyframes character-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.progress-message {
  font-size: 16px;
  font-weight: 600;
  color: #d46b08;
}

/* AI Configuration Creative Styling */
.ai-config-container {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(45, 90, 160, 0.08);
  border: 1px solid #f0f2f5;
}

.ai-section-header {
  text-align: center;
  margin-bottom: 40px;
}

.section-title-magic {
  font-size: 32px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 12px 0;
  line-height: 1.2;
}

.section-subtitle-magic {
  font-size: 16px;
  color: #8c8c8c;
  margin: 0;
}

.provider-section {
  margin-bottom: 40px;
}

.provider-section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f2f5;
}

.section-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border-radius: 12px;
  border: 2px solid #91d5ff;
}

.provider-title {
  font-size: 22px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 4px 0;
}

.provider-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.creative-provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.creative-provider-card {
  background: white;
  border: 2px solid #f0f2f5;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.creative-provider-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(24, 144, 255, 0.02) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.creative-provider-card:hover::before {
  opacity: 1;
}

.creative-provider-card:hover {
  border-color: #91d5ff;
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.12);
  transform: translateY(-2px);
}

.creative-provider-card.selected {
  border-color: #1890ff;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  box-shadow: 0 8px 32px rgba(24, 144, 255, 0.2);
}

.creative-provider-card.selected::after {
  content: '✓';
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.creative-provider-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.provider-visual {
  position: relative;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.provider-icon {
  font-size: 32px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
  border-radius: 12px;
  border: 2px solid #ffadd2;
}

.provider-status {
  font-size: 18px;
}

.provider-info {
  flex: 1;
}

.provider-name {
  font-size: 18px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 8px 0;
}

.provider-desc {
  font-size: 14px;
  color: #595959;
  line-height: 1.4;
  margin: 0 0 16px 0;
}

.provider-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.feature-badge {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  color: #0958d9;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid #91d5ff;
}

.provider-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.rating-text {
  font-size: 12px;
  font-weight: 600;
  color: #f4a261;
}

.provider-selection {
  position: absolute;
  top: 20px;
  right: 20px;
}

.selection-indicator {
  width: 20px;
  height: 20px;
  border: 2px solid #d9d9d9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.creative-provider-card.selected .selection-indicator {
  background: #1890ff;
  border-color: #1890ff;
}

.selection-dot {
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
}

/* AI Power Level Indicator */
.ai-power-indicator {
  background: linear-gradient(135deg, #fff8e1 0%, #fff1b0 100%);
  border: 2px solid #ffd666;
  border-radius: 16px;
  padding: 20px;
  margin-top: 32px;
}

.power-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.power-icon {
  font-size: 20px;
}

.power-title {
  font-size: 16px;
  font-weight: 700;
  color: #d46b08;
}

.power-bar {
  background: rgba(255, 255, 255, 0.8);
  height: 12px;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
}

.power-fill {
  height: 100%;
  background: linear-gradient(90deg, #ffd666 0%, #f4a261 100%);
  border-radius: 6px;
  transition: width 0.6s ease;
}

.power-message {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
  text-align: center;
}

/* Step Content Styling */
.step-content {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(45, 90, 160, 0.08);
  border: 1px solid #f0f2f5;
}

.enhanced-card {
  border: none;
  box-shadow: none;
  background: transparent;
}

.enhanced-card :deep(.ant-card-head) {
  border: none;
  padding: 0 0 24px 0;
}

.enhanced-card :deep(.ant-card-head-title) {
  font-size: 24px;
  font-weight: 700;
  color: #2d5aa0;
}

.enhanced-card :deep(.ant-card-body) {
  padding: 0;
}

/* Step Navigation */
.step-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f2f5;
}

.step-navigation .ant-btn {
  height: 48px;
  border-radius: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-navigation .ant-btn-primary {
  background: linear-gradient(135deg, #2d5aa0 0%, #1890ff 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);
}

.step-navigation .ant-btn-primary:hover {
  background: linear-gradient(135deg, #1e3a6f 0%, #0d7cc0 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
}

/* Mobile Responsiveness - Phase 2 Enhanced */
@media (max-width: 768px) {
  .ad-create-page {
    padding: 16px;
  }

  .creative-progress-container {
    padding: 20px;
  }

  .progress-title {
    font-size: 24px;
  }

  .progress-steps {
    gap: 8px;
  }

  .step-circle {
    width: 44px;
    height: 44px;
  }

  .step-content {
    max-width: 90px;
  }

  .step-title {
    font-size: 12px;
  }

  .step-desc {
    font-size: 10px;
  }

  .ai-config-container {
    padding: 24px;
  }

  .section-title-magic {
    font-size: 26px;
  }

  .creative-provider-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .creative-provider-card {
    padding: 20px;
  }

  .provider-icon {
    width: 44px;
    height: 44px;
    font-size: 24px;
  }

  .step-navigation {
    flex-direction: column;
    gap: 12px;
  }

  .step-navigation .ant-btn {
    width: 100%;
    height: 44px;
  }
}

@media (max-width: 480px) {
  .progress-line {
    left: 22px;
    right: 22px;
  }

  .step-circle {
    width: 36px;
    height: 36px;
  }

  .step-check,
  .step-number {
    font-size: 14px;
  }

  .section-title-magic {
    font-size: 22px;
  }

  .provider-title {
    font-size: 18px;
  }
}

  .step-navigation .ant-btn:first-child {
    order: 1;
  }

@media (max-width: 480px) {
  .ad-create-page {
    padding: 8px;
  }

  .step-content {
    padding: 0.75rem;
  }

  .wizard-progress {
    padding: 0.75rem;
  }

  .ai-provider-card {
    min-height: 120px;
    padding: 0.75rem;
  }
}

.nav-item:hover,
.nav-item.active {
  background: #e6f7ff;
  color: #1890ff;
  border-right: 3px solid #1890ff;
}

.main-content {
  flex: 1;
  margin-left: 0;
  transition: margin-left 0.3s ease;
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.mobile-header h1 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.step-content {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.wizard-progress {
  padding: 2rem;
  background: white;
  margin-bottom: 2rem;
}

.step-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #f0f0f0;
}

.ai-provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-top: 1rem;
}

.ai-provider-card {
  border: 2px solid #e5e7eb;
  border-radius: 1rem;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.ai-provider-card:hover {
  border-color: #1890ff;
  box-shadow: 0 10px 25px rgba(24, 144, 255, 0.1);
  border-color: #6b8499;
}

.ai-provider-card.selected {
  border-color: #2d5aa0;
  background: #f0f4f7;
  box-shadow: 0 3px 12px rgba(45, 90, 160, 0.12);
}

.ai-provider-card.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.provider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.provider-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.provider-features {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1rem;
}

.ad-preview-container {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
}

.preview-instruction {
  font-size: 1.125rem;
  color: #666;
  margin-bottom: 1.5rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.instruction-help-btn {
  color: #1890ff;
  padding: 0;
  min-width: auto;
  height: 28px;
}

.instruction-help-btn:hover,
.instruction-help-btn:focus {
  color: #40a9ff;
}

.unsaved-tip {
  margin-top: 12px;
  color: #fa8c16;
  font-weight: 500;
}

.ad-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 2rem;
  width: 100%;
}

.ad-preview-card {
  border: 2px solid #e5e7eb;
  border-radius: 1.5rem;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.ad-preview-card:hover {
  border-color: #52c41a;
  box-shadow: 0 20px 40px rgba(82, 196, 26, 0.1);
  border-color: #4a8c4a;
}

.ad-preview-card.selected {
  border-color: #4a8c4a;
  background: #f2f8f2;
  box-shadow: 0 3px 12px rgba(74, 140, 74, 0.12);
}

.ad-preview-card.unsaved-highlight {
  border-color: #faad14;
  box-shadow: 0 0 0 3px rgba(250, 173, 20, 0.3), 0 12px 24px rgba(250, 173, 20, 0.25);
  animation: unsaved-highlight-pulse 0.8s ease-in-out alternate;
}

@keyframes unsaved-highlight-pulse {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-3px);
  }
}

.ad-preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ad-preview-image {
  width: 192px;
  height: 128px;
  border-radius: 1rem;
  overflow: hidden;
  margin-bottom: 1rem;
  background: #f4f4f2;
  border: 2px solid #e8e8e4;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ad-preview-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.ad-preview-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ad-preview-headline {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.3;
  margin-bottom: 0.5rem;
}

.ad-preview-primary-text {
  font-size: 1rem;
  color: #4b5563;
  line-height: 1.5;
  margin-bottom: 0.5rem;
  flex: 1;
}

.ad-preview-description {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 1rem;
}

.ad-preview-cta {
  background: #2d5aa0;
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  border: 1px solid #274d89;
  font-weight: 600;
  text-align: center;
  transition: all 0.2s ease;
}

.ad-preview-cta:hover {
  background: #274d89;
  border-color: #1f3d6b;
}

.ad-preview-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f0f0f0;
}

.lead-form-questions {
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  padding: 1rem;
  background: #f9fafb;
}

.question-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 0.375rem;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
}

.ad-links-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ad-link-item {
  margin-bottom: 0.5rem;
}

.confirm-preview {
  background: #f9fafb;
  padding: 1rem;
  border-radius: 0.5rem;
  margin-top: 1rem;
}

.confirm-preview h4 {
  margin: 0 0 0.5rem 0;
  font-weight: 600;
}

.confirm-preview p {
  margin: 0;
  color: #666;
}

.media-modal-content {
  text-align: center;
}

.ad-types-help {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.ad-type-help-item {
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.75rem;
  background: #f9fafb;
}

.ad-type-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.ad-type-desc {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.75rem;
  line-height: 1.5;
}

.ad-type-features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ad-type-features li {
  font-size: 0.875rem;
  color: #4b5563;
  padding: 0.25rem 0;
  position: relative;
  padding-left: 1.25rem;
}

.ad-type-features li::before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #52c41a;
  font-weight: bold;
}

.help-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.help-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.quality-help-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.help-intro {
  font-size: 0.95rem;
  color: #6b7280;
  margin: 0;
}

.quality-help-content ul {
  margin: 0.5rem 0 0 1.25rem;
  padding: 0;
  color: #4b5563;
}

.quality-help-content li {
  margin-bottom: 0.25rem;
}

.help-footnote {
  font-size: 0.85rem;
  color: #9ca3af;
  margin-top: -0.5rem;
}

.help-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.help-text {
  font-size: 1rem;
  color: #6b7280;
  line-height: 1.6;
}

.help-steps {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.help-step {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.help-step .step-number {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  flex-shrink: 0;
}

.help-step .step-content h4 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.25rem;
}

.help-step .step-content p {
  font-size: 0.875rem;
  color: #6b7280;
  line-height: 1.5;
}

/* Responsive Design */
@media (max-width: 768px) {
  .ai-provider-grid {
    grid-template-columns: 1fr;
  }
  
  .ad-preview-grid {
    grid-template-columns: 1fr;
  }
  
  .ad-preview-card {
    min-height: 350px;
  }
  
  .step-navigation {
    flex-direction: column;
    gap: 1rem;
  }
  
  .step-content {
    padding: 1rem;
  }
}

@media (min-width: 1024px) {
  .main-content {
    margin-left: 250px;
  }
  
  .sidebar {
    position: relative;
    left: 0;
  }
  
  .mobile-header {
    display: none;
  }
  
  .ai-provider-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .ad-preview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1280px) {
  .ad-preview-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Quality Score Integration Styles */
.quality-score-summary {
  background: linear-gradient(135deg, #fff8e1 0%, #fff1b0 100%);
  border: 2px solid #ffd666;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-icon {
  font-size: 32px;
  animation: pulse-icon 2s ease-in-out infinite;
}

@keyframes pulse-icon {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.summary-content {
  display: flex;
  flex-direction: column;
}

.summary-label {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #d46b08;
}

.summary-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #d46b08;
  font-weight: 600;
}

.best-quality-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ffd666 0%, #f4a261 100%);
  color: #8c4a00;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(255, 214, 102, 0.4);
  z-index: 10;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.needs-review-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(250, 173, 20, 0.3);
}

.ad-preview-card.best-quality {
  border-color: #ffd666;
  box-shadow: 0 8px 32px rgba(255, 214, 102, 0.3);
}

.ad-preview-card.best-quality:hover {
  border-color: #f4a261;
  box-shadow: 0 12px 40px rgba(255, 214, 102, 0.4);
}

.quality-score-wrapper {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}

.quality-score-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: #8c8c8c;
  font-size: 14px;
}

/* Adjust ad preview card to accommodate quality score */
.ad-preview-card {
  position: relative;
  padding-bottom: 1rem;
}

/* Mobile responsive adjustments for quality score */
@media (max-width: 768px) {
  .quality-score-summary {
    flex-direction: column;
    padding: 16px;
  }

  .summary-icon {
    font-size: 24px;
  }

  .summary-value {
    font-size: 20px;
  }

  .best-quality-badge {
    font-size: 11px;
    padding: 4px 8px;
  }
}

/* Issue #8: Creative Style Dropdown and Preview */
.style-option {
  padding: 8px 0;
}

.creative-style-item :deep(.ant-form-item-label > label) {
  display: flex;
  align-items: center;
  line-height: 1.5;
  padding-bottom: 4px;
}

.style-label {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
  margin-bottom: 4px;
}

.style-description {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

.style-preview {
  display: flex;
  align-items: flex-start;
  margin-top: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f4ff 100%);
  border-radius: 8px;
  border: 1px solid #91caff;
}

.preview-icon {
  font-size: 24px;
  margin-right: 12px;
  flex-shrink: 0;
}

.preview-content {
  flex: 1;
}

.preview-title {
  font-weight: 600;
  font-size: 13px;
  color: #1677ff;
  margin-bottom: 4px;
}

.preview-desc {
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
}

/* Mobile responsive for style preview */
@media (max-width: 768px) {
  .style-preview {
    padding: 10px 12px;
  }

  .preview-icon {
    font-size: 20px;
    margin-right: 8px;
  }

  .preview-title {
    font-size: 12px;
  }

  .preview-desc {
    font-size: 11px;
  }
}

/* New Provider Selection Table Styles */
.provider-selection-table {
  margin: 24px 0;
}

.variation-label {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}

.variation-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
  font-size: 14px;
  font-weight: 700;
}

.variation-text {
  color: #262626;
  font-size: 14px;
}

.provider-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.provider-option-name {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.provider-option-description {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.3;
  white-space: normal;
}

/* Provider Info Section */
.provider-info-section {
  margin-top: 32px;
}

.provider-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.provider-info-card {
  background: white;
  border: 1px solid #f0f2f5;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.provider-info-card:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
}

.provider-info-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.provider-info-card.disabled:hover {
  border-color: #f0f2f5;
  box-shadow: none;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.info-name {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.info-description {
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
  margin-bottom: 0;
}

.async-progress-container {
  text-align: center;
}

.async-progress-container .progress-icon {
  margin-bottom: 20px;
}

.progress-status {
  text-align: center;
  margin-bottom: 12px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: #f0f5ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  text-transform: capitalize;
}

.async-progress-container .progress-details,
.async-progress-container .progress-jobid {
  margin-top: 12px;
  font-size: 13px;
  color: #64748b;
}

.async-progress-container .progress-actions {
  margin-top: 20px;
}

/* Per-Variation Upload Section */
.variation-upload-section {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 20px;
  margin: 12px 0;
}

.variation-upload-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-section {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
}

.upload-section h5 {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-info-section {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  transition: opacity 0.3s ease;
}

.ai-info-section h5 {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-info-section p {
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin: 8px 0;
}

.ai-info-section.disabled {
  opacity: 0.5;
}

/* Mobile responsive for provider selection */
@media (max-width: 768px) {
  .provider-info-grid {
    grid-template-columns: 1fr;
  }

  .variation-label {
    gap: 8px;
  }

  .variation-number {
    width: 24px;
    height: 24px;
    font-size: 12px;
  }

  .variation-text {
    font-size: 13px;
  }

  /* Per-variation upload responsive */
  .variation-upload-section {
    padding: 16px;
  }

  .variation-upload-section > div[style*="grid"] {
    grid-template-columns: 1fr !important;
    gap: 12px !important;
  }

  .upload-section,
  .ai-info-section {
    padding: 12px;
  }
}
</style>
