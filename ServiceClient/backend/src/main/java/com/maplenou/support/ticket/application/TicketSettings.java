package com.maplenou.support.ticket.application;

import java.time.Duration;

/**
 * Settings of the ticket module, built from the configuration by the adapter layer.
 *
 * @param autoCloseAfter resolved tickets are closed after this time without a reopening request (null = never)
 * @param referenceTtl   how long the vocabulary and categories stay cached
 * @param exportLimit    maximum number of rows of a CSV export
 */
public record TicketSettings(Duration autoCloseAfter, Duration referenceTtl, int exportLimit) {
}
