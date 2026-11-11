# Maplenou Customer Service

This is the developer documentation of the Maplenou Customer Service and Ticket Management MVP: a Spring Boot API,
a Next.js web application, three documentation sites (user, staff, developer) in French and English, and a
documentation assistant powered by Ollama (Ollama cloud by default). It contains the **developer guide** (how to run, change, test and
operate the code) and the **project records** (requirements, UX/UI, architecture, validation, governance).

## Current objective

Build the first functional Customer Service and Ticket Management MVP.

**Status:** CS-01 and CS-02 approved · CS-03 → CS-09 implemented and validated, awaiting owner approval.

## Developer guide

Start here if you are new to the code base. The whole stack runs with Docker only.

| Page | What you will find |
| --- | --- |
| [Getting started](guide/getting-started.md) | Run the app, the documentation and the assistant with Docker; ports; optional native toolchains |
| [Repository tour](guide/repository.md) | What each folder contains and where to put new things |
| [Architecture](guide/architecture.md) | Modular monolith, ports and adapters, BFF, after-commit events |
| [Backend](guide/backend.md) | Packages, request flow, errors, Flyway, caching, security, notifications, adding an endpoint |
| [Backend processing](guide/backend-processing.md) | One request step by step, the business flows, manual API tests |
| [Test the API (Swagger, Postman)](guide/api-testing.md) | Swagger UI with a demo token, the Postman collection and newman |
| [Firebase push notifications](guide/firebase.md) | Connect a Firebase project, browser push, Flutter apps, troubleshooting |
| [Maplenou platform integration](guide/maplenou-integration.md) | ServiceClient in the Maplenou repository: sign-in with Maplenou accounts, support roles, real orders from Bakend, import of the former tickets, rollout |
| [Frontend](guide/frontend.md) | App Router layout, BFF proxy, data fetching, FR/EN and themes, accessibility, adding a screen |
| [Documentation sites](guide/documentation.md) | The six Zensical sites, `build.py`, writing rules, publishing |
| [Documentation assistant](guide/assistant.md) | The Ollama RAG assistant: retrieval, API, configuration, Ollama cloud, on the computer or in Docker, troubleshooting, answer cache, security |
| [Testing](guide/testing.md) | Commands to verify the project with or without Docker, smoke test |
| [Caching and disk space](guide/caching.md) | Every cache and its limits, keeping Docker images and disks small, clean-up |
| [Operations](guide/operations.md) | `.env` variables, secrets, health, logs, backups, disk space, publishing, upgrades |
| [Contributing](guide/contributing.md) | Workflow, commits, definition of done, change control |
| [AI agents](guide/ai-agents.md) | How AI coding agents work in this repository and reproduce it |

```bash
cp .env.example .env                                 # once (Windows: copy .env.example .env)
docker compose up -d --build --wait                  # app http://localhost:3001 · API http://localhost:8081
docker compose --profile docs up -d --build --wait   # + documentation http://localhost:8006 and its assistant
scripts/doctor.sh                                    # something does not start? (Windows: scripts\doctor.ps1)
./scripts/test.sh all                                # every test suite in containers
```

## Continue development

Start with:

1. [Project Continuity](project/PROJECT-CONTINUITY.md)
2. [Development Matrix](project/DEVELOPMENT-MATRIX.md)
3. [Project Plan](project/PROJECT-PLAN.md)
4. [Architecture](architecture/README.md) and [decisions](architecture/decisions.md)
5. [References and Conventions](governance/REFERENCES.md)
6. [Reproduction Guide](governance/REPRODUCTION.md)
7. [Upgrade Plan](governance/UPGRADE-PLAN.md)
