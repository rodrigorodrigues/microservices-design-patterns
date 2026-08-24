# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root, or
- **`CONTEXT-MAP.md`** at the repo root if it exists — it points at one `CONTEXT.md` per context. Read each one relevant to the topic.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in. In multi-context repos, also check `src/<context>/docs/adr/` for context-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

This repo currently has a single root `CONTEXT.md`, covering cross-cutting, system-wide vocabulary (e.g. Authentication/JWT, shared across services rather than owned by one). `docs/adr/` at the root holds the decisions that go with it.

```
/
├── CONTEXT.md                         ← system-wide vocabulary
├── docs/adr/                          ← system-wide decisions
└── person-service/, go-service/, scala-address-service/, ...
```

Given the number of independent services in this repo, expect this to grow into the **multi-context** layout over time — a root `CONTEXT-MAP.md` pointing at one `CONTEXT.md` per service, each with its own `docs/adr/` for service-scoped decisions:

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← system-wide decisions
├── person-service/
│   ├── CONTEXT.md
│   └── docs/adr/                      ← service-specific decisions
└── ...                                 ← one CONTEXT.md per service directory once it has local vocabulary worth capturing
```

Don't create a service's `CONTEXT.md`/`CONTEXT-MAP.md` ahead of need — per the rule above, `/domain-modeling` creates these lazily, the first time a service actually accrues terms or decisions that are local to it rather than system-wide.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in the relevant `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders) — but worth reopening because…_
