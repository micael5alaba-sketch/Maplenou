import { defineMessages } from "../define";

/** Help-centre search on the requester home page (components/HelpSearch.tsx). */
export const helpSearch = defineMessages({
  fr: {
    title: "Trouver une réponse",
    label: "Rechercher dans le centre d'aide",
    placeholder: "Ex. : rouvrir un ticket, pièce jointe…",
    search: "Rechercher",
    noResult: "Aucun article ne correspond. Essayez d'autres mots ou créez un ticket.",
    unavailable: "La recherche n'est pas disponible pour le moment. Consultez les articles ci-dessous.",
    newTab: "nouvel onglet",
    popular: "Articles utiles",
    articleCreate: "Créer un ticket",
    articleFollow: "Suivre mes tickets",
    articleReopen: "Réouverture et fermeture",
    articleNotifications: "Notifications",
    articleFaq: "Questions fréquentes",
  },
  en: {
    title: "Find an answer",
    label: "Search the help centre",
    placeholder: "E.g. reopen a ticket, attachment…",
    search: "Search",
    noResult: "No article matches. Try other words or create a ticket.",
    unavailable: "Search is not available right now. See the articles below.",
    newTab: "new tab",
    popular: "Useful articles",
    articleCreate: "Create a ticket",
    articleFollow: "Follow my tickets",
    articleReopen: "Reopening and closing",
    articleNotifications: "Notifications",
    articleFaq: "Frequently asked questions",
  },
});
