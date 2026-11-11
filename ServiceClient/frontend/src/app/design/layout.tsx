import type { Metadata } from "next";
import { getMessages } from "@/lib/i18n/server";
import { design } from "@/lib/i18n/messages/design";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getMessages(design);
  return { title: t.title, robots: { index: false, follow: false } };
}

export default function DesignLayout({ children }: { children: React.ReactNode }) {
  return children;
}
