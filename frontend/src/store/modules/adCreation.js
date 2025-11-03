export default {
  namespaced: true,

  state: {
    currentStep: 1,
    formData: {
      campaignId: null,
      adType: 'website_conversion',
      name: '',
      numberOfVariations: 3,
      language: 'vi',
      callToAction: '',
      websiteUrl: '',
      leadFormQuestions: [''],
      prompt: '',
      textProvider: 'openai',
      imageProvider: 'openai',
      enhancementOptions: [],
      personaId: null
    },
    adLinks: [''],
    uploadedFiles: [],
    uploadedFileUrl: '',
    enhancedImages: [],
    adVariations: [],
    selectedVariation: null,
    editingVariation: null,
    adId: null,
    savedPrompts: [],
    validation: {
      step1Valid: false,
      step2Valid: false
    },
    progress: {
      step1Complete: false,
      step2Complete: false,
      step3Complete: false
    }
  },

  getters: {
    currentStep: state => state.currentStep,
    formData: state => state.formData,
    adLinks: state => state.adLinks,
    uploadedFiles: state => state.uploadedFiles,
    uploadedFileUrl: state => state.uploadedFileUrl,
    enhancedImages: state => state.enhancedImages,
    adVariations: state => state.adVariations,
    selectedVariation: state => state.selectedVariation,
    editingVariation: state => state.editingVariation,
    adId: state => state.adId,
    isStep1Valid: state => state.validation.step1Valid,
    isStep2Valid: state => state.validation.step2Valid,
    isStep1Complete: state => state.progress.step1Complete,
    isStep2Complete: state => state.progress.step2Complete,
    isStep3Complete: state => state.progress.step3Complete
  },

  mutations: {
    SET_CURRENT_STEP(state, step) {
      state.currentStep = step
    },

    UPDATE_FORM_DATA(state, payload) {
      state.formData = { ...state.formData, ...payload }
    },

    SET_AD_LINKS(state, links) {
      state.adLinks = links
    },

    ADD_AD_LINK(state) {
      state.adLinks.push('')
    },

    REMOVE_AD_LINK(state, index) {
      state.adLinks.splice(index, 1)
    },

    UPDATE_AD_LINK(state, { index, value }) {
      state.adLinks[index] = value
    },

    SET_UPLOADED_FILES(state, files) {
      state.uploadedFiles = files
    },

    SET_UPLOADED_FILE_URL(state, url) {
      state.uploadedFileUrl = url
    },

    SET_ENHANCED_IMAGES(state, images) {
      state.enhancedImages = images
    },

    SET_AD_VARIATIONS(state, variations) {
      state.adVariations = variations
    },

    SET_SELECTED_VARIATION(state, variation) {
      state.selectedVariation = variation
    },

    SET_EDITING_VARIATION(state, variation) {
      state.editingVariation = variation
    },

    UPDATE_VARIATION(state, { index, variation }) {
      if (index >= 0 && index < state.adVariations.length) {
        state.adVariations.splice(index, 1, variation)
        if (state.selectedVariation?.id === variation.id) {
          state.selectedVariation = variation
        }
      }
    },

    SET_AD_ID(state, adId) {
      state.adId = adId
    },

    SET_SAVED_PROMPTS(state, prompts) {
      state.savedPrompts = prompts
    },

    SET_STEP_VALIDATION(state, { step, isValid }) {
      state.validation[`step${step}Valid`] = isValid
    },

    SET_STEP_PROGRESS(state, { step, isComplete }) {
      state.progress[`step${step}Complete`] = isComplete
    },

    ADD_LEAD_FORM_QUESTION(state) {
      state.formData.leadFormQuestions.push('')
    },

    REMOVE_LEAD_FORM_QUESTION(state, index) {
      state.formData.leadFormQuestions.splice(index, 1)
    },

    RESET_STATE(state) {
      state.currentStep = 1
      state.formData = {
        campaignId: null,
        adType: 'website_conversion',
        name: '',
        numberOfVariations: 3,
        language: 'vi',
        callToAction: '',
        websiteUrl: '',
        leadFormQuestions: [''],
        prompt: '',
        textProvider: 'openai',
        imageProvider: 'openai',
        enhancementOptions: [],
        personaId: null
      }
      state.adLinks = ['']
      state.uploadedFiles = []
      state.uploadedFileUrl = ''
      state.enhancedImages = []
      state.adVariations = []
      state.selectedVariation = null
      state.editingVariation = null
      state.adId = null
      state.savedPrompts = []
      state.validation = {
        step1Valid: false,
        step2Valid: false
      }
      state.progress = {
        step1Complete: false,
        step2Complete: false,
        step3Complete: false
      }
    }
  },

  actions: {
    nextStep({ commit, state }) {
      if (state.currentStep < 3) {
        const nextStep = state.currentStep + 1
        commit('SET_CURRENT_STEP', nextStep)
        commit('SET_STEP_PROGRESS', { step: state.currentStep, isComplete: true })
        return nextStep
      }
      return state.currentStep
    },

    prevStep({ commit, state }) {
      if (state.currentStep > 1) {
        const prevStep = state.currentStep - 1
        commit('SET_CURRENT_STEP', prevStep)
        return prevStep
      }
      return state.currentStep
    },

    goToStep({ commit }, step) {
      if (step >= 1 && step <= 3) {
        commit('SET_CURRENT_STEP', step)
      }
    },

    updateFormField({ commit, state }, payload) {
      commit('UPDATE_FORM_DATA', payload)

      // Auto-validate steps when form data changes
      if (state.currentStep === 1) {
        const isValid = !!(
          state.formData.campaignId &&
          state.formData.name &&
          state.formData.adType &&
          (state.formData.prompt || state.adLinks.some(link => link.trim()))
        )
        commit('SET_STEP_VALIDATION', { step: 1, isValid })
      } else if (state.currentStep === 2) {
        const isValid = !!(state.formData.textProvider && state.formData.imageProvider)
        commit('SET_STEP_VALIDATION', { step: 2, isValid })
      }
    },

    validateStep({ commit, state }, step) {
      let isValid = false

      if (step === 1) {
        isValid = !!(
          state.formData.campaignId &&
          state.formData.name &&
          state.formData.adType &&
          (state.formData.prompt || state.adLinks.some(link => link.trim()))
        )
      } else if (step === 2) {
        isValid = !!(state.formData.textProvider && state.formData.imageProvider)
      }

      commit('SET_STEP_VALIDATION', { step, isValid })
      return isValid
    },

    resetAdCreation({ commit }) {
      commit('RESET_STATE')
    }
  }
}
