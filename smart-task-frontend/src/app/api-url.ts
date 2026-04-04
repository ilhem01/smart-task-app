/**
 * API Gateway base URL.
 * - If VITE_API_URL is set at build time, it wins (split hosts / custom DNS).
 * - Otherwise in the browser, use the same host as the page with port 8080 so EC2 (or any) deploy
 *   does not call localhost (which would target the user's PC and trigger loopback/private-network blocks).
 */
function resolveApiBaseUrl(): string {
  const fromEnv = import.meta.env?.VITE_API_URL?.trim();
  if (fromEnv) {
    return fromEnv;
  }
  if (typeof globalThis !== 'undefined' && 'location' in globalThis) {
    const loc = (globalThis as unknown as { location?: Location }).location;
    if (loc?.hostname) {
      return `${loc.protocol}//${loc.hostname}:8080`;
    }
  }
  return 'http://localhost:8080';
}

const API_URL = resolveApiBaseUrl();

export { API_URL };
