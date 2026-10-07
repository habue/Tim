# Tim — Trail Investigator Module

Meteor Client addon for **Minecraft 26.1.2**, ported from rithsgit's 1.21.4 addon.
The port is maintained on the `meteor-26.1.2` branch. Original modules and HUD elements are preserved.

## Installation

1. Use Minecraft **26.1.2**, Java **25**, Fabric Loader **0.19.2 or newer**, and Meteor Client for **26.1.2**.
2. Download `Tim-26.1.2` from a successful [GitHub Actions build](https://github.com/habue/Tim/actions/workflows/build.yml).
3. Extract the ZIP and put `Tim-1.0-26.1.2.jar` in your `mods` directory.
4. Enable modules in the **Tim** category and add HUD elements from the **Tim** group.

Features that use Baritone require the compatible Baritone for Minecraft 26.1. Install it alongside Meteor if it is not already included in your setup.

## Building

With JDK 25 installed:

```sh
./gradlew build
```

The installable JAR is written to `build/libs/`. GitHub Actions builds every push to
`meteor-26.1.2`, pull requests, and manual runs, then uploads the JAR as an artifact.
Meteor, Minecraft and Baritone are compile dependencies; they are not bundled in the addon.

## License

CC0-1.0, as declared by the upstream repository's `LICENSE`.
