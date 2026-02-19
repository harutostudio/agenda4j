# Release Guide

This document defines a repeatable release flow for `agenda4j`.

## Prerequisites

- JDK 21
- Maven 3.9+
- Access to target package registry (Maven Central or GitHub Packages)
- Required credentials configured in `~/.m2/settings.xml`

## Pre-release checks

1. Run compile:
   - `mvn -q -pl agenda4j-core,agenda4j-mongo,agenda4j-spring-boot-starter -am compile`
2. Run tests:
   - `mvn -q test`
3. Verify changelog entry in `CHANGELOG.md`.
4. Confirm README version snippets.

## Versioning

- `0.1.x`: evolving API, no strict backward-compatibility guarantees.
- `1.0.0+`: semantic versioning with backward compatibility guarantees.

## Release steps

1. Update root and module versions from `x.y.z-SNAPSHOT` to `x.y.z`.
2. Commit version bump and changelog updates.
3. Create git tag: `vX.Y.Z`.
4. Publish artifacts using your target registry profile, for example:
   - `mvn -Prelease -DskipTests deploy`
5. Restore next development version: `x.y.(z+1)-SNAPSHOT`.

## Post-release

1. Verify artifacts are resolvable from target registry.
2. Publish release notes based on `CHANGELOG.md`.
3. Announce compatibility notes (especially for 0.1.x).
