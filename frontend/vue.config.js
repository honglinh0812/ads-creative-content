const path = require('path')
const fs = require('fs')

module.exports = {
  publicPath: '/',
  devServer: {
    port: 8081,
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://api.linhnh.site:8080',
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
  },
  configureWebpack: {
    performance: {
      hints: false,
      maxEntrypointSize: 512000,
      maxAssetSize: 512000
    },
    optimization: {
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          vendor: {
            test: /[/]node_modules[/]/,
            name: 'vendors',
            chunks: 'all'
          }
        }
      }
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    devtool: process.env.NODE_ENV === 'development' ? 'eval-source-map' : 'source-map'
  },

  chainWebpack: config => {
    config.plugin("html").tap(args => {
      const deployHash = Date.now().toString();
      args[0].meta = {
        ...(args[0].meta || {}),
        "deploy-hash": deployHash,
        "cache-control": "no-cache, no-store, must-revalidate"
      };
      return args;
    });
  },

  // Xoá service worker nếu tồn tại sau build
  pluginOptions: {
    afterBuild: () => {
      const swPath = path.resolve(__dirname, "dist", "service-worker.js");
      if (fs.existsSync(swPath)) {
        console.log("Removing old service-worker.js...");
        fs.unlinkSync(swPath);
      }
    }
  }
}
