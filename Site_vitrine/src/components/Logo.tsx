/** Maplenou logo: the colour version on light backgrounds, the white one in the dark theme (switched in CSS). */
export function Logo() {
  return (
    <>
      {/* eslint-disable-next-line @next/next/no-img-element -- small SVG, no optimisation needed */}
      <img className="on-light" src="/logo_horizontal_color.svg" alt="Maplenou" width={150} height={34} />
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img className="on-dark" src="/logo_horizontal_white.svg" alt="" width={150} height={34} aria-hidden="true" />
    </>
  );
}
