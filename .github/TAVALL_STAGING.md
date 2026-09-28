# Tavall Cache Staging Root

```text
<!-- tavall-staging:v1 -->
Type: REPOSITORY_INTEGRATION
State: ACTIVE
Branch: staging/platform
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/platform
```

This branch is the combined integration tree for Tavall Cache APIs, TTL/snapshot/invalidation behavior, dependency/package changes, and downstream compatibility. Child merges are integration for combined validation, not production promotion.
