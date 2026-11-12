package com.maplenou.support.demo;

import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.ticket.application.TicketCommandService;
import com.maplenou.support.ticket.application.TicketCommands.Assign;
import com.maplenou.support.ticket.application.TicketCommands.ChangeStatus;
import com.maplenou.support.ticket.application.TicketCommands.CreateTicket;
import com.maplenou.support.ticket.application.TicketCommands.PostMessage;
import com.maplenou.support.ticket.application.TicketStatistics;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seeds the demo tickets through the same use cases as the API (demo profile only). */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserDirectory directory;
    private final TicketCommandService tickets;
    private final TicketStatistics statistics;

    public DemoDataSeeder(UserDirectory directory, TicketCommandService tickets, TicketStatistics statistics) {
        this.directory = directory;
        this.tickets = tickets;
        this.statistics = statistics;
    }

    @Override
    public void run(ApplicationArguments args) {
        DemoUsers.ALL.forEach(directory::sync);
        if (statistics.totalTickets() > 0) {
            return;
        }
        Actor jean = DemoUsers.byId(DemoUsers.CUSTOMER_JEAN);
        Actor marie = DemoUsers.byId(DemoUsers.SELLER_MARIE);
        Actor kossi = DemoUsers.byId(DemoUsers.DRIVER_KOSSI);
        Actor sophie = DemoUsers.byId(DemoUsers.AGENT_SOPHIE);
        Actor paul = DemoUsers.byId(DemoUsers.AGENT_PAUL);
        Actor amina = DemoUsers.byId(DemoUsers.MANAGER_AMINA);

        Ticket t1 = tickets.create(jean, new CreateTicket("Problème de livraison",
                "Mon colis n'a pas été livré alors qu'il est marqué comme livré. Le livreur n'est jamais passé à mon adresse.",
                "DELIVERY", "HIGH", true, "CMD-2026-0456", null, List.of()));
        tickets.assign(sophie, t1.id(), new Assign(null));
        tickets.postMessage(sophie, t1.id(), new PostMessage(
                "Bonjour Jean, nous sommes désolés pour ce désagrément. Nous vérifions auprès du livreur et revenons vers vous sous 24 h.",
                null, List.of()), MessageVisibility.PUBLIC);
        tickets.postMessage(sophie, t1.id(), new PostMessage(
                "Le livreur a été contacté. Une nouvelle tentative est prévue demain entre 9 h et 12 h.", null, List.of()),
                MessageVisibility.INTERNAL);

        Ticket t2 = tickets.create(marie, new CreateTicket("Paiement non reversé",
                "Le paiement de cette commande n'a toujours pas été reversé sur mon compte vendeur.",
                "PAYMENT", "HIGH", false, "CMD-2026-0789", null, List.of()));
        tickets.assign(amina, t2.id(), new Assign(DemoUsers.AGENT_PAUL));
        tickets.postMessage(paul, t2.id(), new PostMessage(
                "Bonjour Marie, pouvez-vous nous transmettre le relevé de votre compte de reversement ?", null, List.of()),
                MessageVisibility.PUBLIC);
        tickets.changeStatus(paul, t2.id(), new ChangeStatus("WAITING_FOR_CUSTOMER", null));

        tickets.create(kossi, new CreateTicket("Client introuvable",
                "Le client est absent à l'adresse indiquée et ne répond pas au téléphone.",
                "DELIVERY", "MEDIUM", false, null, "LIV-2026-0912", List.of()));

        Ticket t4 = tickets.create(jean, new CreateTicket("Remboursement demandé",
                "Un article de ma commande était endommagé, je demande un remboursement partiel.",
                "RETURN_REFUND", "MEDIUM", false, "CMD-2026-0501", null, List.of()));
        tickets.assign(paul, t4.id(), new Assign(null));
        tickets.postMessage(paul, t4.id(), new PostMessage(
                "Votre remboursement partiel de 5 000 FCFA a été validé.", null, List.of()), MessageVisibility.PUBLIC);
        tickets.changeStatus(paul, t4.id(), new ChangeStatus("RESOLVED", "Remboursement validé"));

        Ticket t5 = tickets.create(jean, new CreateTicket("Accès au compte bloqué",
                "Je n'arrive plus à me connecter à mon compte.", "ACCOUNT", "LOW", false, null, null, List.of()));
        tickets.assign(sophie, t5.id(), new Assign(null));
        tickets.postMessage(sophie, t5.id(), new PostMessage("Votre compte a été débloqué.", null, List.of()),
                MessageVisibility.PUBLIC);
        tickets.changeStatus(sophie, t5.id(), new ChangeStatus("RESOLVED", null));
        tickets.changeStatus(sophie, t5.id(), new ChangeStatus("CLOSED", null));

        log.info("Demo data seeded: {} tickets", statistics.totalTickets());
    }
}
