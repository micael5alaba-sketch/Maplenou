/* eslint-disable @next/next/no-img-element -- static WebP files already sized (public/images/visuels, 2 widths each) */

/**
 * Maplenou marketing visuals (originals: Site_vitrine/design/originals, not in Git). Each one exists in two widths;
 * the browser picks one with srcset/sizes. The visuals carry printed French text, so their `alt` (messages.ts,
 * `visuals`) repeats that text for screen readers.
 */
const VISUALS = {
  "decouvrez-maplenou": [1600, 900],
  "commerce-local": [1600, 900],
  "experience-repensee": [1600, 900],
  "application-maplenou": [1600, 900],
  "suivez-nous": [1600, 900],
  "ecrans-application": [1600, 900],
  "vos-produits-visibilite": [1080, 1080],
  "plus-de-clients": [1080, 1080],
} as const;

export type VisualName = keyof typeof VISUALS;

export function Visual({ name, alt, sizes, priority = false, className }: {
  name: VisualName; alt: string; sizes: string; priority?: boolean; className?: string;
}) {
  const [w, h] = VISUALS[name];
  const half = w / 2;
  const src = (width: number) => `/images/visuels/${name}-${width}.webp`;
  return (
    <img
      className={className}
      src={src(w)}
      srcSet={`${src(half)} ${half}w, ${src(w)} ${w}w`}
      sizes={sizes}
      width={w}
      height={h}
      alt={alt}
      decoding="async"
      loading={priority ? undefined : "lazy"}
      fetchPriority={priority ? "high" : undefined}
    />
  );
}
