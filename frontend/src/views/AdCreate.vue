<template>
  <div class="app-layout">
    <div class="flex flex-1">
      <!-- Sidebar -->
      <aside class="app-sidebar" :class="{ open: sidebarOpen }">
        <div class="sidebar-header">
          <router-link to="/dashboard" class="flex items-center gap-3 px-6 py-4">
            <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
              </svg>
            </div>
            <h1 class="text-lg font-bold text-secondary-900">AI Ads Creator</h1>
          </router-link>
        </div>

        <nav class="nav">
          <router-link to="/dashboard" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z"></path>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5a2 2 0 012-2h4a2 2 0 012 2v6H8V5z"></path>
            </svg>
            <span class="nav-text">Dashboard</span>
          </router-link>
          
          <router-link to="/campaigns" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
            </svg>
            <span class="nav-text">Campaigns</span>
          </router-link>
          
          <router-link to="/ads" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
            </svg>
            <span class="nav-text">Ads</span>
          </router-link>
        </nav>

        <div class="sidebar-footer">
          <div class="px-6 py-4">
            <div class="flex items-center gap-3 mb-3">
              <div class="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                <span class="text-primary-700 font-medium text-sm">{{ userInitials }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-secondary-900 truncate">{{ userName }}</p>
                <p class="text-xs text-secondary-500 truncate">{{ userEmail }}</p>
              </div>
            </div>
            <button @click="logout" class="btn btn-sm btn-ghost w-full">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
              </svg>
              Logout
            </button>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main-content">
        <!-- Mobile Header -->
        <div class="mobile-header lg:hidden">
          <button @click="sidebarOpen = !sidebarOpen" class="btn btn-sm btn-ghost">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
          <h1 class="text-lg font-semibold text-secondary-900">Tạo Quảng Cáo</h1>
        </div>

        <div class="content-wrapper">
          <div class="max-w-4xl mx-auto">
            <!-- Header -->
            <div class="mb-8">
              <div class="flex items-center gap-4 mb-4">
                <router-link to="/ads" class="btn btn-sm btn-ghost">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                  Quay lại
                </router-link>
                <div>
                  <h1 class="text-2xl font-bold text-secondary-900">Tạo Quảng Cáo Mới</h1>
                  <p class="text-secondary-600">Tạo nội dung quảng cáo với sự hỗ trợ của AI</p>
                </div>
              </div>

              <!-- Progress Steps -->
              <div class="flex items-center justify-center mb-8">
                <div class="flex items-center space-x-4">
                  <div class="flex items-center">
                    <div :class="['w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium', 
                                  currentStep >= 1 ? 'bg-primary-600 text-white' : 'bg-neutral-200 text-neutral-500']">
                      1
                    </div>
                    <span class="ml-2 text-sm font-medium text-secondary-900">Thông tin cơ bản</span>
                  </div>
                  <div class="w-16 h-0.5 bg-neutral-200"></div>
                  <div class="flex items-center">
                    <div :class="['w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium', 
                                  currentStep >= 2 ? 'bg-primary-600 text-white' : 'bg-neutral-200 text-neutral-500']">
                      2
                    </div>
                    <span class="ml-2 text-sm font-medium text-secondary-900">Chọn AI Provider</span>
                  </div>
                  <div class="w-16 h-0.5 bg-neutral-200"></div>
                  <div class="flex items-center">
                    <div :class="['w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium', 
                                  currentStep >= 3 ? 'bg-primary-600 text-white' : 'bg-neutral-200 text-neutral-500']">
                      3
                    </div>
                    <span class="ml-2 text-sm font-medium text-secondary-900">Preview & Lưu</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Step 1: Basic Information -->
            <div v-if="currentStep === 1" class="card">
              <div class="card-header">
                <h2 class="card-title">Bước 1: Thông tin cơ bản</h2>
                <p class="card-description">Điền thông tin cơ bản cho quảng cáo của bạn</p>
              </div>
              <div class="card-body space-y-6">
                <!-- Campaign Selection -->
                <div>
                  <label class="form-label">Chọn Campaign <span class="text-red-500">*</span></label>
                  <Dropdown v-model="formData.campaignId" :options="campaigns" optionLabel="name" optionValue="id" placeholder="Chọn campaign..." class="w-full" :class="{ 'p-invalid': !formData.campaignId && showValidation }" required />
                  <p v-if="!formData.campaignId && showValidation" class="form-error">Vui lòng chọn campaign</p>
                </div>

                <!-- Ad Type Selection -->
                <div>
                  <label class="form-label">Loại quảng cáo <span class="text-red-500">*</span></label>
                  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div v-for="type in adTypes" :key="type.value" 
                         :class="['border-2 rounded-lg p-4 cursor-pointer transition-all', 
                                  formData.adType === type.value ? 'border-primary-500 bg-primary-50' : 'border-neutral-200 hover:border-neutral-300']"
                         @click="formData.adType = type.value">
                      <div class="flex items-center space-x-3">
                        <div :class="['w-4 h-4 rounded-full border-2', 
                                      formData.adType === type.value ? 'border-primary-500 bg-primary-500' : 'border-neutral-300']">
                          <div v-if="formData.adType === type.value" class="w-2 h-2 bg-white rounded-full mx-auto mt-0.5"></div>
                        </div>
                        <div>
                          <h3 class="font-medium text-secondary-900">{{ type.label }}</h3>
                          <p class="text-sm text-secondary-600">{{ type.description }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  <p v-if="!formData.adType && showValidation" class="form-error">Vui lòng chọn loại quảng cáo</p>
                </div>

                <!-- Ad Name -->
                <div>
                  <label class="form-label">Tên quảng cáo <span class="text-red-500">*</span></label>
                  <InputText v-model="formData.name" class="w-full" :class="{ 'p-invalid': !formData.name && showValidation }" placeholder="Nhập tên quảng cáo..." required />
                  <p v-if="!formData.name && showValidation" class="form-error">Vui lòng nhập tên quảng cáo</p>
                </div>

                <!-- Prompt -->
                <div>
                  <label class="form-label">Nội dung prompt <span class="text-red-500">*</span></label>
                  <Textarea v-model="formData.prompt" rows="4" class="w-full" :class="{ 'p-invalid': !formData.prompt && showValidation }" placeholder="Ví dụ: Quảng cáo về sản phẩm chất tẩy rửa thân thiện với môi trường..." required></Textarea>
                  <p v-if="!formData.prompt && showValidation" class="form-error">Vui lòng nhập nội dung prompt</p>
                </div>

                <div class="mb-4">
                  <label for="language" class="block text-sm font-medium text-gray-700 mb-1">Ngôn ngữ</label>
                  <Dropdown v-model="formData.language" :options="[{label: 'Tiếng Việt', value: 'vi'}, {label: 'English', value: 'en'}]" optionLabel="label" optionValue="value" id="language" class="w-full" />
                </div>

                <!-- Number of Variations -->
                <div>
                  <label class="form-label">Số lượng quảng cáo muốn tạo <span class="text-red-500">*</span></label>
                  <input
                    type="number"
                    v-model.number="formData.numberOfVariations"
                    id="numberOfVariations"
                    class="w-full p-2 border border-gray-300 rounded-md"
                    :class="{ 'border-red-500': !formData.numberOfVariations && showValidation }"
                    placeholder="Mặc định là 1"
                    min="1"
                    max="10"
                    required
                  />
                  <p v-if="!formData.numberOfVariations && showValidation" class="form-error">Vui lòng nhập số lượng quảng cáo muốn tạo</p>
                </div>

                <!-- Media Upload -->
                <div>
                  <label class="form-label">File phương tiện (tùy chọn)</label>
                  <div class="border-2 border-dashed border-neutral-300 rounded-lg p-6 text-center">
                    <input ref="fileInput" type="file" class="hidden" accept="image/*,video/*" @change="handleFileUpload">
                    <div v-if="!uploadedFile">
                      <svg class="w-12 h-12 text-neutral-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                      </svg>
                      <p class="text-secondary-600 mb-2">Kéo thả file hoặc click để chọn</p>
                      <p class="text-sm text-secondary-500">Hỗ trợ: JPG, PNG, MP4 (tối đa 10MB)</p>
                      <button type="button" @click="$refs.fileInput.click()" class="btn btn-sm btn-secondary mt-3">
                        Chọn file
                      </button>
                    </div>
                    <div v-else class="flex items-center justify-between">
                      <div class="flex items-center space-x-3">
                        <svg class="w-8 h-8 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                        <div>
                          <p class="font-medium text-secondary-900">{{ uploadedFile.name }}</p>
                          <p class="text-sm text-secondary-500">{{ formatFileSize(uploadedFile.size) }}</p>
                        </div>
                      </div>
                      <button type="button" @click="removeFile" class="btn btn-sm btn-ghost text-red-600">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Actions -->
                <div class="flex justify-end">
                  <button @click="nextStep" class="btn btn-primary">
                    Tiếp theo
                    <svg class="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Step 2: AI Provider Selection -->
            <div v-if="currentStep === 2" class="card">
              <div class="card-header">
                <h2 class="card-title">Bước 2: Chọn AI Provider</h2>
                <p class="card-description">Chọn các AI provider để tạo nội dung quảng cáo</p>
              </div>
              <div class="card-body space-y-6">
                <!-- Text Provider -->
                <div>
                  <label class="form-label">Text AI Provider <span class="text-red-500">*</span></label>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div v-for="provider in textProviders" :key="provider.id" 
                         :class="['border-2 rounded-lg p-4 cursor-pointer transition-all', 
                                  formData.textProvider === provider.id ? 'border-primary-500 bg-primary-50' : 'border-neutral-200 hover:border-neutral-300']"
                         @click="formData.textProvider = provider.id">
                      <div class="flex items-start space-x-3">
                        <div :class="['w-4 h-4 rounded-full border-2 mt-1', 
                                      formData.textProvider === provider.id ? 'border-primary-500 bg-primary-500' : 'border-neutral-300']">
                          <div v-if="formData.textProvider === provider.id" class="w-2 h-2 bg-white rounded-full mx-auto mt-0.5"></div>
                        </div>
                        <div>
                          <h3 class="font-medium text-secondary-900">{{ provider.name }}</h3>
                          <p class="text-sm text-secondary-600">{{ provider.description }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  <p v-if="!formData.textProvider && showValidation" class="form-error">Vui lòng chọn text provider</p>
                </div>

                <!-- Image Provider -->
                <div v-if="!uploadedFile">
                  <label class="form-label">Image AI Provider <span class="text-red-500">*</span></label>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div v-for="provider in imageProviders" :key="provider.id" 
                         :class="['border-2 rounded-lg p-4 cursor-pointer transition-all', 
                                  formData.imageProvider === provider.id ? 'border-primary-500 bg-primary-50' : 'border-neutral-200 hover:border-neutral-300']"
                         @click="formData.imageProvider = provider.id">
                      <div class="flex items-start space-x-3">
                        <div :class="['w-4 h-4 rounded-full border-2 mt-1', 
                                      formData.imageProvider === provider.id ? 'border-primary-500 bg-primary-500' : 'border-neutral-300']">
                          <div v-if="formData.imageProvider === provider.id" class="w-2 h-2 bg-white rounded-full mx-auto mt-0.5"></div>
                        </div>
                        <div>
                          <h3 class="font-medium text-secondary-900">{{ provider.name }}</h3>
                          <p class="text-sm text-secondary-600">{{ provider.description }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  <p v-if="!uploadedFile && !formData.imageProvider && showValidation" class="form-error">Vui lòng chọn image provider</p>
                </div>

                <div v-else class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <div class="flex items-center space-x-3">
                    <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <p class="text-blue-800">Bạn đã upload file media, không cần chọn image provider.</p>
                  </div>
                </div>

                <!-- Actions -->
                <div class="flex justify-between">
                  <button @click="prevStep" class="btn btn-secondary">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                    </svg>
                    Quay lại
                  </button>
                  <button @click="generateAd" :disabled="isGenerating" class="btn btn-primary">
                    <svg v-if="isGenerating" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    {{ isGenerating ? 'Đang tạo quảng cáo...' : 'Tạo quảng cáo' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Step 3: Preview & Save -->
            <div v-if="currentStep === 3" class="space-y-6">
              <div class="card">
                <div class="card-header">
                  <h2 class="card-title">Bước 3: Preview & Lưu</h2>
                  <p class="card-description">Chọn và chỉnh sửa quảng cáo bạn muốn lưu</p>
                </div>
              </div>

              <!-- Ad Previews -->
              <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div v-for="(variation, index) in adVariations" :key="variation.id" 
                     :class="['card cursor-pointer transition-all', 
                              selectedVariation?.id === variation.id ? 'ring-2 ring-primary-500 bg-primary-50' : 'hover:shadow-lg']"
                     @click="selectVariation(variation)">
                  <div class="card-body">
                    <div class="flex items-start justify-between mb-4">
                      <h3 class="font-semibold text-secondary-900">Tùy chọn {{ index + 1 }}</h3>
                      <div :class="['w-5 h-5 rounded-full border-2', 
                                    selectedVariation?.id === variation.id ? 'border-primary-500 bg-primary-500' : 'border-neutral-300']">
                        <div v-if="selectedVariation?.id === variation.id" class="w-3 h-3 bg-white rounded-full mx-auto mt-0.5"></div>
                      </div>
                    </div>
                    
                    <!-- Preview Content -->
                    <div class="space-y-3">
                      <div v-if="variation.imageUrl" class="aspect-video bg-neutral-100 rounded-lg overflow-hidden">
                        <img :src="variation.imageUrl" :alt="variation.headline" class="w-full h-full object-cover">
                      </div>
                      <div>
                        <h4 class="font-semibold text-secondary-900 mb-1">{{ variation.headline }}</h4>
                        <p class="text-sm text-secondary-600 mb-2">{{ variation.primaryText }}</p>
                        <p class="text-xs text-secondary-500 mb-3">{{ variation.description }}</p>
                        <button class="btn btn-xs btn-primary">{{ variation.callToAction || 'Tìm hiểu thêm' }}</button>
                      </div>
                    </div>

                    <!-- Edit Button -->
                    <div class="mt-4 pt-4 border-t border-neutral-200">
                      <button @click.stop="editVariation(variation)" class="btn btn-sm btn-ghost w-full">
                        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                        </svg>
                        Chỉnh sửa
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Actions -->
              <div class="flex justify-between">
                <button @click="prevStep" class="btn btn-secondary">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                  Quay lại
                </button>
                <button @click="saveAd" :disabled="!selectedVariation || isSaving" class="btn btn-success">
                  <svg v-if="isSaving" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ isSaving ? 'Đang lưu...' : 'Lưu quảng cáo' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- Edit Modal -->
    <div v-if="showEditModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md mx-4">
        <h3 class="text-lg font-semibold text-secondary-900 mb-4">Chỉnh sửa nội dung</h3>
        
        <div class="space-y-4">
          <div>
            <label class="form-label">Tiêu đề</label>
            <input v-model="editingVariation.headline" type="text" class="form-input">
          </div>
          
          <div>
            <label class="form-label">Nội dung chính</label>
            <textarea v-model="editingVariation.primaryText" class="form-textarea" rows="3"></textarea>
          </div>
          
          <div>
            <label class="form-label">Mô tả</label>
            <textarea v-model="editingVariation.description" class="form-textarea" rows="2"></textarea>
          </div>
          
          <div>
            <label class="form-label">Call to Action</label>
            <input v-model="editingVariation.callToAction" type="text" class="form-input">
          </div>
        </div>

        <div class="flex justify-end space-x-3 mt-6">
          <button @click="cancelEdit" class="btn btn-secondary">Hủy</button>
          <button @click="saveEdit" class="btn btn-primary">Xác nhận</button>
        </div>
      </div>
    </div>

    <!-- Confirmation Modal -->
    <div v-if="showConfirmModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md mx-4">
        <h3 class="text-lg font-semibold text-secondary-900 mb-4">Xác nhận lưu quảng cáo</h3>
        <p class="text-secondary-600 mb-6">Bạn có chắc chắn muốn lưu quảng cáo này không?</p>
        
        <div class="flex justify-end space-x-3">
          <button @click="showConfirmModal = false" class="btn btn-secondary">Không</button>
          <button @click="confirmSave" class="btn btn-primary">Có</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import api from '@/services/api'

export default {
  name: 'AdCreate',
  data() {
    return {
      sidebarOpen: false,
      currentStep: 1,
      showValidation: false,
      isGenerating: false,
      isSaving: false,
      showEditModal: false,
      showConfirmModal: false,
      
      formData: {
        campaignId: '',
        adType: '',
        name: '',
        prompt: '',
        textProvider: '',
        imageProvider: '',
        numberOfVariations: 1,
        language: 'vi'
      },
      
      uploadedFile: null,
      uploadedFileUrl: '',
      
      adTypes: [
        {
          value: 'PAGE_POST_AD',
          label: 'Page Post Ad',
          description: 'Quảng cáo bài đăng trên trang'
        },
        {
          value: 'WEBSITE_CONVERSION_AD',
          label: 'Website Conversion Ad',
          description: 'Quảng cáo chuyển đổi website'
        },
        {
          value: 'LEAD_FORM_AD',
          label: 'Lead Form Ad',
          description: 'Quảng cáo thu thập thông tin khách hàng'
        }
      ],
      
      campaigns: [],
      textProviders: [],
      imageProviders: [],
      adVariations: [],
      selectedVariation: null,
      editingVariation: null
    }
  },
  
  computed: {
    ...mapState('auth', ['user']),
    
    userInitials() {
      if (!this.user?.name) return 'U'
      return this.user.name.split(' ').map(n => n[0]).join('').toUpperCase()
    },
    
    userName() {
      return this.user?.name || 'User'
    },
    
    userEmail() {
      return this.user?.email || ''
    }
  },
  
  async mounted() {
    await this.loadData()
  },
  
  methods: {
    ...mapActions('auth', ['logout']),
    ...mapActions('toast', ['showToast']),
    
    async loadData() {
      try {
        // Load campaigns
        const campaignsResponse = await api.campaigns.getAll(0, 100); // Lấy nhiều campaign hơn để đảm bảo tìm thấy
        this.campaigns = campaignsResponse.data.content || campaignsResponse.data || []
        
        // Load AI providers
        const [textResponse, imageResponse] = await Promise.all([
          api.get('/ai-providers/text'),
          api.get('/ai-providers/image')
        ])
        
        this.textProviders = textResponse.data
        this.imageProviders = imageResponse.data
        
      } catch (error) {
        console.error('Error loading data:', error)
        this.showToast({
          type: 'error',
          message: 'Không thể tải dữ liệu. Vui lòng thử lại.'
        })
      }
    },
    
    nextStep() {
      if (this.currentStep === 1) {
        if (!this.validateStep1()) {
          this.showValidation = true
          return
        }
      }
      
      if (this.currentStep < 3) {
        this.currentStep++
        this.showValidation = false
      }
    },
    
    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
        this.showValidation = false
      }
    },
    
    validateStep1() {
      return this.formData.campaignId && 
             this.formData.adType && 
             this.formData.name && 
             this.formData.prompt &&
             this.formData.numberOfVariations
    },
    
    validateStep2() {
      if (this.uploadedFile) {
        return this.formData.textProvider
      }
      return this.formData.textProvider && this.formData.imageProvider
    },
    
    async handleFileUpload(event) {
      const file = event.target.files[0]
      if (!file) return
      
      // Validate file size (10MB)
      if (file.size > 10 * 1024 * 1024) {
        this.showToast({
          type: 'error',
          message: 'File quá lớn. Vui lòng chọn file nhỏ hơn 10MB.'
        })
        return
      }
      
      try {
        const formData = new FormData()
        formData.append('file', file)
        
        const response = await api.post('/upload/media', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (response.data.success) {
          this.uploadedFile = file
          this.uploadedFileUrl = response.data.fileUrl
          this.showToast({
            type: 'success',
            message: 'Upload file thành công!'
          })
        }
      } catch (error) {
        console.error('Error uploading file:', error)
        this.showToast({
          type: 'error',
          message: 'Không thể upload file. Vui lòng thử lại.'
        })
      }
    },
    
    removeFile() {
      this.uploadedFile = null
      this.uploadedFileUrl = ''
      this.$refs.fileInput.value = ''
    },
    
    formatFileSize(bytes) {
      if (bytes === 0) return '0 Bytes'
      const k = 1024
      const sizes = ['Bytes', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    async generateAd() {
      if (!this.validateStep2()) {
        this.showValidation = true
        return
      }
      
      this.isGenerating = true
      
      try {
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.formData.adType,
          name: this.formData.name,
          prompt: this.formData.prompt,
          language: this.formData.language,
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          mediaFileUrl: this.uploadedFileUrl
        }
        
        const response = await api.post('/ads/generate', requestData)
        
        if (response.data.status === 'success') {
          this.adVariations = response.data.variations
          this.currentStep = 3
          this.showToast({
            type: 'success',
            message: 'Tạo quảng cáo thành công!'
          })
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Error generating ad:', error)
        this.showToast({
          type: 'error',
          message: error.response?.data?.message || 'Không thể tạo quảng cáo. Vui lòng thử lại.'
        })
      } finally {
        this.isGenerating = false
      }
    },
    
    selectVariation(variation) {
      this.selectedVariation = variation
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
        const index = this.adVariations.findIndex(v => v.id === this.editingVariation.id)
        if (index !== -1) {
          this.adVariations[index] = { ...this.editingVariation }
          if (this.selectedVariation?.id === this.editingVariation.id) {
            this.selectedVariation = { ...this.editingVariation }
          }
        }
      }
      this.showEditModal = false
      this.editingVariation = null
    },
    
    saveAd() {
      if (!this.selectedVariation) return
      this.showConfirmModal = true
    },
    
    async confirmSave() {
      this.showConfirmModal = false
      this.isSaving = true
      
      try {
        const response = await api.post(`/ads/${this.adVariations[0].adId || 'temp'}/select-content`, null, {
          params: {
            contentId: this.selectedVariation.id
          }
        })
        
        if (response.data.success) {
          this.showToast({
            type: 'success',
            message: 'Lưu quảng cáo thành công!'
          })
          this.$router.push('/dashboard')
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Error saving ad:', error)
        this.showToast({
          type: 'error',
          message: error.response?.data?.message || 'Không thể lưu quảng cáo. Vui lòng thử lại.'
        })
      } finally {
        this.isSaving = false
      }
    }
  }
}
</script>

<style scoped>
.mobile-header {
  @apply flex items-center gap-4 p-4 border-b border-neutral-200 bg-white;
}

.sidebar-header {
  @apply border-b border-neutral-200;
}

.sidebar-footer {
  @apply mt-auto border-t border-neutral-200;
}

@media (max-width: 1023px) {
  .app-sidebar {
    @apply fixed inset-y-0 left-0 z-50 transform -translate-x-full transition-transform duration-300 ease-in-out;
  }
  
  .app-sidebar.open {
    @apply translate-x-0;
  }
}
</style>

