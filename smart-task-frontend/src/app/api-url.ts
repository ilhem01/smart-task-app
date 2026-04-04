/**
 * API Gateway base URL.
 * - Build-time VITE_API_URL is used for split hosts / custom API domains (non-localhost).
 * - In the browser, if the baked-in URL points at localhost, ignore it and use the same host
 *   as the page with port 8080. Angular/esbuild inlines import.meta.env at build time, which
 *   often becomes localhost:8080 and breaks EC2/public deploys (loopback / PNA).
 */
function viteApiUrlFromBuild(): string | undefined {
  const v = import.meta.env?.VITE_API_URL?.trim();
  return v || undefined;
}

function isLoopbackApiUrl(url: string): boolean {
  try {
    const h = new URL(url).hostname;
    return h === 'localhost' || h === '127.0.0.1' || h === '[::1]';
  } catch {
    return false;
  }
}

let cached: string | null = null;

export function getApiBaseUrl(): string {
  if (cached !== null) {
    return cached;
  }
  const fromBuild = viteApiUrlFromBuild();
  if (typeof window !== 'undefined' && window.location?.hostname) {
    if (fromBuild && !isLoopbackApiUrl(fromBuild)) {
      cached = fromBuild;
      return cached;
    }
    const { protocol, hostname } = window.location;
    cached = `${protocol}//${hostname}:8080`;
    return cached;
  }
  if (fromBuild) {
    cached = fromBuild;
    return cached;
  }
  cached = 'http://localhost:8080';
  return cached;
}
