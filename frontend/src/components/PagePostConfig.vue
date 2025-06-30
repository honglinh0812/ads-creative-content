<template>
  <div class="page-post-config">
    <h2 class="text-xl font-bold mb-4">Cấu hình Page Post Ad</h2>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">Facebook Page ID</label>
      <input 
        type="text" 
        v-model="formData.pageId" 
        class="w-full px-3 py-2 border rounded-md"
        placeholder="123456789012345"
      />
    </div>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">Loại bài đăng</label>
      <select 
        v-model="formData.postType" 
        class="w-full px-3 py-2 border rounded-md"
      >
        <option value="PHOTO">Hình ảnh</option>
        <option value="VIDEO">Video</option>
        <option value="CAROUSEL">Carousel</option>
        <option value="LINK">Link</option>
      </select>
    </div>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">Link URL (nếu có)</label>
      <input 
        type="url" 
        v-model="formData.linkUrl" 
        class="w-full px-3 py-2 border rounded-md"
        placeholder="https://example.com"
      />
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
  name: 'PagePostConfig',
  props: {
    initialData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      formData: {
        pageId: '',
        postType: 'PHOTO',
        linkUrl: '',
        postMessage: ''
      }
    }
  },
  created() {
    if (this.initialData && Object.keys(this.initialData).length > 0) {
      this.formData = { ...this.initialData };
    }
  },
  methods: {
    saveAndContinue() {
      this.$emit('save', this.formData);
    }
  }
}
</script>
