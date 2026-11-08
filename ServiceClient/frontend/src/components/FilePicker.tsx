"use client";

import { useId, useRef } from "react";
import { ATTACHMENT_RULES, validateFiles } from "@/lib/attachments";
import { useFormat, useLocale, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { files as filesMessages } from "@/lib/i18n/messages/files";

/** File selection with the approved formats and limits shown before submission (AC-UX-009). */
export function FilePicker({
  files,
  onChange,
  onError,
  existingTicketBytes = 0,
  disabled,
  compact,
}: {
  files: File[];
  onChange: (files: File[]) => void;
  onError: (message: string | null) => void;
  existingTicketBytes?: number;
  disabled?: boolean;
  compact?: boolean;
}) {
  const t = useMessages(filesMessages);
  const locale = useLocale();
  const fmt = useFormat();
  const input = useRef<HTMLInputElement>(null);
  const hintId = useId();

  function add(list: FileList | null) {
    if (!list) return;
    const next = [...files, ...Array.from(list)];
    const error = validateFiles(next, existingTicketBytes, locale);
    onError(error);
    if (!error) onChange(next);
    if (input.current) input.current.value = "";
  }

  return (
    <div className="stack-sm">
      <div className={compact ? "row" : "dropzone"}>
        <button
          type="button"
          className={`btn ${compact ? "btn-secondary btn-sm" : "btn-tertiary"}`}
          onClick={() => input.current?.click()}
          disabled={disabled || files.length >= ATTACHMENT_RULES.maxFilesPerMessage}
          aria-describedby={hintId}
        >
          {compact ? t.addShort : t.add}
        </button>
        <div id={hintId} className="xsmall muted">
          {t.hint}
        </div>
        <input
          ref={input}
          type="file"
          multiple
          hidden
          accept={ATTACHMENT_RULES.accept}
          onChange={(e) => add(e.target.files)}
          data-testid="file-input"
        />
      </div>
      {files.length > 0 && (
        <ul className="file-chips" aria-label={t.selected} style={{ listStyle: "none", margin: 0, padding: 0 }}>
          {files.map((f, i) => (
            <li key={`${f.name}-${i}`} className="file-chip">
              <span aria-hidden="true">📎</span>
              {f.name} <span className="muted">{fmt.bytes(f.size)}</span>
              <button
                type="button"
                aria-label={format(t.remove, { name: f.name })}
                onClick={() => {
                  onError(null);
                  onChange(files.filter((_, j) => j !== i));
                }}
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
