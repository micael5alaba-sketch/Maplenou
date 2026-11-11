package com.maplenou.support.attachment.application;

/** DEC-003 limits plus the antivirus behaviour, built from the configuration by the adapter layer. */
public record AttachmentLimits(long maxFileBytes, int maxFilesPerMessage, long maxTicketBytes, boolean failClosed) {
}
