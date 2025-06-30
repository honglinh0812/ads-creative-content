<template>
  <div class="lead-form-config">
    <h2 class="text-xl font-bold mb-4">Cấu hình Lead Form</h2>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">Tên form</label>
      <input 
        type="text" 
        v-model="formData.formName" 
        class="w-full px-3 py-2 border rounded-md"
        placeholder="Nhập tên form"
      />
    </div>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">URL chính sách bảo mật</label>
      <input 
        type="url" 
        v-model="formData.privacyPolicyUrl" 
        class="w-full px-3 py-2 border rounded-md"
        placeholder="https://example.com/privacy-policy"
      />
    </div>
    
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">Thông báo cảm ơn</label>
      <textarea 
        v-model="formData.thanksMessage" 
        class="w-full px-3 py-2 border rounded-md"
        rows="3"
        placeholder="Cảm ơn bạn đã gửi thông tin. Chúng tôi sẽ liên hệ lại sớm!"
      ></textarea>
    </div>
    
    <div class="mb-6">
      <h3 class="text-lg font-medium mb-2">Cấu hình trường thông tin</h3>
      
      <div class="overflow-x-auto">
        <table class="min-w-full bg-white border">
          <thead>
            <tr>
              <th class="py-2 px-4 border-b">Trường</th>
              <th class="py-2 px-4 border-b">Tên hiển thị</th>
              <th class="py-2 px-4 border-b">Bắt buộc</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(field, index) in formData.fields" :key="index">
              <td class="py-2 px-4 border-b">{{ getFieldTypeName(field.fieldType) }}</td>
              <td class="py-2 px-4 border-b">
                <input 
                  type="text" 
                  v-model="field.fieldName" 
                  class="w-full px-2 py-1 border rounded"
                />
              </td>
              <td class="py-2 px-4 border-b text-center">
                <input 
                  type="checkbox" 
                  v-model="field.isRequired" 
                  class="form-checkbox h-5 w-5"
                />
              </td>
            </tr>
          </tbody>
        </table>
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
  name: 'LeadFormConfig',
  props: {
    initialData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      formData: {
        formName: '',
        privacyPolicyUrl: '',
        thanksMessage: '',
        fields: [
          { fieldType: 'NAME', fieldName: 'Họ và tên', isRequired: true },
          { fieldType: 'EMAIL', fieldName: 'Email', isRequired: true },
          { fieldType: 'PHONE', fieldName: 'Số điện thoại', isRequired: false },
          { fieldType: 'FACEBOOK', fieldName: 'Facebook', isRequired: false },
          { fieldType: 'LINKEDIN', fieldName: 'LinkedIn', isRequired: false }
        ]
      }
    }
  },
  created() {
    if (this.initialData && Object.keys(this.initialData).length > 0) {
      this.formData = { ...this.initialData };
    }
  },
  methods: {
    getFieldTypeName(fieldType) {
      const fieldTypeMap = {
        'NAME': 'Tên',
        'EMAIL': 'Email',
        'PHONE': 'SĐT',
        'FACEBOOK': 'Facebook',
        'LINKEDIN': 'LinkedIn'
      };
      return fieldTypeMap[fieldType] || fieldType;
    },
    saveAndContinue() {
      this.$emit('save', this.formData);
    }
  }
}
</script>
