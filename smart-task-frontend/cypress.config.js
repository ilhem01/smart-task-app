const { defineConfig } = require('cypress');

module.exports = defineConfig({
  reporter: 'mocha-junit-reporter',
  reporterOptions: {
    mochaFile: 'cypress/results/results-[hash].xml',
    toConsole: true
  },
  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: false
  },
  video: false
});
