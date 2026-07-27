# Hunllef Expected Damage

A RuneLite plugin for reviewing damage RNG and execution during the Crystalline
and Corrupted Hunllef fights.

It complements Gauntlet Performance Tracker by focusing on expected-value
statistics instead of repeating its tick-loss, wrong-prayer, wrong-style and
hazard-count report.

## Features

- Actual versus expected damage dealt and taken.
- Live fight totals and a post-fight lobby summary.
- Separate calculations for the normal and Corrupted Gauntlet.
- Per-attack snapshots of equipment, boosted levels, prayers and combat stance.
- Support for every Gauntlet weapon tier, the sceptre and unarmed 5:1 attacks.
- Actual, Performance EV and No-loss EV kill times.
- Lethal opportunities and death chance.
- Optional paddlefish-equivalent healing analysis.
- Optional local chat summary.

No fight data is uploaded or written to history files.

## Metrics

### Damage

Expected damage uses the standard OSRS accuracy formula and the player's state
when each attack starts. Player successful hits use the current minimum-damage
rule, where a successful zero roll is raised to one. Hunllef attacks retain the
NPC damage range from zero through maximum hit.

Outgoing calculations include the equipped weapon, boosted combat levels,
stance and offensive prayers. Incoming calculations include the player's
Defence and Magic levels, armour bonuses, defensive stance, prayers and the
combined tier of the equipped Gauntlet armour.

Correct protection prayers and armour reduce Hunllef's maximum hit. Armour also
reduces the expected damage from tornadoes. Stomps and damaging floor tiles use
their unmitigated damage ranges.

### Kill time

- `Actual` is the recorded duration of a successful fight.
- `Performance EV` applies average hit RNG to the attacks and delays observed
  during that fight.
- `No-loss EV` uses the same attack setup and weapon mix but removes gaps
  between attacks.

The expected values repeat the observed attack sequence if more attacks are
needed to cover 600 or 1,000 HP. They account for misses, blocked attacks,
minimum player damage, overkill and weapon speed. Neither expected value is an
optimal-gear or perfect-strategy benchmark.

### Lethal opportunities

A lethal opportunity is a Hunllef attack or stomp whose maximum hit could kill
the player at their recorded HP when the attack started. Death chance combines
the lethal-roll probability of those opportunities:

```text
death chance = 1 - product(1 - lethal chance of each opportunity)
```

Tornadoes and floor damage are excluded from this metric.

### Fish analysis

Enable `Show fish analysis` to display:

- `Needed this fight`: actual healing used, adjusted for starting and ending HP.
- `Expected without avoidable hits`: expected healing with correct protection
  prayers and no stomp, tornado or floor damage.
- `Healing from avoidable hits`: the expected difference between the observed
  mechanics and the result without avoidable hits.

Values are expressed in standard 20-HP paddlefish equivalents:

```text
fish equivalent = max(0, damage + ending HP - starting HP) / 20
```

The calculation ignores overhealing and passive regeneration. It retains the
number of standard Hunllef attacks observed in the real fight, so “without
avoidable hits” is not a no-tick-loss incoming-damage simulation.

## Run locally

### Requirements

- Java 11
- Git, if cloning the repository

The Gradle wrapper is included.

On Windows:

```powershell
cd C:\path\to\hunllef-expected-damage
.\gradlew.bat test
.\gradlew.bat run
```

On macOS or Linux:

```bash
./gradlew test
./gradlew run
```

The `run` task opens a RuneLite developer client with the plugin sideloaded.
Enable `Hunllef Expected Damage` in the plugin configuration.

### Jagex Accounts

To use the developer client with a Jagex Account:

1. Open `RuneLite (configure)` from the Windows Start menu.
2. Add `--insecure-write-credentials` to `Client arguments`.
3. Launch RuneLite once through the Jagex Launcher, then close it.
4. Run `.\gradlew.bat run`.
5. After testing, remove the argument and delete
   `.runelite/credentials.properties`.

The credentials file is a secret and must not be shared or committed. More
detail is available in RuneLite's
[Jagex Account development guide](https://github.com/runelite/wiki/blob/master/Using-Jagex-Accounts.md).

## Build

```powershell
.\gradlew.bat clean test build
```

The plugin JAR is created in `build/libs`.

## References

Armour caps and hazard ranges come from the
[OSRS Wiki Gauntlet page](https://oldschool.runescape.wiki/w/The_Gauntlet).
Combat formulas follow the open-source
[OSRS Wiki DPS Calculator](https://github.com/weirdgloop/osrs-dps-calc).

Encounter lifecycle concepts are based on
[Gauntlet Performance Tracker](https://github.com/powerus117/RLCGPerformanceTracker).
The expected-value presentation was inspired by
[PvP Performance Tracker](https://github.com/Matsyir/pvp-performance-tracker).

Licensed under the [BSD 2-Clause License](LICENSE).
