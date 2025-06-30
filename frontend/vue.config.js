module.exports = {
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'https://oriole-relaxing-humbly.ngrok-free.app',
        changeOrigin: true
      }
    }
  },
  css: {
    loaderOptions: {
      sass: {
        additionalData: `
          @import "@/assets/styles/variables.scss";
        `
      }
    }
  }
}
