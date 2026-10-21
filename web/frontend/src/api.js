export class ApiError extends Error {
  constructor(message, status = 0, data = null) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

export async function api(path, body, accessToken) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10000);
  try {
    const response = await fetch(`/api${path}`, {
      method: body === undefined ? 'GET' : 'POST',
      headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
      cache: 'no-store',
    });
    const data = await response.json();
    if (!response.ok) throw new ApiError(data.message || 'Nie udało się wykonać operacji.', response.status, data);
    return data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError('Nie można połączyć się z serwerem gry. Sprawdź połączenie i spróbuj ponownie.');
  } finally {
    clearTimeout(timeout);
  }
}
