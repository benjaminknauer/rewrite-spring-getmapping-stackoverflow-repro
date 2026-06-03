# Repro: StackOverflowError when parsing Spring `@GetMapping` import (rewrite 8.83.6+)

Upstream issue: [openrewrite/rewrite#7889](https://github.com/openrewrite/rewrite/issues/7889)

Minimal Maven project that triggers a `JavaParsingException` / `StackOverflowError`
inside `ReloadableJava{21,25}TypeMapping` when OpenRewrite parses a single Java
file that imports `org.springframework.web.bind.annotation.GetMapping`.

The error did not occur with rewrite `8.83.5`. It surfaced after
commit [`3e6781308`](https://github.com/openrewrite/rewrite/commit/3e6781308)
("Replace shallow class mapping in parsers with proper type attribution",
PR #7861), which shipped in `8.83.6`.

## Reproduce

Requirements:

- JDK 21 or 25 (the plugin's `rewrite-java-21` and `rewrite-java-25` artifacts
  are both pinned to `${rewrite.version}`, so the repro works regardless of
  which JVM Maven runs on)
- Maven 3.9+
- Network access to Maven Central (resolves `spring-web:6.2.11` and its transitives)

```bash
mvn rewrite:run
```

The failing frames are `ReloadableJava21*` on JDK 21 and `ReloadableJava25*`
on JDK 25; the recursion pattern is identical.

Expected build log excerpt:

```
[WARNING] There were problems parsing src/main/java/com/example/Controller.java
[WARNING] java.lang.StackOverflowError: null
  org.openrewrite.java.isolated.ReloadableJava{21,25}TypeMapping.methodDeclarationType(...)
  org.openrewrite.java.isolated.ReloadableJava{21,25}TypeMapping.annotationType(...)
  org.openrewrite.java.isolated.ReloadableJava{21,25}TypeMapping.listAnnotations(...)
  org.openrewrite.java.isolated.ReloadableJava{21,25}TypeMapping.lambda$methodDeclarationType$10(...)
  org.openrewrite.java.internal.DefaultJavaTypeFactory.methodFor(...)
  ...

org.openrewrite.java.JavaParsingException: Failed to convert for the following cursor stack:--- BEGIN PATH ---
JCCompilationUnit(sourceFile = .../Controller.java)
JCImport(line = 3)
--- END PATH ---
```

Setting `<rewrite.version>8.83.5</rewrite.version>` in `pom.xml` makes the error
disappear.

## Why it depends on the full Maven classpath

The recursion is only triggered when the parser can resolve Spring's meta
annotations (in particular `@AliasFor` from `spring-core`). The transitive
classpath produced by Maven is what makes those annotations visible:

```
spring-web, spring-beans, spring-core, spring-jcl,
micrometer-observation, micrometer-commons
```

A parser test wired only against `spring-web.jar` does not reproduce it.
