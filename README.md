# agenda4j

`agenda4j` is a MongoDB-backed Java job scheduler split into three modules:

- `agenda4j-core`: public scheduling API and contracts
- `agenda4j-mongo`: MongoDB job store and runtime engine
- `agenda4j-spring-boot-starter`: Spring Boot auto-configuration

Current status: **0.1.x is evolving**. API and behavior can still change before `1.0.0`.

## Modules

```xml
<dependency>
  <groupId>io.github.harutostudio</groupId>
  <artifactId>agenda4j-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

For non-Spring usage, depend on `agenda4j-core` + `agenda4j-mongo` and wire `MongoAgenda` manually.

## Spring Boot Quick Start

1. Add `agenda4j-spring-boot-starter`.
2. Provide one or more `JobHandler<?>` beans.
3. Configure `application.yml`.

### Minimal `application.yml`

```yaml
agenda:
  enabled: true
  worker-id: api-node-a
  process-every: 5s
  default-lock-lifetime: 30s
  max-concurrency: 20
  default-concurrency: 5
  lock-limit: 0
  batch-size: 5
  max-retry-count: 5
  cleanup-finished-jobs: true
  ensure-indexes-on-startup: false
```

`ensure-indexes-on-startup` is `false` by default. For production, prefer managing indexes by migration scripts.

## MongoDB Index Requirements

Collection: `scheduled_jobs`

```javascript
db.scheduled_jobs.createIndex({ nextRunAt: 1, lockUntil: 1, priority: -1 }, { name: "idx_due_claim" });
db.scheduled_jobs.createIndex({ name: 1, uniqueKey: 1 }, { name: "idx_name_uniqueKey" });
db.scheduled_jobs.createIndex(
  { name: 1 },
  { name: "ux_single_name", unique: true, partialFilterExpression: { type: "SINGLE" } }
);
```

## Build

```bash
mvn -q -pl agenda4j-core,agenda4j-mongo,agenda4j-spring-boot-starter -am compile
mvn -q test
```

## Governance and Docs

- License: Apache License 2.0 (`LICENSE`)
- Notice: `NOTICE`
- Changelog: `CHANGELOG.md`
- Release process: `RELEASE.md`
- Contribution guide: `CONTRIBUTING.txt`
- Security policy: `SECURITY.txt`
- Code of conduct: `CODE_OF_CONDUCT.txt`

## Limitations (0.1.x)

- Public API is still stabilizing.
- Mongo lock/cancel semantics are covered by integration tests but may still evolve.
- Backward compatibility is best effort until `1.0.0`.
