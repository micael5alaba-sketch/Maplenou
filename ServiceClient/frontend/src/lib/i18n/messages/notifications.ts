import type { Locale } from "../config";
import { format } from "../define";
import type { Notification } from "../../types";
import { statusLabels } from "./common";

/** English templates per notification type; French uses the backend text as written. */
const EN: Record<string, { title: string; body: string }> = {
  TICKET_CREATED: { title: "Ticket created", body: "Your ticket {n} has been created. We will let you know when there is an update." },
  NEW_TICKET_IN_QUEUE: { title: "New ticket", body: "{n} is waiting in the queue." },
  TICKET_ASSIGNED: { title: "Ticket assigned", body: "{n} has been assigned to you." },
  AGENT_REPLIED: { title: "New reply", body: "Your ticket {n} has a new reply." },
  REQUESTER_REPLIED: { title: "Requester replied", body: "{n}: the requester has replied." },
  STATUS_CHANGED: { title: "Status updated", body: "The status of your ticket {n} changed: {status}." },
  INFORMATION_REQUESTED: { title: "Information requested", body: "Support needs more information for ticket {n}." },
  TICKET_RESOLVED: { title: "Ticket resolved", body: "Your ticket {n} has been resolved." },
  TICKET_CLOSED: { title: "Ticket closed", body: "Your ticket {n} is closed." },
  REOPEN_REQUESTED: { title: "Reopening request", body: "{n}: the requester would like to reopen the ticket." },
  REOPEN_DECIDED: { title: "Reopening request handled", body: "Your request to reopen ticket {n} was {decision}." },
  TICKET_ESCALATED: { title: "Ticket escalated", body: "{n} has been escalated." },
};

export function localizeNotification(n: Notification, locale: Locale): { title: string; body: string } {
  const tpl = EN[n.type];
  if (locale === "fr" || !tpl || !n.ticketNumber) return { title: n.title, body: n.body };
  const status = n.detail ? (statusLabels.en as Record<string, string>)[n.detail] ?? n.detail : "";
  const decision = n.detail === "APPROVED" ? "approved" : "refused";
  return { title: tpl.title, body: format(tpl.body, { n: n.ticketNumber, status, decision }) };
}
