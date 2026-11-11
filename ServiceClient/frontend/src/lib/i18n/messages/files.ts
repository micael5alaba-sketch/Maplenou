import { defineMessages } from "../define";

/** File picker: approved formats and limits (DEC-003) and client-side validation messages. */
export const files = defineMessages({
  fr: {
    addShort: "+ Pièce jointe",
    add: "+ Ajouter une pièce jointe",
    hint: "JPG, PNG, WEBP ou PDF — 10 Mo max. par fichier, 5 fichiers max.",
    selected: "Pièces jointes sélectionnées",
    remove: "Retirer {name}",
    tooMany: "Maximum {max} pièces jointes par message.",
    badFormat: "Format non autorisé pour « {name} ». Formats acceptés : JPG, PNG, WEBP, PDF.",
    empty: "Le fichier « {name} » est vide.",
    tooLarge: "Le fichier « {name} » dépasse 10 Mo.",
    ticketTooLarge: "La taille totale des pièces jointes du ticket dépasserait 50 Mo.",
  },
  en: {
    addShort: "+ Attachment",
    add: "+ Add an attachment",
    hint: "JPG, PNG, WEBP or PDF — 10 MB max. per file, 5 files max.",
    selected: "Selected attachments",
    remove: "Remove {name}",
    tooMany: "Maximum {max} attachments per message.",
    badFormat: "File type not allowed for “{name}”. Accepted formats: JPG, PNG, WEBP, PDF.",
    empty: "The file “{name}” is empty.",
    tooLarge: "The file “{name}” exceeds 10 MB.",
    ticketTooLarge: "The total size of the ticket's attachments would exceed 50 MB.",
  },
});
