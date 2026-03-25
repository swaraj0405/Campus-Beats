module.exports = {
  apps: [
    {
      name: "campus-beats-frontend",
      script: "npm.cmd",
      args: "run dev",
      cwd: "./",
      watch: false,
    },
    {
      name: "campus-beats-audio",
      script: "npm.cmd",
      args: "start",
      cwd: "./audio-service",
      watch: false,
    },
    {
      name: "campus-beats-backend",
      script: "powershell.exe",
      args: "-ExecutionPolicy Bypass -NoProfile -File ./start-backend.ps1",
      cwd: "./backend",
      watch: false,
    }
  ]
};
