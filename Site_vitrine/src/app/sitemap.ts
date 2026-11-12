import type { MetadataRoute } from "next";
import { siteConfig } from "@/lib/config";

export const dynamic = "force-dynamic";

export default function sitemap(): MetadataRoute.Sitemap {
  return ["", "/vendre", "/livrer", "/service-client", "/boutiques"].map((path) => ({
    url: `${siteConfig.publicUrl}${path}`,
    changeFrequency: "weekly",
    priority: path === "" ? 1 : 0.7,
  }));
}
