const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 9000,
    // 本地联调：同源代理到网关，规避浏览器跨域；生产部署时改为直连网关域名
    proxy: {
      '/member': { target: 'http://localhost:8000', changeOrigin: true },
      '/business': { target: 'http://localhost:8000', changeOrigin: true }
    }
  }
})
