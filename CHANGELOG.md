<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# CLion3DS Changelog

## [Unreleased]

- Switched the managed Windows emulator to the maintained Citra fork and added an action that opens its built-in firmware downloader. Removed the partial GitHub NAND fetch action.
- Detect physical-console firmware update ZIPs and explain their incompatibility with the emulator importer.
- Firmware import accepts emulator data folders and ZIPs and preserves the previous emulator data.
- Added emulator firmware setup, import, and backup actions. Imports preserve the prior firmware in persistent per-user storage and reject raw NAND images.
- Build, CIA export, deploy, and emulator actions now use CLion's selected CMake profile directory; deploy and emulator actions build before launch. Generated CIA files are included in CMake clean.
- Keep the Nintendo 3DS tool window action groups compact at the top of the panel.
- Added a Nintendo 3DS tool window tab with the existing Build, Run, and Tools actions.
- Moved 3DS build commands to Build and device/emulator commands to Run; removed the duplicate Tools menu project creator.
- Added a Nintendo 3DS Application template to CLion's New Project wizard.
- The template generates a CMake project with optional Citro2D or Citro3D linkage.
- New projects use C11 and GNU extensions, so CLion indexes the devkitPro headers as C code.
- The New Project wizard now accepts a project name, Home Menu title, description, publisher, product code, and unique ID.
- The project name updates the New Project location, so the generated folder uses that name.
