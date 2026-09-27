# abstract-cache-system

Owns foundational cache keys, values, metadata, maps, registry access, and statistics contracts.

## Responsibility

### Owns
- Common key/value/domain/type/version models and metadata.
- Cache maps and registry/statistics interfaces.

### Does Not Own
- Semantic routing or storage-specific persistence.
- An independently running cache service.

## Repository Structure

tavall-cache/
├── **[`abstract-cache-system`](README.md) ← This Module**
├── [`abstract-cache-semantic`](../abstract-cache-semantic/README.md)
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
| [`abstract-cache-semantic`](../abstract-cache-semantic/README.md) | Builds semantic behavior on the base contracts. |
| [`abstract-cache-suite`](../abstract-cache-suite/README.md) | Exercises the shared contracts. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| GENERAL | [Repository README](../README.md) | Public overview and module map. | GitHub |
| Technical | [Contribution guide](../CONTRIBUTING.md) | Repository-specific development and validation. | GitHub |

## Deployment

> This module is not independently deployed.

Runtime owner: `None`. No Deployment record applies to this library or test-only boundary.

## Development

- **Module Type:** `LIBRARY`
- **Runtime:** `None`
- **Current PR Stack:** [staging integration #10](https://github.com/TavallStudios/tavall-cache/pull/10) and [CI transition #11](https://github.com/TavallStudios/tavall-cache/pull/11); documentation update: __PR_LINK__.
- Repository-specific development guide: [CONTRIBUTING.md](../CONTRIBUTING.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-cache/abstract-cache-system/README.md` | 2026-09-27 12:51 PM PDT | __PR_URL__ |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:51 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:51 PM PDT | GitHub | `CREATED` | `TavallStudios/tavall-cache/abstract-cache-system/README.md` | — | __PR_URL__ | Added a contextual module README with source-backed ownership and routing. |

</details>
