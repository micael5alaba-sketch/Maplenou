/**
 * Firebase web configuration for browser push (docs/guide/firebase.md). These values identify the Firebase
 * project and are public by design (they end up in every browser); the secret service-account key stays on
 * the backend. They are read at run time from the environment, so one Docker image works for every project.
 */
export interface WebPushConfig {
  apiKey: string;
  authDomain?: string;
  projectId: string;
  messagingSenderId: string;
  appId: string;
  vapidKey: string;
}

export type WebPushConfigResponse = { configured: true; config: WebPushConfig } | { configured: false };

type Env = Record<string, string | undefined>;

export function readWebPushConfig(env: Env): WebPushConfigResponse {
  const v = (name: string) => (env[name] ?? "").trim();
  const config = {
    apiKey: v("FIREBASE_WEB_API_KEY"),
    authDomain: v("FIREBASE_WEB_AUTH_DOMAIN") || undefined,
    projectId: v("FIREBASE_WEB_PROJECT_ID"),
    messagingSenderId: v("FIREBASE_WEB_MESSAGING_SENDER_ID"),
    appId: v("FIREBASE_WEB_APP_ID"),
    vapidKey: v("FIREBASE_WEB_VAPID_KEY"),
  };
  const required = [config.apiKey, config.projectId, config.messagingSenderId, config.appId, config.vapidKey];
  return required.every(Boolean) ? { configured: true, config } : { configured: false };
}
