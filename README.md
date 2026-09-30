# 3DS Development for CLion

A CLion plugin for Nintendo 3DS homebrew projects using devkitPro, libctru, CMake, and 3dslink.

3DS projects have a **Nintendo 3DS** tab on CLion's left tool window bar. It groups the plugin's Build, Run, and Tools actions in one place; the same actions remain in their CLion menus.

## AI Usage Disclaimer

As a developer, I intend on making use of every tool available to me, as I've outgrown the elitism I once had against AI. Codex is one such tool that I made use of during the development process.
Any code written by AI was thoroughly vetted, tested, and improved by myself. By the time the project is released on JetBrains's Marketplace, all image assets will be replaced by ones created by a human.
Admittedly, I am not very good at writing documentation. This is something that AI absolutely excels in. In fact, this disclaimer section is the only documentation that wasn't written by ChatGPT. Honestly, if
you're a developer and you insist on writing long, detailed documentation yourself - all the power to you, but I'm not that type of person. This is **not** to say that I support the AI-bro's idea of "vibecoding".
You, as a developer, should always be the last line of quality control when using automated tools such as this. Test your shit, fix your shit. This isn't, and shouldn't be an entirely hands-off process.

## What it does

- **Tools > Nintendo 3DS > Install or Repair Tools** installs the devkitPro 3DS toolchain and, on Windows, a portable emulator.
- **File > New > Project > Nintendo 3DS Application** asks for the project name, Home Menu title, description, publisher, optional product code and unique ID, and graphics support. It creates a C11 CMake application template with source, icon, banner, audio, and CIA metadata.
- **Build > Build 3DS Project** installs missing 3DS tools when needed and builds in CLion's selected CMake profile directory, so **Build > Clean** removes its outputs.
- **Build > Build and Export CIA** builds the application and creates `dist/<project>.cia`, downloading makerom and bannertool on first use.
- **Run > Send CIA to 3DS** builds the CIA and offers it to `FBI Remote Install` over the local network. In FBI, choose **Remote Install > Receive URLs over the network** before running this action, then confirm the installation on the console.
- **Run > Deploy to 3DS** builds the selected profile and sends its latest `.3dsx` to Homebrew Launcher netloader through 3dslink.
- **Run > Run in Emulator** builds the selected profile and launches its latest `.3dsx`, downloading the maintained Citra fork on first use on Windows. **Tools > Nintendo 3DS > Use Existing Emulator** accepts another emulator executable.
- **Tools > Nintendo 3DS > Download Emulator Firmware** installs and opens the maintained fork. In its **File > Download System Files** menu, select a region; the emulator downloads and installs both old and new 3DS system titles into its data directory. The download currently requires that menu selection in the emulator.
- **Tools > Nintendo 3DS > Import Emulator Firmware** imports an extracted emulator data folder or ZIP containing `nand` and/or `sysdata` directly into the emulator's persistent user directory. It backs up previous data automatically. **Back Up Emulator Firmware** creates a portable copy. Close the emulator before either action. Raw `NAND.bin` images cannot be imported because the emulator does not support that format; the import needs already extracted emulator data.
- The [3DS firmware release ZIPs](https://github.com/THZoria/3DS_Firmware/releases) contain an `updates` folder intended for sysUpdater on physical hardware. The emulator firmware importer detects this layout and explains why it cannot use it as emulator data.
- The earlier RetroBIOS NAND action has been removed because its partial Citra archives cannot provide a complete firmware setup.

Downloads come from [devkitPro](https://github.com/devkitPro/installer/releases), [Project_CTR](https://github.com/3DSGuy/Project_CTR/releases), [3ds-bannertool](https://github.com/carstene1ns/3ds-bannertool/releases), and the [maintained emulator fork's releases](https://github.com/AzaharPlus/AzaharPlus/releases). The plugin checks GitHub's SHA-256 digest when the release provides one. CIA tools and the emulator are stored in CLion's per-user system directory. The devkitPro Windows installation root defaults to `C:\devkitPro`.

## Install the plugin

Build with `gradlew.bat test buildPlugin`, then install the ZIP from `build/distributions` using **Settings > Plugins > Install Plugin from Disk** in CLion. Configuration caching is enabled, so repeating the same build command can reuse its task graph.

Pushing to `main` or `master` runs the release workflow, which tests and packages the plugin and creates a GitHub release named after `version` in `gradle.properties`. The release workflow uses GitHub's built-in `GITHUB_TOKEN`. To update JetBrains Marketplace, add a repository secret named `JETBRAINS_MARKETPLACE_TOKEN` containing a JetBrains Marketplace personal access token, then run **Publish Plugin to JetBrains Marketplace** from GitHub Actions and choose the stable, beta, or alpha channel. `GITHUB_TOKEN` cannot authenticate to JetBrains Marketplace. JetBrains requires the first version of a new plugin to be uploaded manually before Gradle can publish updates. Increase the plugin version before each release or Marketplace update.

Both workflows use `gradle/actions/setup-gradle` to cache dependencies, compiled build scripts, artifact transforms, and build outputs. To also reuse the configuration cache between GitHub runs, add a repository Actions secret named `GRADLE_ENCRYPTION_KEY` containing a random 16-byte key encoded as Base64. Generate one with `openssl rand -base64 16`, or in PowerShell:

```powershell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(16))
```

Keep the same secret value across runs. Without it, dependency and build output caching still work, but the task graph is recalculated on each fresh runner. The first run warms the caches; changes to build configuration or the plugin version can require recalculation even when the caches are restored. Gradle cache usage is shown in each workflow run's job summary.

Open **Tools > Nintendo 3DS > Install or Repair Tools** to provision everything before creating a project, or use **Build 3DS Project** and **Run in Emulator** to install each tool on first use. An existing devkitPro directory can be selected with **Use Existing devkitPro**.

On Linux and macOS, the plugin installs the 3DS packages through an existing `dkp-pacman` or `pacman` installation. Automatic installation of the system package manager, CIA tools, and emulator is currently Windows only. CLion supplies CMake itself on supported installations.

For hardware deployment, open Homebrew Launcher on the console and press Y to start netloader. The `deploy` action accepts an IP address or uses network broadcast when left empty.

The generator creates applications. Sending a CIA to a physical console requires a 3DS with custom firmware and FBI on the same local network.
