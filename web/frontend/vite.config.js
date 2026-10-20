import { defineConfig } from 'vite';

export default defineConfig({
  build: { outDir: '../src/main/resources/public', emptyOutDir: true },
  server: { proxy: { '/api': 'http://127.0.0.1:8080' } },
});
