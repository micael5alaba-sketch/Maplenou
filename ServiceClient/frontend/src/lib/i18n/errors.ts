import { ApiError } from "../api";
import type { Locale } from "./config";
import { format } from "./define";
import { ERROR_CODES_EN, errorMessages } from "./messages/errors";

/** User-facing text of an error in the given language (see messages/errors.ts for the strategy). */
export function describeError(error: unknown, locale: Locale): string {
  const t = errorMessages[locale];
  if (!(error instanceof ApiError)) {
    return error instanceof Error && error.message ? error.message : t.generic;
  }
  const { status, code, detail, errors } = error.problem;
  if (status === 0) return t.network;
  if (locale === "fr" && detail) return detail;
  if (code === "VALIDATION_FAILED" && errors && Object.keys(errors).length) {
    return format(t.invalidFields, { fields: Object.keys(errors).join(", ") });
  }
  if (locale === "en" && code && ERROR_CODES_EN[code]) return ERROR_CODES_EN[code];
  if (status === 401) return t.status401;
  if (status === 403) return t.status403;
  if (status === 404) return t.status404;
  if (status >= 500) return t.status5xx;
  return t.generic;
}
