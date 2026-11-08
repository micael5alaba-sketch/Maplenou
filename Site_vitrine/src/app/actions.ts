"use server";

import { redirect } from "next/navigation";
import { siteConfig } from "@/lib/config";

const EMAIL = /^[^\s@]{1,64}@[^\s@]{1,190}\.[^\s@]{2,}$/;

/**
 * "Stay informed" form: sends the address to Bakend's public newsletter endpoint (POST /api/newsletter/subscribe),
 * then comes back to the home page with the result in the URL (works without JavaScript). Nothing is stored here.
 */
export async function subscribeToNewsletter(formData: FormData): Promise<void> {
  const email = String(formData.get("email") ?? "").trim();
  let result: "ok" | "invalid" | "error" = "invalid";
  if (email.length <= 254 && EMAIL.test(email)) {
    try {
      const res = await fetch(`${siteConfig.apiUrl}/api/newsletter/subscribe`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
        signal: AbortSignal.timeout(5000),
        cache: "no-store",
      });
      // 409 = already subscribed: same answer for the visitor (and nothing revealed about other addresses)
      result = res.ok || res.status === 409 ? "ok" : res.status === 400 ? "invalid" : "error";
    } catch {
      result = "error";
    }
  }
  redirect(`/?newsletter=${result}#newsletter`);
}
