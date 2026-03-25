module.exports = {
  apps: [
    {
      name: "campus-beats-frontend",
      script: "./run-frontend.js",
      watch: false
    },
    {
      name: "campus-beats-audio",
      script: "./run-audio.js",
      watch: false
    },
    {
      name: "campus-beats-backend",
      script: "./run-backend.js",
      watch: false
    }
  ]
};
