import "server-only";

/**
 * Links and the Maplenou API, read from the environment at run time (Site_vitrine/.env.example), so one Docker
 * image serves every environment. Empty optional links hide their buttons.
 */
export const siteConfig = {
  /** Bakend, reached by the server only (public catalogue endpoints). */
  apiUrl: (process.env.MAPLENOU_API_URL ?? "http://localhost:8080").replace(/\/$/, ""),
  /** Where "Open the app" leads (Maplenou web app, or a store page). */
  appUrl: process.env.APP_URL ?? "",
  androidUrl: process.env.ANDROID_APP_URL ?? "",
  iosUrl: process.env.IOS_APP_URL ?? "",
  /** Customer-service web app (ServiceClient/frontend). */
  serviceClientUrl: process.env.SERVICE_CLIENT_URL ?? "http://localhost:3001",
  /** Public help centre (ServiceClient documentation, user guide). */
  helpCenterUrl: process.env.HELP_CENTER_URL ?? "http://localhost:8006/user/fr/",
  /** Social networks of Maplenou; empty ones are not shown. */
  social: {
    facebook: process.env.SOCIAL_FACEBOOK_URL ?? "",
    instagram: process.env.SOCIAL_INSTAGRAM_URL ?? "",
    tiktok: process.env.SOCIAL_TIKTOK_URL ?? "",
    whatsapp: process.env.SOCIAL_WHATSAPP_URL ?? "",
    x: process.env.SOCIAL_X_URL ?? "",
    youtube: process.env.SOCIAL_YOUTUBE_URL ?? "",
  },
  /** Absolute URL of this site, for links in search engines and social previews. */
  publicUrl: (process.env.SITE_PUBLIC_URL ?? "http://localhost:3002").replace(/\/$/, ""),
};

/** Help centre in the visitor's language: …/user/fr/ ↔ …/user/en/. */
export function helpCenterFor(locale: "fr" | "en"): string {
  return siteConfig.helpCenterUrl.replace(/\/(fr|en)\/?$/, `/${locale}/`);
}
