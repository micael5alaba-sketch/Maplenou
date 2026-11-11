import { defineMessages } from "../define";

/** Accessible labels and tooltips of the support charts (components/Charts.tsx). */
export const charts = defineMessages({
  fr: {
    barLabel: "{label} : {value} ({pct} %)",
    barTip: "{label} — {value} ({pct} %)",
    columnLabel: "{label} : {value}",
    columnTip: "{label} — {value}",
    axisMax: "max. {max}",
  },
  en: {
    barLabel: "{label}: {value} ({pct}%)",
    barTip: "{label} — {value} ({pct}%)",
    columnLabel: "{label}: {value}",
    columnTip: "{label} — {value}",
    axisMax: "max {max}",
  },
});
