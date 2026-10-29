# Third-party software and reference

Game graphics are drawn in Java code. No Poor Bunny sprites, music, or other game assets are included.
Gameplay inspiration: [Poor Bunny! by Adventure Islands](https://play.google.com/store/apps/details?id=com.adventureislands.poorbunny).

Build dependencies and portable runtime:

| Component | Version | License / source |
| --- | --- | --- |
| OpenJFX | 25 | GPLv2 with Classpath Exception; [OpenJFX source](https://github.com/openjdk/jfx/tree/jfx25) |
| Gson | 2.13.2 | Apache 2.0; [Gson source and license](https://github.com/google/gson/tree/gson-parent-2.13.2) |
| JUnit Jupiter (tests only) | 5.13.4 | EPL 2.0; [JUnit source](https://github.com/junit-team/junit5/tree/r5.13.4) |
| Apache Maven (build only) | 3.9.11 | Apache 2.0; [Maven](https://maven.apache.org/) |

The local portable build contains a reduced Oracle JDK 23.0.2 runtime made by `jlink` from the JDK installed on this machine. Its original license notices are retained in `runtime/legal/`. The runtime's applicable distribution terms are documented by [Oracle's JDK licensing page](https://www.oracle.com/java/technologies/javase/jdk-faqs.html). Rebuilding with an appropriately licensed OpenJDK distribution is supported via `JAVA_HOME`.

JavaFX platform JARs also retain their bundled notices and native-library licenses. The portable package keeps these JARs intact. For a different OS/architecture, build from source on that platform; the Windows package cannot run there.
