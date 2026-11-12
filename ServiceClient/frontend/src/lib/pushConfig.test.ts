import { describe, expect, it } from "vitest";
import { readWebPushConfig } from "./pushConfig";

const full = {
  FIREBASE_WEB_API_KEY: "AIza-test",
  FIREBASE_WEB_PROJECT_ID: "maplenou-support",
  FIREBASE_WEB_MESSAGING_SENDER_ID: "1234567890",
  FIREBASE_WEB_APP_ID: "1:1234567890:web:abc",
  FIREBASE_WEB_VAPID_KEY: "BKey",
};

describe("readWebPushConfig", () => {
  it("is configured when every required value is set", () => {
    const r = readWebPushConfig({ ...full, FIREBASE_WEB_AUTH_DOMAIN: " maplenou-support.firebaseapp.com " });
    expect(r.configured).toBe(true);
    if (r.configured) {
      expect(r.config.projectId).toBe("maplenou-support");
      expect(r.config.authDomain).toBe("maplenou-support.firebaseapp.com");
    }
  });

  it("is not configured when a required value is missing or blank", () => {
    expect(readWebPushConfig({}).configured).toBe(false);
    expect(readWebPushConfig({ ...full, FIREBASE_WEB_VAPID_KEY: "  " }).configured).toBe(false);
  });

  it("never exposes anything when not configured", () => {
    expect(readWebPushConfig({ FIREBASE_WEB_API_KEY: "AIza-test" })).toEqual({ configured: false });
  });
});
