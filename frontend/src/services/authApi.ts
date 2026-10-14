type CsrfToken = {
  headerName: string;
  token: string;
};

async function csrfHeaders(contentType?: string): Promise<HeadersInit> {
  const response = await fetch("/api/v1/auth/csrf", { credentials: "same-origin" });
  if (!response.ok) throw new Error("Authentication request failed");

  const csrf = (await response.json()) as CsrfToken;
  return {
    ...(contentType ? { "Content-Type": contentType } : {}),
    [csrf.headerName]: csrf.token,
  };
}

export async function postAuth<T = void>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: "POST",
    credentials: "same-origin",
    headers: await csrfHeaders("application/json"),
    body: JSON.stringify(body),
  });

  if (!response.ok) throw new Error("Authentication request failed");
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export async function deleteCurrentSession(): Promise<void> {
  const response = await fetch("/api/v1/auth/sessions/current", {
    method: "DELETE",
    credentials: "same-origin",
    headers: await csrfHeaders(),
  });

  if (!response.ok) throw new Error("Could not end the session");
}
