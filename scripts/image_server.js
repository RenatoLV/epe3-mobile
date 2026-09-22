/**
 * Servidor HTTP local para pruebas de rendimiento de red (Paso 3).
 *
 * Sirve las 8 imágenes originales de alta resolución (~2.4 MB c/u) y las 8 imágenes WebP (~100 KB c/u).
 * Informa cabeceras precisas Content-Type y Content-Length para que Android Studio
 * Network Inspector capture el volumen de datos exacto transferido.
 *
 * Uso:
 *   node scripts/image_server.js
 *   node scripts/image_server.js --port 8080
 *
 * Puertos y hosts:
 *   - Emulador Android (AVD): http://10.0.2.2:8085 (o 8080)
 *   - Teléfono físico:        http://<TU_IP_LAN>:8085
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');

const requestedPort = parseInt(process.env.PORT || process.argv[2] || '8085', 10);
const PUBLIC_DIR = path.join(__dirname, 'public');

function getLocalIps() {
  const interfaces = os.networkInterfaces();
  const ips = [];
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        ips.push({ interface: name, address: iface.address });
      }
    }
  }
  return ips;
}

function createServer(port) {
  const server = http.createServer((req, res) => {
    const url = new URL(req.url, `http://${req.headers.host}`);
    const pathname = decodeURIComponent(url.pathname);

    // Endpoint de salud
    if (pathname === '/health' || pathname === '/') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        status: 'running',
        service: 'EPE3 Mock Image Server',
        port: port,
        endpoints: {
          original: '/images/original/medico_[1-8].jpg',
          optimized: '/images/optimized/medico_[1-8].webp'
        }
      }, null, 2));
      return;
    }

    // Rutas de imágenes
    if (pathname.startsWith('/images/')) {
      const filePath = path.join(PUBLIC_DIR, pathname);

      if (!filePath.startsWith(PUBLIC_DIR)) {
        res.writeHead(403, { 'Content-Type': 'text/plain' });
        res.end('Acceso denegado');
        return;
      }

      if (!fs.existsSync(filePath)) {
        console.warn(`[404 NOT FOUND] ${pathname}`);
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end(`Archivo no encontrado: ${pathname}`);
        return;
      }

      const stat = fs.statSync(filePath);
      const ext = path.extname(filePath).toLowerCase();

      let contentType = 'application/octet-stream';
      let cacheHeader = 'no-cache';

      if (ext === '.webp') {
        contentType = 'image/webp';
        cacheHeader = 'public, max-age=86400';
      } else if (ext === '.jpg' || ext === '.jpeg') {
        contentType = 'image/jpeg';
        cacheHeader = 'no-cache, no-store, must-revalidate';
      } else if (ext === '.png') {
        contentType = 'image/png';
      }

      res.writeHead(200, {
        'Content-Type': contentType,
        'Content-Length': stat.size,
        'Cache-Control': cacheHeader,
        'Access-Control-Allow-Origin': '*'
      });

      const isOriginal = pathname.includes('/original/');
      const tag = isOriginal ? 'ORIGINAL ' : 'OPTIMIZED';
      const sizeDisplay = stat.size > 1024 * 1024
        ? `${(stat.size / (1024 * 1024)).toFixed(2)} MB`
        : `${(stat.size / 1024).toFixed(1)} KB`;

      console.log(`[${tag}] 200 GET ${pathname} -> ${sizeDisplay} (${stat.size} bytes)`);

      const readStream = fs.createReadStream(filePath);
      readStream.pipe(res);
      return;
    }

    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Ruta no encontrada');
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.warn(`\n[AVISO] El puerto ${port} está ocupado por otro servicio del sistema.`);
      if (port === 8080) {
        console.log('Intentando automáticamente en el puerto 8085...');
        createServer(8085);
      } else {
        console.error(`Error: No se pudo enlazar al puerto ${port}.`);
        process.exit(1);
      }
    } else {
      console.error('Error del servidor:', err);
    }
  });

  server.listen(port, '0.0.0.0', () => {
    console.log('================================================================');
    console.log(` Servidor de imágenes iniciado en el puerto ${port}`);
    console.log('================================================================');
    console.log(` • Host local (PC):         http://localhost:${port}`);
    console.log(` • Emulador Android (AVD):  http://10.0.2.2:${port}`);

    const localIps = getLocalIps();
    if (localIps.length > 0) {
      console.log(' • Teléfono físico (Wi-Fi):');
      localIps.forEach(ip => {
        console.log(`     -> http://${ip.address}:${port} (${ip.interface})`);
      });
    } else {
      console.log(' • Teléfono físico: Conéctalo a la misma red Wi-Fi y consulta la IP con ipconfig');
    }
    console.log('----------------------------------------------------------------');
    console.log(' Endpoints disponibles:');
    console.log(`   GET http://10.0.2.2:${port}/images/original/medico_1.jpg  (~2.4 MB)`);
    console.log(`   GET http://10.0.2.2:${port}/images/optimized/medico_1.webp (~100 KB)`);
    console.log(`   GET http://10.0.2.2:${port}/health`);
    console.log(' Presiona Ctrl+C para detener el servidor.\n');
  });
}

createServer(requestedPort);
