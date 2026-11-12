"use client";

import { useId, useState } from "react";
import { buyerPrice, categoryForPrice, fcfa, type FeeCategory, MIN_ITEM_PRICE } from "@/lib/commission";

interface Labels {
  calcTitle: string; calcLabel: string; calcCategory: string; catStandard: string; catHigh: string;
  calcCommission: string; calcDisplay: string; calcNet: string; calcBelowMin: string; calcHighHint: string;
}

/** Seller-side estimate: base price + category → price shown to the buyer (commission included) and amount received. */
export function CommissionCalculator({ locale, labels }: { locale: "fr" | "en"; labels: Labels }) {
  const id = useId();
  const [value, setValue] = useState("10000");
  const [category, setCategory] = useState<FeeCategory>("standard");
  const base = Math.max(0, Math.floor(Number(value.replace(/\D/g, "")) || 0));
  const { rate, commission, display, sellerReceives } = buyerPrice(base, category);
  const pct = new Intl.NumberFormat(locale === "fr" ? "fr-FR" : "en-GB", { style: "percent", maximumFractionDigits: 1 })
    .format(rate);

  return (
    <div className="panel calc">
      <h3>{labels.calcTitle}</h3>
      <label htmlFor={id}>{labels.calcLabel}</label>
      <input
        id={id}
        inputMode="numeric"
        autoComplete="off"
        value={value}
        onChange={(e) => setValue(e.target.value.replace(/[^\d\s]/g, ""))}
      />
      <fieldset className="calc-cat">
        <legend>{labels.calcCategory}</legend>
        {(["standard", "high"] as const).map((c) => (
          <label key={c}>
            <input type="radio" name={`${id}-cat`} value={c} checked={category === c} onChange={() => setCategory(c)} />
            {c === "standard" ? labels.catStandard : labels.catHigh}
          </label>
        ))}
      </fieldset>
      <div role="status">
        {base > 0 && base < MIN_ITEM_PRICE && <p className="warn">{labels.calcBelowMin}</p>}
        {category === "standard" && categoryForPrice(base) === "high" && <p className="warn">{labels.calcHighHint}</p>}
      </div>
      <div className="calc-out" aria-live="polite">
        <div><span className="muted">{labels.calcCommission.replace("{rate}", pct)}</span><span>+ {fcfa(commission, locale)}</span></div>
        <div><span className="muted">{labels.calcDisplay}</span><span>{fcfa(display, locale)}</span></div>
        <div><span>{labels.calcNet}</span><span className="calc-net">{fcfa(sellerReceives, locale)}</span></div>
      </div>
    </div>
  );
}
