import { exec } from 'child_process';
const ps = exec('powershell -ExecutionPolicy Bypass -NoProfile -Command "cd audio-service; npm start"');
ps.stdout.on('data', data => console.log(data));
ps.stderr.on('data', data => console.error(data));
