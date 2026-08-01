const CompressionPlugin = require('compression-webpack-plugin')

module.exports = {
  transpileDependencies: [
    'naive-ui',
    'vueuc',
    '@css-render',
    'css-render',
    '@juggle',
    'vooks',
    'evtd',
    'element-plus',
    '@ctrl',
    '@vueuse',
    'async-validator',
    'date-fns',
    'vue-router'
  ],
  devServer: {
    port: 81,
    https: false,
    open: false
  },
  publicPath: '/im/',
  lintOnSave: false,
  productionSourceMap: false,
  configureWebpack: {
    plugins: [
      new CompressionPlugin({
        algorithm: 'gzip',
        test: /\.js$|\.html$|\.css$/,
        filename: '[path].gz[query]',
        minRatio: 1,
        threshold: 10240,
        deleteOriginalAssets: false
      })
    ]
  }
}
