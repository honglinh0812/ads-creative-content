module.exports = {
  extends: [
    'stylelint-config-tailwindcss'
  ],
  rules: {
    'at-rule-no-unknown': [
      true,
      {
        ignoreAtRules: [
          'tailwind',
          'apply',
          'variants',
          'responsive',
          'screen',
          'layer',
          'mixin',
          'include',
          'extend',
          'function',
          'return',
          'if',
          'else',
          'for',
          'each',
          'while'
        ]
      }
    ],
    'function-no-unknown': [
      true,
      {
        ignoreFunctions: [
          'theme',
          'screen',
          'darken',
          'lighten',
          'saturate',
          'desaturate',
          'adjust-hue',
          'rgba',
          'mix'
        ]
      }
    ]
  },
  overrides: [
    {
      files: ['*.vue', '**/*.vue'],
      customSyntax: 'postcss-html'
    },
    {
      files: ['*.scss', '**/*.scss'],
      customSyntax: 'postcss-scss'
    }
  ]
};