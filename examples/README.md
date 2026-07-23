# Examples

Standalone Gradle projects demonstrating `io.github.malczuuu.nullmarked`. Each has its own Gradle wrapper and can be
copied out and run on its own.

## NullMarked & Kotlin interop example

Shows a Java API annotated via the plugin interoping with Kotlin: `@NullMarked` packages give Kotlin real
`String`/`String?` types instead of platform types, so a nullness bug that would otherwise only surface as a runtime
`NullPointerException` gets caught by the Kotlin compiler instead. See
[`example-kotlin-interop/README.md`](example-kotlin-interop/README.md).

## NullMarked & NullAway example

Pairs the plugin with [NullAway](https://github.com/uber/NullAway) and
[Error Prone](https://errorprone.info) to enforce nullness at compile time, and shows how excluding a legacy package
from generation keeps it out of that enforcement. See
[`example-nullmarked-nullaway/README.md`](example-nullmarked-nullaway/README.md).
