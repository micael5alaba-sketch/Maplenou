"use client";

import Link from "next/link";
import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useFormat, useLabels, useMessages } from "@/lib/i18n/client";
import { newTicket } from "@/lib/i18n/messages/newTicket";
import { useApi } from "@/lib/useApi";
import { defaultPriority, priorityCodes, useVocabulary } from "@/lib/vocabulary";
import type { Category, DeliverySummary, OrderSummary, TicketDetail, TicketPriority } from "@/lib/types";
import { FilePicker } from "@/components/FilePicker";
import { Alert, Loading } from "@/components/ui";
import { useSession } from "@/components/session";

interface References {
  orders: OrderSummary[];
  deliveries: DeliverySummary[];
}


/** A3 — Create Ticket. */
function CreateTicketForm() {
  const t = useMessages(newTicket);
  const labels = useLabels();
  const f = useFormat();
  const errorText = useErrorText();
  const params = useSearchParams();
  const session = useSession();
  const categories = useApi<Category[]>("/categories");
  const refs = useApi<References>("/me/references");
  const driver = session.role === "DELIVERY_DRIVER";
  const vocabulary = useVocabulary();
  // the priorities a requester may choose (CRITICAL is support-only, BR-036), least urgent first
  const requesterPriorities = priorityCodes(vocabulary, true).reverse();

  const [category, setCategory] = useState(params.get("categorie") ?? (driver ? "DELIVERY" : ""));
  const [reference, setReference] = useState("");
  const [priority, setPriority] = useState<TicketPriority>(() => defaultPriority(vocabulary));
  const [urgent, setUrgent] = useState(false);
  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [fileError, setFileError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  // The error itself is kept (not its text) so the message follows a language switch.
  const [submitError, setSubmitError] = useState<{ error: unknown } | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState<TicketDetail | null>(null);

  function validate() {
    const e: Record<string, string> = {};
    if (!category) e.category = t.categoryRequired;
    if (!subject.trim()) e.subject = t.subjectRequired;
    if (!description.trim()) e.description = t.descriptionRequired;
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function submit(ev: React.FormEvent) {
    ev.preventDefault();
    if (submitting || !validate() || fileError) return;
    setSubmitting(true);
    setSubmitError(null);
    const payload = {
      subject: subject.trim(),
      description: description.trim(),
      categoryCode: category,
      priority,
      urgent,
      orderRef: !driver && reference ? reference : null,
      deliveryRef: driver && reference ? reference : null,
    };
    try {
      const ticket = files.length
        ? await api.multipart<TicketDetail>("/tickets", "ticket", payload, files)
        : await api.post<TicketDetail>("/tickets", payload);
      setCreated(ticket);
      window.scrollTo({ top: 0 });
    } catch (err) {
      if (err instanceof ApiError && err.problem.errors) setErrors(err.problem.errors);
      setSubmitError({ error: err });
    } finally {
      setSubmitting(false);
    }
  }

  if (created) {
    return (
      <div className="card stack" style={{ textAlign: "center" }} role="status">
        <div style={{ fontSize: "2.2rem", color: "var(--success)" }} aria-hidden="true">✓</div>
        <h1>{t.createdTitle}</h1>
        <div>
          <div className="small muted">{t.ticketNumber}</div>
          <div className="ticket-number" style={{ fontSize: "1.4rem" }} data-testid="created-ticket-number">
            {created.ticketNumber}
          </div>
        </div>
        <p className="secondary small">{t.createdBody}</p>
        <div className="row" style={{ justifyContent: "center" }}>
          <Link className="btn btn-primary" href={`/aide/tickets/${created.id}`}>{t.viewTicket}</Link>
          <Link className="btn btn-secondary" href="/aide">{t.backToHelp}</Link>
        </div>
      </div>
    );
  }

  return (
    <form className="stack" onSubmit={submit} noValidate>
      <Link href="/aide" className="back-link">{t.backLink}</Link>
      <h1>{driver ? t.titleIncident : t.titleTicket}</h1>
      {submitError && (
        <Alert tone="danger" title={t.notCreatedTitle}>
          {submitError.error instanceof ApiError ? errorText(submitError.error) : t.createFailed}
        </Alert>
      )}

      <div className="field">
        <label htmlFor="category" className="required">{t.category}</label>
        {categories.loading ? <Loading lines={1} /> : (
          <select id="category" className="select" value={category} onChange={(e) => setCategory(e.target.value)}
                  aria-invalid={!!errors.category} aria-describedby={errors.category ? "category-err" : undefined}>
            <option value="">{t.selectCategory}</option>
            {categories.data?.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
          </select>
        )}
        {errors.category && <span id="category-err" className="error">{errors.category}</span>}
      </div>

      <div className="field">
        <label htmlFor="reference">{driver ? t.relatedDelivery : t.relatedOrder} <span className="muted small">{t.optional}</span></label>
        <select id="reference" className="select" value={reference} onChange={(e) => setReference(e.target.value)} disabled={refs.loading}>
          <option value="">{refs.loading ? t.loading : driver ? t.noDelivery : t.noOrder}</option>
          {refs.data?.orders.map((o) => (
            <option key={o.ref} value={o.ref}>{o.ref} — {f.date(o.orderedAt)} — {f.money(o.amount, o.currency)}</option>
          ))}
          {refs.data?.deliveries.map((d) => (
            <option key={d.ref} value={d.ref}>{d.ref} — {d.customerName} — {d.destination}</option>
          ))}
        </select>
      </div>

      <fieldset className="field" style={{ border: 0, padding: 0, margin: 0 }}>
        <legend className="label" style={{ marginBottom: 6 }}>{t.priority}</legend>
        <div className="segmented">
          {requesterPriorities.map((p) => (
            <label key={p}>
              <input type="radio" name="priority" value={p} checked={priority === p} onChange={() => setPriority(p)} />
              {labels.priority[p]}
            </label>
          ))}
        </div>
        <label className="checkbox" style={{ marginTop: 8 }}>
          <input type="checkbox" checked={urgent} onChange={(e) => setUrgent(e.target.checked)} />
          <span>
            {t.urgent}
            <span className="hint" style={{ display: "block" }}>
              {t.urgentHint}
            </span>
          </span>
        </label>
      </fieldset>

      <div className="field">
        <label htmlFor="subject" className="required">{t.subject}</label>
        <input id="subject" className="input" value={subject} maxLength={200} onChange={(e) => setSubject(e.target.value)}
               placeholder={driver ? t.subjectPlaceholderIncident : t.subjectPlaceholderTicket}
               aria-invalid={!!errors.subject} aria-describedby={errors.subject ? "subject-err" : undefined} />
        {errors.subject && <span id="subject-err" className="error">{errors.subject}</span>}
      </div>

      <div className="field">
        <label htmlFor="description" className="required">{t.description}</label>
        <textarea id="description" className="textarea" value={description} maxLength={5000}
                  onChange={(e) => setDescription(e.target.value)} placeholder={t.descriptionPlaceholder}
                  aria-invalid={!!errors.description} aria-describedby={errors.description ? "description-err" : undefined} />
        {errors.description && <span id="description-err" className="error">{errors.description}</span>}
      </div>

      <div className="field">
        <span className="label">{t.attachments} <span className="muted small">{t.optional}</span></span>
        <FilePicker files={files} onChange={setFiles} onError={setFileError} disabled={submitting} />
        {fileError && <span className="error" role="alert">{fileError}</span>}
      </div>

      <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
        {submitting ? t.sending : driver ? t.sendIncident : t.sendTicket}
      </button>
    </form>
  );
}

export default function NewTicketPage() {
  return (
    <Suspense fallback={<Loading />}>
      <CreateTicketForm />
    </Suspense>
  );
}
