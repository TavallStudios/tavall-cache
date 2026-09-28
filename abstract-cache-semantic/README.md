# abstract-cache-semantic

Owns the semantic cache API, entry/key/tag models, routing policy, tier SPI, serialization contracts, and semantic statistics.

## Responsibility

### Owns
- Semantic cache construction and tier-routing decisions.
- Adapter/codec interfaces, semantic entries, tags, and routing statistics.

### Does Not Own
- External database clients or their service lifecycle.
- A standalone service or deployment.

## Repository Structure

tavall-cache/
├── [`abstract-cache-system`](../abstract-cache-system/README.md)
├── **[`abstract-cache-semantic`](README.md) ← This Module**
├── [`abstract-cache-storage-memory`](../abstract-cache-storage-memory/README.md)
├── [`abstract-cache-storage-disk`](../abstract-cache-storage-disk/README.md)
├── [`abstract-cache-storage-redis`](../abstract-cache-storage-redis/README.md)
├── [`abstract-cache-storage-mongo`](../abstract-cache-storage-mongo/README.md)
├── [`abstract-cache-storage-postgres`](../abstract-cache-storage-postgres/README.md)
├── [`abstract-cache-storage-qdrant`](../abstract-cache-storage-qdrant/README.md)
└── [`abstract-cache-suite`](../abstract-cache-suite/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`abstract-cache-system`](../abstract-cache-system/README.md) | Uses base cache contracts and metadata. |
| [`abstract-cache-storage-memory`](../abstract-cache-storage-memory/README.md) | A concrete semantic-tier adapter. |
| [`abstract-cache-suite`](../abstract-cache-suite/README.md) | Validates routing and storage integrations. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| GENERAL | [Repository README](../README.md) | Public overview and module map. | GitHub |
| Technical | [Contribution guide](../CONTRIBUTING.md) | Repository-specific development and validation. | GitHub |
| Progression | [Abstract Cache Semantic Progression](../docs/progression/ABSTRACT_CACHE_SEMANTIC_PROGRESSION.md) | Module implementation, validation, and history. | GitHub |
| Progression | [Tavall Cache System Progression](../docs/progression/TAVALL_CACHE_SYSTEM_PROGRESSION.md) | Cross-module architecture and system acceptance. | GitHub |

## Deployment

> This module is not independently deployed.

Runtime owner: `None`. No Deployment record applies to this library or test-only boundary.

## Development

- **Module Type:** `LIBRARY`
- **Runtime:** `None`
- **Current PR Stack:** [staging integration #10](https://github.com/TavallStudios/tavall-cache/pull/10) and [CI transition #11](https://github.com/TavallStudios/tavall-cache/pull/11); documentation update: [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14).
- Repository-specific development guide: [CONTRIBUTING.md](../CONTRIBUTING.md).

- **Progression:** [Module Progression](../docs/progression/ABSTRACT_CACHE_SEMANTIC_PROGRESSION.md) · [System Progression](../docs/progression/TAVALL_CACHE_SYSTEM_PROGRESSION.md).
- **Module CI:** Missing in audited main: `.tavallci/ci.yaml`.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-cache/abstract-cache-semantic/README.md` | 2026-09-27 5:59 PM PDT | [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `CREATED` | `TavallStudios/tavall-cache/abstract-cache-semantic/README.md` | — | [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14) | Added a contextual module README with source-backed ownership and routing. |
| 2026-09-27 5:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/tavall-cache/abstract-cache-semantic/README.md` | `TavallStudios/tavall-cache/abstract-cache-semantic/README.md` | [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14) | Added module and System Progression routes and recorded module CI state. |

</details>
