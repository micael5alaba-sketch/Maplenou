import { defineMessages } from "../define";

/** Ticket history timeline (components/History.tsx). */
export const historyMessages = defineMessages({
  fr: {
    empty: "Aucun historique",
    ariaLabel: "Historique du ticket",
    change: " : {from} → {to}",
    value: " : {to}",
  },
  en: {
    empty: "No history",
    ariaLabel: "Ticket history",
    change: ": {from} → {to}",
    value: ": {to}",
  },
});
