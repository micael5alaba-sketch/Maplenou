import { defineMessages } from "../define";

/** Requester and support layouts (navigation). */
export const shell = defineMessages({
  fr: {
    navHelp: "Aide",
    navMyTickets: "Mes tickets",
    navNotifications: "Notifications",
    navAccount: "Compte",
    mainNav: "Navigation principale",
    supportNav: "Navigation support",
    sectionSupport: "Support",
    sectionAdmin: "Administration",
    dashboard: "Tableau de bord",
    queue: "File d'attente",
    reports: "Rapports",
    agents: "Agents",
    categories: "Catégories",
    slaCalendar: "Calendrier SLA",
    workflow: "Statuts et priorités",
    templates: "Réponses et messages",
    permissions: "Droits",
  },
  en: {
    navHelp: "Help",
    navMyTickets: "My tickets",
    navNotifications: "Notifications",
    navAccount: "Account",
    mainNav: "Main navigation",
    supportNav: "Support navigation",
    sectionSupport: "Support",
    sectionAdmin: "Administration",
    dashboard: "Dashboard",
    queue: "Queue",
    reports: "Reports",
    agents: "Agents",
    categories: "Categories",
    slaCalendar: "SLA calendar",
    workflow: "Statuses and priorities",
    templates: "Replies and messages",
    permissions: "Permissions",
  },
});

export const pages = defineMessages({
  fr: {
    notFoundTitle: "Page introuvable",
    notFoundText: "Cette page n'existe pas ou vous n'y avez pas accès.",
    backHome: "Retour à l'accueil",
    deniedTitle: "Accès refusé",
    deniedText: "Votre rôle ne permet pas d'accéder à cet espace.",
    backToMySpace: "Retour à mon espace",
  },
  en: {
    notFoundTitle: "Page not found",
    notFoundText: "This page does not exist or you do not have access to it.",
    backHome: "Back to home",
    deniedTitle: "Access denied",
    deniedText: "Your role does not give access to this area.",
    backToMySpace: "Back to my area",
  },
});
