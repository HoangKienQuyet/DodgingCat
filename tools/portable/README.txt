DODGING CAT - WINDOWS X64 PORTABLE

1. Extract the entire outer ZIP to a writable folder.
2. Double-click Run.cmd. No installed Java, JDK, Maven, 7-Zip or Internet is needed.
3. First launch prepares the bundled runtime, full-resolution PNGs and original WAV/MP3 audio.
   Wait for preparation to finish. Later launches reuse these files.
4. Use Run.cmd for normal launches, including folders with Vietnamese names.

Do not run from inside the ZIP viewer. Keep the entire extracted folder together.
The executable shares assets with the included source/resources folder to avoid
shipping two copies. Do not delete source or runtime folders.

F11: fullscreen/windowed. F10: FPS display.
F9: developer money; pressing F9 again resets THIS portable profile completely.
Save data: DodgingCat/data/profile.json (beside this copy of the application).

CODE / INTERVIEW
Open-source.cmd prepares the files and opens the complete source tree.
Close the game, edit source/src/main/java, then run Build.cmd to compile using the
included Java 25 compiler. Neither Maven nor a system JDK is needed for this build.
The ordinary Maven/JUnit project is also included for the development team.

Troubleshoot.cmd checks/restores assets and saves startup errors to game-debug.log.
The first preparation log is DodgingCat/prepare-assets.log; with a Unicode path,
look under the cache directory recorded in portable-root.txt instead.
For Unicode folder names, Windows Java native libraries require an ASCII path.
Run.cmd prepares a local runtime cache under LocalAppData/DodgingCatPortableRuntime
(or Public/DodgingCatPortableRuntime). Source edits are synchronized before a build;
save data still stays beside the original portable folder. An English/ASCII path
avoids the extra cache and uses less disk space. Allow about 700 MB of free disk
space when using Unicode paths (about 400 MB for an ASCII path).

TARGET: 64-bit Windows. This package does not run on Linux or macOS.
No installer, registry changes, services, external downloads or admin rights are
used by the launch scripts. School policy may still restrict running executables.

SUBMISSION
This contains source and a runnable portable game. The final report and slides
must still be added to the one-file LMS submission and checked against its limit.
See the external build-report.json for the exact portable ZIP size/hash.
