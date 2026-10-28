DODGING CAT - DIRECT SOURCE PORTABLE (WINDOWS X64)

Extract the entire ZIP to a writable folder. The src folder and pom.xml are
visible immediately. Double-click Run.cmd to play. On the first launch, the
bundled Java 25 runtime and JavaFX files are unpacked inside support/. No installed
Java, Maven, Internet connection, administrator access or setup wizard is
needed. Do not launch from inside the ZIP viewer.

To edit code during the interview, open src/main/java, save the change, close
the game, run Build.cmd, then Run.cmd. Build.cmd uses the bundled Java compiler.
The normal Maven project and tests remain in src and pom.xml; running JUnit
tests with Maven requires a separately installed Maven/JUnit environment.

All original images and audio are included directly in src/main/resources.
They are not downscaled or transcoded by this package. Data is saved beside
this copy under data/profile.json. F11 toggles fullscreen; F10 shows FPS.
F9 grants dev money; pressing F9 again resets this portable profile.

If the extraction path contains Vietnamese or other non-ASCII characters,
Run.cmd copies the native Java runtime and source to a local ASCII path for
Windows compatibility. Edits are synchronized before each build. Keep at least
500 MB of free disk space. No files are installed system-wide.

Run.cmd and Build.cmd are the two actions for the demo. src/ and pom.xml are
the complete Maven project. support/ contains the runtime and internal helper
files; docs/ contains project documentation. Do not remove these folders.

The ZIP intentionally omits target, .git, IDE caches and prior deliverables.
The final LMS submission must also contain the report and slides in its one
allowed file and remain below the 100 MB upload limit.
