/* 游迹中国 · 离线缓存（Service Worker）
 * 页面：联网优先。有网时拿最新版本，3.5 秒没响应就先用缓存；没网时直接用缓存。
 * 图标、清单等静态文件：缓存优先，后台静默更新。
 * 版本号由 tools/build.sh 打包时写入，版本变化后旧缓存会被清理。
 */
const VERSION = '7031ea98d093';
const CACHE = 'youji-' + VERSION;
const CORE = ['./', './index.html', './manifest.webmanifest', './icons/icon-192.png', './icons/icon-512.png', './icons/maskable-512.png', './icons/apple-touch-icon.png', './icons/favicon-32.png'];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(CORE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', event => {
  event.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k.startsWith('youji-') && k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', event => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;
  event.respondWith(req.mode === 'navigate' ? page(req) : asset(req));
});

async function page(req) {
  const cache = await caches.open(CACHE);
  const net = fetch(req).then(res => { if (res.ok) cache.put('./index.html', res.clone()); return res; });
  net.catch(() => {});
  try {
    return await Promise.race([net, new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 3500))]);
  } catch (e) {
    const hit = await cache.match('./index.html');
    return hit || net;
  }
}

async function asset(req) {
  const cache = await caches.open(CACHE);
  const hit = await cache.match(req);
  const net = fetch(req).then(res => { if (res.ok) cache.put(req, res.clone()); return res; });
  if (hit) { net.catch(() => {}); return hit; }
  return net;
}
