<template>
  <div class="website-conversion-config">
    <h2 class="text-xl font-bold mb-4">Cấu hình Website Conversion Ad</h2>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">URL website</label>
      <input 
        type="url" 
        v-model="formData.websiteUrl" 
        class="w-full px-3 py-2 border rounded-md"
        placeholder="https://example.com"
      />
    </div>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">Facebook Pixel ID</label>
      <input 
        type="text" 
        v-model="formData.pixelId" 
        class="w-full px-3 py-2 border rounded-md"
        placeholder="123456789012345"
      />
    </div>
    
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">Sự kiện chuyển đổi</label>
      
      <div class="space-y-2">
        <div class="flex items-center">
          <input 
            type="checkbox" 
            id="event-registration" 
            v-model="conversionEvents.REGISTRATION" 
            class="form-checkbox h-5 w-5 mr-2"
          />
          <label for="event-registration" class="text-gray-700">Đăng ký</label>
        </div>
        
        <div class="flex items-center">
          <input 
            type="checkbox" 
            id="event-pageview" 
            v-model="conversionEvents.PAGE_VIEW" 
            class="form-checkbox h-5 w-5 mr-2"
          />
          <label for="event-pageview" class="text-gray-700">Xem trang</label>
        </div>
      </div>
    </div>
    
    <div class="flex justify-between">
      <button 
        class="btn btn-secondary px-4 py-2"
        @click="$emit('back')">
        Quay lại
      </button>
      <button 
        class="btn btn-primary px-6 py-2"
        @click="saveAndContinue">
        Lưu và tiếp tục
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WebsiteConversionConfig',
  props: {
    initialData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      formData: {
        websiteUrl: '',
        pixelId: '',
      },
      conversionEvents: {
        REGISTRATION: false,
        PAGE_VIEW: false
      }
    }
  },
  created() {
    if (this.initialData && Object.keys(this.initialData).length > 0) {
      this.formData = { 
        websiteUrl: this.initialData.websiteUrl || '',
        pixelId: this.initialData.pixelId || ''
      };
      
      if (this.initialData.conversionEvents && this.initialData.conversionEvents.length > 0) {
        this.initialData.conversionEvents.forEach(event => {
          this.conversionEvents[event] = true;
        });
      }
    }
  },
  methods: {
    saveAndContinue() {
      const selectedEvents = Object.keys(this.conversionEvents)
        .filter(key => this.conversionEvents[key]);
      
      const data = {
        ...this.formData,
        conversionEvents: selectedEvents
      };
      
      this.$emit('save', data);
    }
  }
}
</script>
