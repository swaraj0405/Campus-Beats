import { exec } from 'child_process';
const ps = exec('powershell -ExecutionPolicy Bypass -NoProfile -File start-kl-smartq-frontend.ps1');
ps.stdout.on('data', data => console.log(data));
ps.stderr.on('data', data => console.error(data));
