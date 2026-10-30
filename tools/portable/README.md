# Legacy lossless portable templates

[Tools index](../README.md) · [Current portable guide](../../docs/portable-windows.md) · [Root README](../../README.md)

These files support [build-portable.ps1](../../build-portable.ps1), the earlier jpackage/payload layout. The current preferred package is produced by build-direct-portable.ps1 and uses src/ at the package root; do not mix its commands with this layout.

The legacy builder creates an outer launcher directory and prepares a DodgingCat/ application with source under DodgingCat/source/. PNG/WAV assets may be packed losslessly and restored by PortableAssets, with dimension/pixel or file hashes checked. MP3 stays unchanged. Run.cmd prepares the payload/runtime; Open-source.cmd opens the source; Troubleshoot.cmd restores/checks resources. Build.cmd/Compile.cmd use the included compiler in the buildable edition. The -DemoOnly edition omits the compiler/Build.cmd while retaining source and tests.

Resolve-root.ps1 handles Unicode-path runtime caching; Sync-assets.ps1 shares resources with source. README.txt and README-demo.txt are player instructions copied by the legacy builder. They are not instructions for launching scripts directly from tools/portable. The current package's single-file LMS workflow and exact requirements are documented in the portable guide.
