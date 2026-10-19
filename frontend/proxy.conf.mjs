export default {
  '/api': {
    target: 'http://127.0.0.1:18080',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, ''),
  },
};
