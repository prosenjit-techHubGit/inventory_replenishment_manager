import type { ApiError } from "../models/types";

export class HttpError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 8000);
  let response: Response;
  try {
    response = await fetch(path, { ...init, headers, signal: controller.signal });
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      throw new HttpError(0, "The API did not respond. Is the backend running?");
    }
    throw new HttpError(0, "Unable to reach the API. Is the backend running?");
  } finally {
    window.clearTimeout(timeout);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  let payload: T | ApiError | undefined;
  if (text) {
    try {
      payload = JSON.parse(text) as T | ApiError;
    } catch {
      payload = undefined;
    }
  }
  if (!response.ok) {
    const error = payload as ApiError | undefined;
    throw new HttpError(
      response.status,
      error?.message ??
        (response.status === 502
          ? "Unable to reach the API. Is the backend running?"
          : `Request failed (${response.status})`),
      error?.code,
    );
  }
  return payload as T;
}
