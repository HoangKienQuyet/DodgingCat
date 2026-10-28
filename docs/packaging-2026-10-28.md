# Baseline and submission packaging — 2026-10-28

The original local workspace was backed up before packaging under
`deliverables/baseline-20261028-161101/`. The full workspace archive is for local
recovery only. It includes Git metadata and build/profiling files and must not be
uploaded as the source submission. `baseline-manifest.json` records SHA-256 for
the original 183 source/resource/documentation files; `SHA256SUMS.txt` identifies
the original backup ZIPs. These are snapshots made on 28 October, not a claim
that Git development history existed earlier.

The LMS limit supplied by the student is one file, 100 MB. The final outer ZIP
must contain the project, final report, and presentation slides. A source-only
ZIP is a preparation artifact, not a complete final submission. Use a conservative
100,000,000-byte outer limit until the LMS confirms its precise unit.

The source package includes `src/` (including every active resource and tests),
`pom.xml`, run/test scripts, README files, `docs/`, and the small art-generation
tools. It excludes `.git/`, `target/`, IDE/cache folders, temporary files, backup
archives, and generated delivery folders. Runtime/dependency requirements remain
JDK 25, Maven and the dependencies in `pom.xml`; this is not a portable runtime.
The package must still be tested on the university's required environment.

PNG compression used [Oxipng 10.2.1](https://github.com/oxipng/oxipng), a build-time
utility, with `-o 3 --threads 2 -r`. It adds no runtime dependency. No resizing,
palette quantization or `--alpha` transformation was requested. All 62 original
and optimized PNGs were decoded using Java ImageIO and compared row by row:
87,560,914 pixels had identical dimensions and ARGB values, including transparent
pixels. Original artwork and its original metadata remain in the baseline backup;
existing image-generation/source attribution documentation is retained.

Five unused `backgrounds/*_wide.png` variants were removed. Production rendering
already exclusively used `<map>.png` with a floor-aligned crop. The redundant
preload was removed and the historical opt-in performance probe now uses the
same Kitchen source as the game. No active map source, character sheet, font,
button, audio file, sound duration, or gameplay rule was removed or resampled.

Reserve room in the final ZIP for the report and slides. Do not include raw
profiling dumps or every full-resolution test screenshot. Evidence selected for
the report should remain legible and record the actual test environment/results.
Do not submit an archive containing only a JAR or only a GitHub link in place of
the complete source project required by the assignment.
