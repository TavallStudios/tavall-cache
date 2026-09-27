# Tavall Cache

Tavall Cache is a modular Java library that separates shared cache contracts and semantic routing from adapters for memory, disk, Redis, MongoDB, PostgreSQL, and Qdrant.

## Why Tavall Cache

- Select cache behavior and backing store through separate modules.
- Reuse common key, value, metadata, and routing contracts across adapters.
- Keep storage-specific integrations behind adapter boundaries.

## Features

- Foundational cache keys, values, metadata, and statistics.
- Semantic cache entries, tags, tier selection, and routing policy.
- Memory, local disk, Redis, MongoDB, PostgreSQL, and Qdrant tier adapters.
- Cross-module repository verification.

## Quick Start

This repository does not document published dependency coordinates. Use JDK 25 and the committed Gradle Wrapper to build and verify the source:

```bash
./gradlew check
```

## How It Works

The base module supplies common types. The semantic module adds routing and tier contracts. Storage modules implement those contracts for specific backing stores; applications choose and configure the adapters they need.

## Project Structure

├── [`abstract-cache-system`](abstract-cache-system/README.md)
├── [`abstract-cache-semantic`](abstract-cache-semantic/README.md)
├── [`abstract-cache-storage-memory`](abstract-cache-storage-memory/README.md)
├── [`abstract-cache-storage-disk`](abstract-cache-storage-disk/README.md)
├── [`abstract-cache-storage-redis`](abstract-cache-storage-redis/README.md)
├── [`abstract-cache-storage-mongo`](abstract-cache-storage-mongo/README.md)
├── [`abstract-cache-storage-postgres`](abstract-cache-storage-postgres/README.md)
├── [`abstract-cache-storage-qdrant`](abstract-cache-storage-qdrant/README.md)
└── [`abstract-cache-suite`](abstract-cache-suite/README.md)

## Documentation

- [Contribution guide](CONTRIBUTING.md) — repository-specific development and validation.
- [Workflow compatibility pointer](docs/quality/GIT_WORKFLOW.md) — redirects to shared policy.
- [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md) — shared contribution and review guidance.


## Requirements / Compatibility

- JDK 25 for the configured Gradle toolchain.
- Backing-store/provider setup is selected and configured by consuming applications.

## Building From Source

Run `./gradlew check`. Provider integration tests may require external services.

## Contributing

Open a GitHub pull request and follow the repository [CONTRIBUTING.md](CONTRIBUTING.md).

## License

No license file is currently tracked in this repository. Contact the maintainers before redistributing or reusing the code.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-cache/README.md` | 2026-09-27 12:59 PM PDT | [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/tavall-cache/README.md` | `TavallStudios/tavall-cache/README.md` | [PR #14](https://github.com/TavallStudios/tavall-cache/pull/14) | Reworked the public root README to route contributors and map the current modules. |

</details>
