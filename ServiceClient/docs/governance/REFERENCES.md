# Project References

| Reference | Use in Maplenou |
| --- | --- |
| ISO/IEC/IEEE 29148:2018 | Requirements engineering, requirements quality, traceability |
| Agile Manifesto Principles | Iterative delivery and frequent validation |
| Mermaid documentation | Version-controlled project/UML diagrams |
| Zensical | Documentation sites (user, staff, developer × FR/EN) and GitHub Pages |
| GitHub Actions documentation | CI/CD and documentation automation |
| OWASP Authorization Cheat Sheet | Least privilege, deny-by-default, authorization checks |
| OWASP File Upload Cheat Sheet | Attachment validation and storage |
| RFC 9457 | Standardized HTTP API error representation (implemented) |
| RFC 7519 / OAuth 2.0 resource server | JWT bearer authentication of the API |
| W3C WCAG 2.2 | Accessibility baseline of the web app (CS-02 design system) |
| Nielsen Norman Group — 10 Usability Heuristics | Usability evaluation of the screens (CS-02) |
| OWASP CSV Injection | Neutralised cells in the CSV export (`CsvWriter`) |
| OWASP Logging Cheat Sheet | Structured logs with a request id, no personal data in pushes |
| Hexagonal architecture (Ports and Adapters, A. Cockburn) | `domain` / `application` + ports / `adapter` layout of every backend module |
| ArchUnit documentation | Module and layer boundaries enforced by `ArchitectureTest` |
| Transactional outbox pattern (microservices.io) | Domain events stored with the change, delivered after commit |
| Prometheus and Grafana documentation | Metrics (`/actuator/prometheus`), alert rules, dashboards |
| ClamAV clamd protocol (INSTREAM) | Antivirus scan of attachments |
| Cloudinary authenticated delivery | Private attachment storage with signed, short-lived URLs |
| Caddy documentation | HTTPS reverse proxy of the public deployment (automatic certificates) |
| Spring Boot 4 reference documentation | Backend conventions (configuration, security, testing) |
| Flyway documentation | Versioned database migrations |
| Testcontainers documentation | Integration tests against real PostgreSQL |
| Next.js (App Router) documentation | Frontend conventions |
| Playwright documentation | Browser end-to-end tests |
| Firebase Cloud Messaging documentation | Push notifications |
| Conventional Commits | Consistent Git commit messages |
| Keep a Changelog | Structured project change history |
| Semantic Versioning | Release versioning |

Official links: [WCAG 2.2](https://www.w3.org/TR/WCAG22/) ·
[NN/g heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/) ·
[ISO/IEC/IEEE 29148](https://www.iso.org/standard/72089.html) · [Agile principles](https://agilemanifesto.org/principles) ·
[OWASP Authorization](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html) ·
[OWASP File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html) ·
[OWASP CSV Injection](https://owasp.org/www-community/attacks/CSV_Injection) ·
[Mermaid](https://mermaid.js.org/intro/getting-started.html) · [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) ·
[ArchUnit](https://www.archunit.org/userguide/html/000_Index.html) ·
[Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html) ·
[Caddy](https://caddyserver.com/docs/) · [Zensical](https://zensical.org/docs/).

The former `REFERENCES-CS-02.md` (UX/UI phase) is merged into this page.
