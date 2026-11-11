"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { useErrorText, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { push } from "@/lib/i18n/messages/push";
import {
  detectPushState,
  disablePush,
  enablePush,
  loadPushConfig,
  PushSetupError,
  sendTestPush,
  type PushState,
} from "@/lib/push";
import type { WebPushConfig } from "@/lib/pushConfig";
import { useToast } from "./ui";

/** Turns browser push (Firebase Cloud Messaging) on or off for this device and sends a test notification. */
export function PushNotifications() {
  const t = useMessages(push);
  const toast = useToast();
  const errorText = useErrorText();
  const [config, setConfig] = useState<WebPushConfig | null>(null);
  const [state, setState] = useState<PushState>("loading");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let alive = true;
    (async () => {
      const c = await loadPushConfig();
      const s = await detectPushState(c);
      if (alive) {
        setConfig(c);
        setState(s);
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  const run = useCallback(
    async (action: () => Promise<void>) => {
      setBusy(true);
      try {
        await action();
      } catch (e) {
        if (e instanceof PushSetupError) {
          setState(e.reason === "not-configured" ? "not-configured" : e.reason);
        } else if (e instanceof ApiError) {
          toast(errorText(e), true);
        } else {
          // browser or Firebase error (private window, push service unreachable…): never show its raw text
          console.warn("push setup failed", e);
          toast(t.failed, true);
        }
      } finally {
        setBusy(false);
      }
    },
    [errorText, toast, t.failed],
  );

  const message = {
    loading: t.loading,
    unsupported: t.unsupported,
    "not-configured": t.notConfigured,
    blocked: t.blocked,
    off: t.off,
    on: t.on,
  }[state];

  return (
    <section className="card stack-sm" aria-labelledby="push-title" aria-busy={busy || state === "loading"}>
      <h2 id="push-title">{t.title}</h2>
      <p className="small secondary" role="status">{message}</p>
      {config && state === "off" && (
        <div>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            disabled={busy}
            onClick={() =>
              run(async () => {
                await enablePush(config);
                setState("on");
                toast(t.enabled);
              })
            }
          >
            {t.enable}
          </button>
        </div>
      )}
      {config && state === "on" && (
        <div className="row">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            disabled={busy}
            onClick={() =>
              run(async () => {
                const r = await sendTestPush();
                toast(r.mode === "log-only" ? t.testLogOnly : format(t.testSent, { count: r.devices }));
              })
            }
          >
            {t.test}
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            disabled={busy}
            onClick={() =>
              run(async () => {
                await disablePush(config);
                setState("off");
                toast(t.disabled);
              })
            }
          >
            {t.disable}
          </button>
        </div>
      )}
    </section>
  );
}
