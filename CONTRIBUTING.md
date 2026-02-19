# Contributing to agenda4j

Thanks for contributing.

## Development setup

1. Use JDK 21 and Maven 3.9+.
2. Build modules:
   - `mvn -q -pl agenda4j-core,agenda4j-mongo,agenda4j-spring-boot-starter -am compile`
3. Run tests:
   - `mvn -q test`

## Pull request rules

1. Keep PRs focused and small.
2. Add or update tests for behavior changes.
3. Update docs when user-facing behavior changes.
4. Keep backward compatibility in mind; `0.1.x` is evolving but breaking changes must be documented.

## Commit and changelog

1. Use clear commit messages.
2. For release-impacting changes, add a short entry to `CHANGELOG.md`.

## Reporting issues

Please include:
- Reproduction steps
- Expected vs actual behavior
- Java version, MongoDB version, and framework version
- Logs or stack trace if available

## Code style

- Prefer clear, explicit naming.
- Keep public APIs minimal and stable.
- Avoid hidden behavior; document scheduling and lock semantics clearly.
