package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.ticket.application.view.TicketViews.PersonView;
import com.maplenou.support.ticket.application.view.TicketViews.TicketSummary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CSV for spreadsheets: UTF-8 with a byte-order mark and ";" separators (what Excel expects in French settings),
 * every value quoted. Cells starting with = + - @ are prefixed with ' so a spreadsheet never runs them as formulas
 * (CSV injection).
 */
final class CsvWriter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CsvWriter() {
    }

    static byte[] tickets(List<TicketSummary> rows, boolean english, ZoneId zone) {
        StringBuilder sb = new StringBuilder();
        line(sb, english
                ? new String[]{"Number", "Subject", "Category", "Priority", "Status", "Requester", "Requester role",
                "Assigned to", "Escalated", "SLA", "Order", "Delivery", "Created", "Updated"}
                : new String[]{"Numéro", "Objet", "Catégorie", "Priorité", "Statut", "Demandeur", "Rôle du demandeur",
                "Attribué à", "Escaladé", "SLA", "Commande", "Livraison", "Créé le", "Mis à jour le"});
        for (TicketSummary t : rows) {
            line(sb, new String[]{t.ticketNumber(), t.subject(),
                    t.category() == null ? "" : (english && t.category().nameEn() != null ? t.category().nameEn() : t.category().name()),
                    t.priority(), t.status(), name(t.requester()),
                    t.requester() == null || t.requester().role() == null ? "" : t.requester().role().name(),
                    name(t.assignedAgent()), t.escalated() ? (english ? "yes" : "oui") : (english ? "no" : "non"),
                    t.slaState() == null ? "" : t.slaState().name(), nz(t.orderRef()), nz(t.deliveryRef()),
                    date(t.createdAt(), zone), date(t.updatedAt(), zone)});
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        out.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static void line(StringBuilder sb, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(cell(cells[i]));
        }
        sb.append("\r\n");
    }

    static String cell(String value) {
        String v = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        if (!v.isEmpty() && "=+-@\t".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String name(PersonView p) {
        return p == null ? "" : p.name();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String date(Instant i, ZoneId zone) {
        return i == null ? "" : DATE_TIME.format(i.atZone(zone));
    }
}
