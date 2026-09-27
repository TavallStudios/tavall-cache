# abstract-cache-suite

Owns the repository verification suite that exercises the base, semantic, and storage-adapter modules.

## Responsibility

### Owns
- Cross-module unit/integration verification.
- Integration tests that require external services, when configured.

### Does Not Own
- Production cache APIs or adapters.
- A runtime service or deployment.

## Repository Structure

tavall-cache/
├── [`abstract-cache-system`](../abstract-cache-system/README.md)
├── [`abstract-cache-semantic`](../abstract-cache-semantic/README.md)
├── [`abstract-cache-storage-memory`](../abstract-cache-storage-memory/README.md)
├── [`abstract-cache-storage-disk`](../abstract-cache-storage-disk/README.md)
├── [`abstract-cache-storage-redis`](../abstract-cache-storage-redis/README.md)
├── [`abstract-cache-storage-mongo`](../abstract-cache-storage-mongo/README.md)
├── [`abstract-cache-storage-postgres`](../abstract-cache-storage-postgres/README.md)
├── [`abstract-cache-storage-qdrant`](../abstract-cache-storage-qdrant/README.md)
└── **[`abstract-cache-suite`](README.md) ← This Module**

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`abstract-cache-system`](../abstract-cache-system/README.md) | Exercises the module at the repository verification boundary. |
| [`abstract-cache-semantic`](../abstract-cache-semantic/README.md) | Exercises the module at the repository verification boundary. |
| [`abstract-cache-storage-memory`](../abstract-cache-storage-memory/README.md) | Exercises the module at the repository verification boundary. |
| [`abstract-cache-storage-disk`](../abstract-cache-storage-disk/README.md) | Exercises the module at the repository verification boundary. |
| [`abstract-cache-storage-redis`](../abstract-cache-storage-redis/README.md) | Exercises the module at the repository verification boundary. |
| [`abstract-cache-storage-mongo`](../abstract-cache-storage-mongo/README.md) | Exercises the module at the repository verification boundary. |
| [`abstract-cache-storage-postgres`](../abstract-cache-storage-postgres/README.md) | Exercises the module at the repository verification boundary. |
| [`abstract-cache-storage-qdrant`](../abstract-cache-storage-qdrant/README.md) | Exercises the module at the repository verification boundary. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| GENERAL | [Repository README](../README.md) | Public overview and module map. | GitHub |
| Technical | [Contribution guide](../CONTRIBUTING.md) | Repository-specific development and validation. | GitHub |

## Deployment

> This module is not independently deployed.

Runtime owner: `None`. No Deployment record applies to this library or test-only boundary.

## Development

- **Module Type:** `TEST_SUITE`
- **Runtime:** `None`
- **Current PR Stack:** [staging integration #10](https://github.com/TavallStudios/tavall-cache/pull/10) and [CI transition #11](https://github.com/TavallStudios/tavall-cache/pull/11); documentation update: [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14).
- Repository-specific development guide: [CONTRIBUTING.md](../CONTRIBUTING.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-cache/abstract-cache-suite/README.md` | 2026-09-27 12:59 PM PDT | [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `CREATED` | `TavallStudios/tavall-cache/abstract-cache-suite/README.md` | — | [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14) | Added a contextual module README with source-backed ownership and routing. |

</details>
