// Asset preloader shared by index.html (wasm) and debug.html (js).
//
// Image decoding and file fetches are async in a browser, but the engine's
// asset lookups are synchronous calls that expect data to already be there.
// Everything in assets.json is therefore loaded up front and parked on
// window.__assets, which Java reads through com.nuclearunicorn.web.Assets.
window.__assets = { images: {}, text: {} };

// Engine paths are classpath-style ("/resources/ui/x.png") while the files are
// served from ./resources/, so each asset is registered under both spellings.
function registerImage(path, img) {
  window.__assets.images['/resources/' + path] = img;
  window.__assets.images[path] = img;
}

function registerText(path, body) {
  window.__assets.text['/resources/' + path] = body;
  window.__assets.text[path] = body;
}

function loadImage(path) {
  return new Promise(resolve => {
    const img = new Image();
    img.onload = () => { registerImage(path, img); resolve(); };
    img.onerror = () => { console.warn('asset failed:', path); resolve(); };
    img.src = './resources/' + path;
  });
}

function loadText(path) {
  return fetch('./resources/' + path)
    .then(r => (r.ok ? r.text() : null))
    .then(body => { if (body !== null) registerText(path, body); })
    .catch(() => console.warn('asset failed:', path));
}

export async function preloadAssets() {
  const manifest = await fetch('./assets.json')
    .then(r => r.json())
    .catch(() => ({ images: [], text: [] }));

  const images = (manifest.images || []).filter(Boolean);
  const text = (manifest.text || []).filter(Boolean);

  await Promise.all([...images.map(loadImage), ...text.map(loadText)]);
  return { images: images.length, text: text.length };
}
