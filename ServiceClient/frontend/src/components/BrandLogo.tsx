import Image from "next/image";

/**
 * Maplenou logo (files in /public). `variant="auto"` shows the colour logo on light themes and the white one on dark
 * themes (switched in CSS, see `.brand-light` / `.brand-dark`); `variant="white"` is for the dark green sidebar.
 */
export function BrandLogo({ variant = "auto", height = 32, alt = "Maplenou" }: { variant?: "auto" | "white"; height?: number; alt?: string }) {
  const width = Math.round(height * (852 / 250));
  if (variant === "white") {
    return <Image src="/logo-white.svg" alt={alt} width={width} height={height} priority unoptimized className="brand-img" />;
  }
  return (
    <>
      <Image src="/logo-color.svg" alt={alt} width={width} height={height} priority unoptimized className="brand-img brand-light" />
      <Image src="/logo-white.svg" alt="" aria-hidden="true" width={width} height={height} unoptimized className="brand-img brand-dark" />
    </>
  );
}
