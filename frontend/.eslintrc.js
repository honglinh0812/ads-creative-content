module.exports = {
  root: true,
  env: {
    node: true,
  },
  extends: [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
  ],
  parserOptions: {
    ecmaVersion: 2020,
  },
  rules: {
    'vue/multi-word-component-names': ['error', {
      ignores: ['Home', 'Login', 'Dashboard', 'Ads', 'Button', 'Textarea', 'Dropdown', 'Card', 'Column', 'Dialog', 'Toast', 'Divider'],
    }],
  },
};

