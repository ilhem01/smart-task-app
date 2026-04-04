/**
 * API Gateway base URL.
 * - Build-time VITE_API_URL is used for split hosts / custom API domains (non-localhost).
 * - In the browser, loopback URLs from the build are ignored; use same host as the page :8080.
 * - No module-level cache: caching once could pin localhost if the first call hit a bad branch.
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

function hasBrowserLocation(): boolean {
  return (
    typeof window !== 'undefined' &&
    !!window.location &&
    typeof window.location.hostname === 'string' &&
    window.location.hostname.length > 0
  );
}

export function getApiBaseUrl(): string {
  const fromBuild = viteApiUrlFromBuild();
  if (hasBrowserLocation()) {
    if (fromBuild && !isLoopbackApiUrl(fromBuild)) {
      return fromBuild;
    }
    const { protocol, hostname } = window.location;
    return `${protocol}//${hostname}:8080`;
  }
  if (fromBuild && !isLoopbackApiUrl(fromBuild)) {
    return fromBuild;
  }
  return 'http://localhost:8080';
}
