/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Brand colour — adjust to match Lolita Laundry's identity
        brand: {
          50:  '#f0f9ff',
          100: '#e0f2fe',
          300: '#7dd3fc',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
          // Logo navy (sampled from the Lolita wordmark) — used for the sidebar surface
          800: '#002f6c',
          900: '#001f4d',
        },
      },
    },
  },
  plugins: [],
}
