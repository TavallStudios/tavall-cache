# abstract-cache-storage-redis

Provides the Redis adapter for semantic cache tier storage.

## Responsibility

### Owns
- Translate semantic tier operations to the Redis backing-store boundary.
- Expose the module's concrete adapter implementation to cache consumers.

### Does Not Own
- Semantic routing policy or shared cache contracts.
- Operating or deploying the external backing store.

## Repository Structure

tavall-cache/
├── [`abstract-cache-system`](../abstract-cache-system/README.md)
├── [`abstract-cache-semantic`](../abstract-cache-semantic/README.md)
├── [`abstract-cache-storage-memory`](../abstract-cache-storage-memory/README.md)
├── [`abstract-cache-storage-disk`](../abstract-cache-storage-disk/README.md)
├── **[`abstract-cache-storage-redis`](README.md) ← This Module**
├── [`abstract-cache-storage-mongo`](../abstract-cache-storage-mongo/README.md)
├── [`abstract-cache-storage-postgres`](../abstract-cache-storage-postgres/README.md)
├── [`abstract-cache-storage-qdrant`](../abstract-cache-storage-qdrant/README.md)
└── [`abstract-cache-suite`](../abstract-cache-suite/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`abstract-cache-semantic`](../abstract-cache-semantic/README.md) | Implements the semantic tier adapter contract. |
| [`abstract-cache-suite`](../abstract-cache-suite/README.md) | Covered by repository verification where applicable. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| GENERAL | [Repository README](../README.md) | Public overview and module map. | GitHub |
| Technical | [Contribution guide](../CONTRIBUTING.md) | Repository-specific development and validation. | GitHub |

## Deployment

> This module is not independently deployed.

Runtime owner: `None`. No Deployment record applies to this library or test-only boundary.

## Development

- **Module Type:** `ADAPTER`
- **Runtime:** `None`
- **Current PR Stack:** [staging integration #10](https://github.com/TavallStudios/tavall-cache/pull/10) and [CI transition #11](https://github.com/TavallStudios/tavall-cache/pull/11); documentation update: __PR_LINK__.
- Repository-specific development guide: [CONTRIBUTING.md](../CONTRIBUTING.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-cache/abstract-cache-storage-redis/README.md` | 2026-09-27 12:51 PM PDT | __PR_URL__ |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:51 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:51 PM PDT | GitHub | `CREATED` | `TavallStudios/tavall-cache/abstract-cache-storage-redis/README.md` | — | __PR_URL__ | Added a contextual module README with source-backed ownership and routing. |

</details>
