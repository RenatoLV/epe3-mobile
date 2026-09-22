/**
 * Generador reproducible de imágenes ficticias para las pruebas de rendimiento (Paso 3).
 *
 * Crea:
 * 1. Ocho imágenes originales de alta resolución (~2.4 MB c/u) en public/images/original/
 * 2. Ocho imágenes optimizadas en formato WebP (~80-160 KB c/u) en public/images/optimized/
 *
 * No requiere dependencias externas (usa módulos nativos de Node.js).
 */
const fs = require('fs');
const path = require('path');

const PUBLIC_DIR = path.join(__dirname, 'public');
const ORIGINAL_DIR = path.join(PUBLIC_DIR, 'images', 'original');
const OPTIMIZED_DIR = path.join(PUBLIC_DIR, 'images', 'optimized');

// Asegurar directorios
fs.mkdirSync(ORIGINAL_DIR, { recursive: true });
fs.mkdirSync(OPTIMIZED_DIR, { recursive: true });

console.log('Generando imágenes de prueba para Historial Clínico...');

/**
 * Rutas de las imágenes WebP de muestra instaladas con Android Studio
 */
const SCENIC_DIR = 'C:\\Program Files\\Android\\Android Studio\\plugins\\android\\resources\\sampleData\\backgrounds\\scenic';

const webpSources = [
  'Countryside.webp',          // ~197 KB
  'Despair.webp',              // ~112 KB
  'Eagle_Fall_Sunrise.webp',    // ~90 KB
  'Colors_of_Autumn.webp',      // ~81 KB
  'Longue_Vue.webp',            // ~77 KB
  'Lost_in_a_Field.webp',       // ~72 KB
  'One_Wheel.webp',             // ~86 KB
  'Stream.webp'                // ~103 KB
];

/**
 * 1. Generar 8 imágenes WebP optimizadas
 */
for (let i = 1; i <= 8; i++) {
  const targetPath = path.join(OPTIMIZED_DIR, `medico_${i}.webp`);
  const srcName = webpSources[i - 1];
  const srcPath = path.join(SCENIC_DIR, srcName);

  if (fs.existsSync(srcPath)) {
    fs.copyFileSync(srcPath, targetPath);
    const stat = fs.statSync(targetPath);
    console.log(`[OPTIMIZED] medico_${i}.webp: ${(stat.size / 1024).toFixed(1)} KB (fuente: ${srcName})`);
  } else {
    // Fallback: crear archivo WebP sintético con cabeceras RIFF/WEBP válidas
    const webpHeader = Buffer.from([
      0x52, 0x49, 0x46, 0x46, // "RIFF"
      0x00, 0x00, 0x00, 0x00, // tamaño (actualizado abajo)
      0x57, 0x45, 0x42, 0x50, // "WEBP"
      0x56, 0x50, 0x38, 0x20  // "VP8 "
    ]);
    const payloadSize = 120 * 1024; // ~120 KB
    const dummyPayload = Buffer.alloc(payloadSize, 0xAA);
    const fullWebp = Buffer.concat([webpHeader, dummyPayload]);
    fullWebp.writeUInt32LE(fullWebp.length - 8, 4);
    fs.writeFileSync(targetPath, fullWebp);
    console.log(`[OPTIMIZED] medico_${i}.webp: ${(fullWebp.length / 1024).toFixed(1)} KB (sintético)`);
  }
}

/**
 * 2. Generar 8 imágenes originales de alta resolución (~2.4 MB c/u)
 * Crea archivos con cabecera JPEG válida y relleno de datos para simular fotos de cámara de 12+ MP
 */
const targetSizeBytes = [
  2411724, // 2.30 MB
  2516582, // 2.40 MB
  2306867, // 2.20 MB
  2621440, // 2.50 MB
  2359296, // 2.25 MB
  2464153, // 2.35 MB
  2569011, // 2.45 MB
  2411724  // 2.30 MB
];

for (let i = 1; i <= 8; i++) {
  const targetPath = path.join(ORIGINAL_DIR, `medico_${i}.jpg`);
  const targetSize = targetSizeBytes[i - 1];

  // Cabecera JPEG mínima válida (SOI, APP0 JFIF, DQT, SOF0, SOS)
  const jpegHeader = Buffer.from([
    0xFF, 0xD8,                         // SOI
    0xFF, 0xE0, 0x00, 0x10,             // APP0 length 16
    0x4A, 0x46, 0x49, 0x46, 0x00,       // "JFIF\0"
    0x01, 0x01,                         // version 1.1
    0x01,                               // units: dpi
    0x00, 0x48, 0x00, 0x48,             // 72 x 72 dpi
    0x00, 0x00,                         // no thumbnail
    0xFF, 0xDB, 0x00, 0x43, 0x00        // DQT table (67 bytes)
  ]);
  
  // DQT table dummy 64 bytes
  const dqt = Buffer.alloc(64, 0x10);
  
  // SOF0 (Start of Frame: 2400x2400 pixels, 3 components RGB)
  const sof0 = Buffer.from([
    0xFF, 0xC0, 0x00, 0x11,             // length 17
    0x08,                               // 8 bits per sample
    0x09, 0x60,                         // height 2400
    0x09, 0x60,                         // width 2400
    0x03,                               // 3 color components
    0x01, 0x22, 0x00,                   // Component 1 (Y)
    0x02, 0x11, 0x01,                   // Component 2 (Cb)
    0x03, 0x11, 0x01                    // Component 3 (Cr)
  ]);
  
  // SOS (Start of Scan)
  const sos = Buffer.from([
    0xFF, 0xDA, 0x00, 0x0C,             // length 12
    0x03,                               // 3 components
    0x01, 0x00,                         // component 1 selector
    0x02, 0x11,                         // component 2 selector
    0x03, 0x11,                         // component 3 selector
    0x00, 0x3F, 0x00                    // spectral selection
  ]);

  const prefix = Buffer.concat([jpegHeader, dqt, sof0, sos]);
  const footer = Buffer.from([0xFF, 0xD9]); // EOI (End of Image)
  
  const payloadLen = Math.max(0, targetSize - prefix.length - footer.length);
  // Rellenar evitando secuencias 0xFF sin escape
  const payload = Buffer.alloc(payloadLen, 0x55);
  
  const fullJpeg = Buffer.concat([prefix, payload, footer]);
  fs.writeFileSync(targetPath, fullJpeg);
  
  console.log(`[ORIGINAL]  medico_${i}.jpg: ${(fullJpeg.length / (1024 * 1024)).toFixed(2)} MB (${fullJpeg.length} bytes)`);
}

console.log('\nGeneración completada.');
console.log(`Directorio original:  ${ORIGINAL_DIR}`);
console.log(`Directorio optimizado: ${OPTIMIZED_DIR}`);
