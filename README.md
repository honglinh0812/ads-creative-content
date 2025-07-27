# ads-creative-content
Ads Creative Content is a web application for ads content generation. Just a short description would work. 

## Architecture Overview

### Backend (Java Spring Boot)

### Frontend (Vue.js)

## Setup Instructions

### 1. Backend Setup
1. Install Java 17+ and Maven
2. Cấu hình database PostgreSQL
3. Cấu hình các API key trong `application.properties`:
   ```properties
   scrape.creators.api.key=your-actual-api-key-here
   scrape.creators.api.base.url=https://api.scrapecreators.com/v1
   ```
4. Chạy backend: `mvn spring-boot:run`

### 2. Frontend Setup
1. Cài đặt Node.js 16+
2. `cd frontend`
3. `npm install`
4. `npm run serve`

## Features

- ✅ **Integrated Scraping**: ScrapeCreators API tích hợp trực tiếp vào Java backend
- ✅ **No External Dependencies**: Không cần Python service riêng biệt
- ✅ **Modern UI**: GitHub-style interface với dark mode
- ✅ **AI Content Generation**: OpenAI, Gemini, Hugging Face integration
- ✅ **Image Generation**: Stable Diffusion và các AI image providers
- ✅ **Authentication**: Facebook OAuth2 integration
- ✅ **Responsive Design**: Mobile-friendly interface 
