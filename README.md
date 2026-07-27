# Hunlleff Expected Damage

A RuneLite Plugin Hub-style plugin for the Crystalline and Corrupted
Hunlleff. It keeps the useful fight review from Gauntlet Performance Tracker
and adds live and post-fight **actual versus expected damage** in both
directions.

The overlay answers two different questions:

- **To Hunlleff:** did your weapon attacks deal more or less damage than their
  mathematical expectation?
- **To you:** did you take more or less damage than the mathematical
  expectation of Hunlleff's attacks and the hazards you touched?

A positive difference is good on the `To Hunlleff` line. A negative difference
is good on the `To you` line. The colors follow that convention.

## Features

- Live actual / expected / difference totals during the fight.
- Post-fight results in the Gauntlet lobby.
- Separate expected-damage models for normal and Corrupted Gauntlet.
- Per-attack snapshots of boosted levels, equipment, attack style and prayers.
- All basic, attuned and perfected bows, staves and halberds, plus the sceptre
  and unarmed 5:1 attacks.
- Hunlleff Ranged and Magic accuracy against the player's current armour,
  Defence, Magic, defensive stance and prayers.
- Correct-prayer damage caps from the combined armour tier.
- Expected damage for stomp, tornado and damaging-floor contact.
- Tick use, lost ticks, wrong styles, wrong prayers and avoidable-hit counts.
- Optional local chat summary. The plugin sends no data over the network and
  does not write fight history to disk.

## What “expected” means

For a standard attack:

```text
expected damage = chance to hit × average successful hit
                = chance to hit × (maximum hit / 2)
```

The normal OSRS accuracy roll is then used:

```text
if attack roll > defence roll:
    P(hit) = 1 - (defence roll + 2) / (2 × (attack roll + 1))
otherwise:
    P(hit) = attack roll / (2 × (defence roll + 1))
```

Player attack rolls use the boosted combat level, current offensive prayer,
stance and the exact offensive bonuses of the equipped Gauntlet gear.
Hunlleff's defence is level 240 with +20 defence bonuses. Powered staves use
their fixed 23 / 31 / 39 base maximum hits and current Magic-prayer damage
bonuses.

Incoming rolls use the Hunlleff's style and difficulty-specific attack bonus,
then roll against the player's current Ranged or Magic defence. The Magic
defence level is the standard 70% Magic / 30% Defence combination. Correct
protection prayer and the armour tier sum select the relevant Hunlleff damage
cap.

Stomps use half of the difficulty's unprotected maximum hit. Tornado and floor
expectations use the midpoint of their documented inclusive damage ranges.
This makes the `To you` comparison like-for-like: actual damage includes those
hazards, so expected damage does too.

Expected damage is an average over many attacks, not a prediction for the next
hit. A short fight can legitimately finish well above or below expectation.

### Scope and edge cases

- Reflected or otherwise non-weapon damage, such as Vengeance, is included in
  actual damage dealt because it produces a real hitsplat. It is not assigned
  an attack-roll expectation, so it will increase the displayed difference.
- If RuneLite observes an unsupported player attack, the detailed result shows
  `Damage model coverage` instead of silently inventing an expectation.
- The calculation constants are intentionally self-contained and covered by
  unit tests. They should be reviewed when Jagex changes Gauntlet equipment,
  prayers or Hunlleff mechanics.

The armour caps and hazard ranges are documented on the
[OSRS Wiki Gauntlet page](https://oldschool.runescape.wiki/w/The_Gauntlet).
The combat formula implementation follows the open-source
[OSRS Wiki DPS Calculator](https://github.com/weirdgloop/osrs-dps-calc).

## Run locally

### Requirements

- A 64-bit JDK. Java 11 is the Plugin Hub target and is the safest choice.
- Git, if cloning rather than downloading the source archive.

The Gradle wrapper is included, so a separate Gradle installation is not
needed.

### Windows

Open PowerShell in the repository root:

```powershell
.\gradlew.bat test
.\gradlew.bat run
```

### macOS or Linux

Open a terminal in the repository root:

```bash
./gradlew test
./gradlew run
```

The second command launches a RuneLite developer client and sideloads
`Hunlleff Expected Damage`. Log in, open RuneLite's plugin configuration,
search for that name, and make sure it is enabled. Enter either Gauntlet and
the overlay will appear when the Hunlleff encounter begins.

To test the key paths:

1. Do one normal and one Corrupted Gauntlet.
2. Use at least two weapon styles and switch prayers normally.
3. Deliberately take one floor or tornado hit so its expected value is included.
4. After the encounter, check that the result remains visible in the lobby.
5. Enable `Chat summary` and verify that the message is local game chat only.

For a Jagex Account, first follow RuneLite's
[development login guide](https://github.com/runelite/wiki/blob/master/Using-Jagex-Accounts.md).
Treat the generated `credentials.properties` as a secret and never commit or
share it.

## Build and verify

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
```

The first command runs the deterministic combat-math tests. The second also
creates the normal plugin JAR under `build/libs`. Local testing should use the
`run` task rather than copying that JAR into an installed RuneLite client.

## Plugin Hub submission

This repository already follows the current standard Plugin Hub layout:

- Java 11 bytecode
- `runelite-plugin.properties` with `build=standard`
- one public `Plugin` entry point
- Gradle wrapper and a developer-client `run` task
- no reflection, subprocesses, runtime downloads or external services
- BSD 2-Clause licensing

The normal submission process is:

1. Publish this repository publicly and push a tested commit.
2. Fork `runelite/plugin-hub`.
3. Add a manifest file under `plugins/` containing the repository URL and
   tested commit hash.
4. Run the Plugin Hub verification task described in its README.
5. Open a pull request.

See the current
[RuneLite Plugin Hub submission guide](https://github.com/runelite/plugin-hub/blob/master/README.md)
before submitting. RuneLite currently lists new high-end PvM boss plugins
among features that may be rejected, so contributing the expected-damage
feature to the already-listed Gauntlet Performance Tracker may be the easier
review route. This standalone project remains suitable for local testing and
for proposing that integration.

## Acknowledgements

Fight lifecycle, tick-use and review concepts are based on
[Gauntlet Performance Tracker](https://github.com/powerus117/RLCGPerformanceTracker).
The actual-versus-expected presentation was inspired by
[PvP Performance Tracker](https://github.com/Matsyir/pvp-performance-tracker).

Licensed under the [BSD 2-Clause License](LICENSE).
