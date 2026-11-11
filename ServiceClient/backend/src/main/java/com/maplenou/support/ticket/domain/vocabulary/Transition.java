package com.maplenou.support.ticket.domain.vocabulary;

/** A status change support may make by hand (automatic changes such as assignment are not listed). */
public record Transition(String from, String to) {
}
