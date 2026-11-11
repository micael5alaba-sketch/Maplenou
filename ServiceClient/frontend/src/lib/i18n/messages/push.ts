import { defineMessages } from "../define";

/** Browser push notifications card (notifications screens, requester and support). */
export const push = defineMessages({
  fr: {
    title: "Notifications push",
    loading: "Vérification des notifications push…",
    unsupported: "Ce navigateur ne prend pas en charge les notifications push. Vos notifications restent visibles ici.",
    notConfigured:
      "Les notifications push ne sont pas encore configurées sur ce serveur. Vos notifications restent visibles ici.",
    blocked:
      "Les notifications sont bloquées pour ce site. Autorisez-les dans les réglages du navigateur (icône à gauche de l'adresse), puis rechargez la page.",
    off: "Recevez une alerte sur cet appareil quand un ticket reçoit une réponse ou change de statut.",
    on: "Les notifications push sont activées sur cet appareil.",
    enable: "Activer les notifications push",
    disable: "Désactiver",
    test: "Envoyer une notification de test",
    enabled: "Notifications push activées sur cet appareil.",
    disabled: "Notifications push désactivées sur cet appareil.",
    testSent: "Notification de test envoyée à {count} appareil(s).",
    failed:
      "Impossible d'activer les notifications push sur ce navigateur. Réessayez, ou utilisez une fenêtre normale (pas de navigation privée).",
    testLogOnly:
      "Le serveur n'a pas de clé Firebase : la notification de test a seulement été écrite dans le journal du serveur.",
  },
  en: {
    title: "Push notifications",
    loading: "Checking push notifications…",
    unsupported: "This browser does not support push notifications. Your notifications stay visible here.",
    notConfigured: "Push notifications are not configured on this server yet. Your notifications stay visible here.",
    blocked:
      "Notifications are blocked for this site. Allow them in the browser settings (icon left of the address), then reload the page.",
    off: "Get an alert on this device when a ticket receives a reply or changes status.",
    on: "Push notifications are on for this device.",
    enable: "Turn on push notifications",
    disable: "Turn off",
    test: "Send a test notification",
    enabled: "Push notifications are on for this device.",
    disabled: "Push notifications are off for this device.",
    testSent: "Test notification sent to {count} device(s).",
    failed: "Push notifications could not be turned on in this browser. Try again, or use a normal (not private) window.",
    testLogOnly: "The server has no Firebase key: the test notification was only written to the server log.",
  },
});
