<template>
  <div class="ad-type-selection">
    <h1 class="text-2xl font-bold mb-4">Chọn loại quảng cáo</h1>
    
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
      <!-- Page Post Ad -->
      <div 
        class="ad-type-card p-4 border rounded-lg cursor-pointer transition-all"
        :class="{ 'border-primary-500 bg-primary-50': selectedAdType === 'PAGE_POST' }"
        @click="selectAdType('PAGE_POST')">
        <div class="flex flex-col items-center">
          <img src="@/assets/images/page-post-ad.svg" alt="Page Post Ad" class="w-24 h-24 mb-4" />
          <h3 class="text-lg font-semibold">Page Post Ad</h3>
          <p class="text-center text-gray-600 mt-2">
            Quảng cáo bài đăng trên Page Facebook của bạn
          </p>
        </div>
      </div>
      
      <!-- Lead Form Ad -->
      <div 
        class="ad-type-card p-4 border rounded-lg cursor-pointer transition-all"
        :class="{ 'border-primary-500 bg-primary-50': selectedAdType === 'LEAD_FORM' }"
        @click="selectAdType('LEAD_FORM')">
        <div class="flex flex-col items-center">
          <img src="@/assets/images/lead-form-ad.svg" alt="Lead Form Ad" class="w-24 h-24 mb-4" />
          <h3 class="text-lg font-semibold">Lead Form Ad</h3>
          <p class="text-center text-gray-600 mt-2">
            Thu thập thông tin khách hàng tiềm năng
          </p>
        </div>
      </div>
      
      <!-- Website Conversion Ad -->
      <div 
        class="ad-type-card p-4 border rounded-lg cursor-pointer transition-all"
        :class="{ 'border-primary-500 bg-primary-50': selectedAdType === 'WEBSITE_CONVERSION' }"
        @click="selectAdType('WEBSITE_CONVERSION')">
        <div class="flex flex-col items-center">
          <img src="@/assets/images/website-conversion-ad.svg" alt="Website Conversion Ad" class="w-24 h-24 mb-4" />
          <h3 class="text-lg font-semibold">Website Conversion Ad</h3>
          <p class="text-center text-gray-600 mt-2">
            Tăng chuyển đổi trên website của bạn
          </p>
        </div>
      </div>
    </div>
    
    <div class="flex justify-end">
      <button 
        class="btn btn-primary px-6 py-2"
        :disabled="!selectedAdType"
        @click="continueToNextStep">
        Tiếp tục
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdTypeSelection',
  data() {
    return {
      selectedAdType: null
    }
  },
  methods: {
    selectAdType(type) {
      this.selectedAdType = type;
    },
    continueToNextStep() {
      if (this.selectedAdType) {
        this.$emit('ad-type-selected', this.selectedAdType);
      }
    }
  }
}
</script>

<style scoped>
.ad-type-card:hover {
  border-color: var(--color-primary-300);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
</style>
