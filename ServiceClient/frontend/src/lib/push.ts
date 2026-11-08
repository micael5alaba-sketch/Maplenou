"use client";

import { api } from "./api";
import type { WebPushConfig, WebPushConfigResponse } from "./pushConfig";

/**
 * Browser push with Firebase Cloud Messaging (docs/guide/firebase.md).
 * The Firebase SDK is loaded only when the user enables or disables push (dynamic import), so pages that
 * never use it do not download it. Firebase keeps its own registration data in the browser's IndexedDB once
 * push is enabled; the app itself stores nothing.
 */
export const PUSH_WORKER_URL = "/push-sw.js";

export type PushState = "loading" | "unsupported" | "not-configured" | "blocked" | "off" | "on";

export class PushSetupError extends Error {
  constructor(readonly reason: "blocked" | "unsupported" | "not-configured") {
    super(reason);
  }
}

export function pushSupported(): boolean {
  return (
    typeof window !== "undefined" &&
    window.isSecureContext &&
    "serviceWorker" in navigator &&
    "PushManager" in window &&
    "Notification" in window
  );
}

export async function loadPushConfig(): Promise<WebPushConfig | null> {
  try {
    const res = await fetch("/api/push-config", { cache: "no-store" });
    const body = (await res.json()) as WebPushConfigResponse;
    return body.configured ? body.config : null;
  } catch {
    return null;
  }
}

async function workerRegistration(): Promise<ServiceWorkerRegistration | undefined> {
  return navigator.serviceWorker.getRegistration(PUSH_WORKER_URL);
}

/** Current state without prompting the user and without loading Firebase. */
export async function detectPushState(config: WebPushConfig | null): Promise<PushState> {
  if (!pushSupported()) return "unsupported";
  if (!config) return "not-configured";
  if (Notification.permission === "denied") return "blocked";
  if (Notification.permission !== "granted") return "off";
  const reg = await workerRegistration();
  const sub = await reg?.pushManager.getSubscription();
  return sub ? "on" : "off";
}

async function messaging(config: WebPushConfig) {
  const [{ initializeApp, getApps }, fcm] = await Promise.all([import("firebase/app"), import("firebase/messaging")]);
  if (!(await fcm.isSupported())) throw new PushSetupError("unsupported");
  const app = getApps()[0] ?? initializeApp(config);
  return { fcm, instance: fcm.getMessaging(app) };
}

async function currentToken(config: WebPushConfig): Promise<{ token: string; deleteToken: () => Promise<boolean> }> {
  const registration = await navigator.serviceWorker.register(PUSH_WORKER_URL, { scope: "/" });
  await navigator.serviceWorker.ready;
  const { fcm, instance } = await messaging(config);
  const token = await fcm.getToken(instance, { vapidKey: config.vapidKey, serviceWorkerRegistration: registration });
  return { token, deleteToken: () => fcm.deleteToken(instance) };
}

/** Asks for permission, subscribes this browser and registers its token for the signed-in user. */
export async function enablePush(config: WebPushConfig): Promise<void> {
  if (!pushSupported()) throw new PushSetupError("unsupported");
  const permission = await Notification.requestPermission();
  if (permission !== "granted") throw new PushSetupError("blocked");
  const { token } = await currentToken(config);
  await api.post("/notifications/devices", { token, platform: "WEB" });
}

/** Removes this browser's token from the user's devices and from Firebase. */
export async function disablePush(config: WebPushConfig): Promise<void> {
  const { token, deleteToken } = await currentToken(config);
  await api.del(`/notifications/devices/${encodeURIComponent(token)}`);
  await deleteToken();
}

export interface PushTestResult {
  mode: "firebase" | "log-only";
  devices: number;
  invalidRemoved: number;
}

export const sendTestPush = () => api.post<PushTestResult>("/notifications/devices/test");

/**
 * On logout: stop this browser from receiving the previous user's pushes. Firebase then reports the token as
 * unregistered at the next send and the backend deletes it. Never throws.
 */
export async function unsubscribeThisBrowser(): Promise<void> {
  try {
    if (!pushSupported()) return;
    const sub = await (await workerRegistration())?.pushManager.getSubscription();
    await sub?.unsubscribe();
  } catch {
    // best effort
  }
}
