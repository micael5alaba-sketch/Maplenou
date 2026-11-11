package com.maplenou.support.ticket.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CSV export cells: quoted, one line per ticket, never interpreted as a spreadsheet formula (OWASP CSV injection). */
class CsvWriterTest {

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+33 6", "-2", "@SUM(A1)", "\tcmd"})
    void formulaLikeCellsArePrefixed(String value) {
        assertThat(CsvWriter.cell(value)).startsWith("\"'");
    }

    @Test
    void quotesAndLineBreaksAreEscaped() {
        assertThat(CsvWriter.cell("Il a dit \"non\"\r\nhier")).isEqualTo("\"Il a dit \"\"non\"\"  hier\"");
        assertThat(CsvWriter.cell(null)).isEqualTo("\"\"");
        assertThat(CsvWriter.cell("Colis; abîmé")).isEqualTo("\"Colis; abîmé\"");
    }

    @Test
    void emptyExportStillHasTheBomAndAHeaderInTheChosenLanguage() {
        String fr = new String(CsvWriter.tickets(List.of(), false, ZoneId.of("Africa/Lome")), StandardCharsets.UTF_8);
        String en = new String(CsvWriter.tickets(List.of(), true, ZoneId.of("Africa/Lome")), StandardCharsets.UTF_8);
        assertThat(fr).startsWith("﻿").contains(";");
        assertThat(en).startsWith("﻿");
        assertThat(fr.lines().count()).isEqualTo(1);
        assertThat(fr).isNotEqualTo(en);
    }
}
