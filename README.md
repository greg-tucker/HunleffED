# Hunlleff Expected Damage

A focused RuneLite expected-value companion for the Crystalline and Corrupted
Hunlleff.

This plugin is deliberately not a replacement for Gauntlet Performance
Tracker. It does not repeat the final tick-loss, wrong-prayer, wrong-style or
hazard-count report. Its result overlay is limited to analytics the existing
performance plugin does not provide:

- actual versus expected damage dealt;
- actual versus expected damage taken;
- actual kill time versus performance and no-lost-ticks expected values; and
- Hunlleff's lethal opportunities and death chance;
- optionally, actual versus clean-mechanics damage in paddlefish equivalents.

Both plugins can therefore be enabled without receiving two copies of the same
post-fight statistics.

## Features

- Live actual / expected damage totals during the fight.
- A compact expected-value result in the Gauntlet lobby.
- Separate models for normal and Corrupted Gauntlet.
- Per-attack snapshots of boosted levels, equipment, stance and prayers.
- All basic, attuned and perfected bows, staves and halberds, plus the sceptre
  and unarmed 5:1 attacks.
- Hunlleff Ranged and Magic accuracy against the player's current armour,
  Defence, Magic, defensive stance and prayers.
- Correct-prayer damage caps from the combined armour tier.
- Expected damage for stomp, tornado and damaging-floor contact.
- Separate expected kill times with the player's observed attack cadence and
  with all observed gaps removed.
- Per-attack lethal probability and overall death chance.
- Optional 20-HP paddlefish-equivalent analysis for actual, clean-mechanics and
  extra damage.
- Optional local chat summary. No data is uploaded or written to fight-history
  files.

## Damage expectation

For a standard attack:

```text
expected damage = chance to hit x average successful hit
                = chance to hit x (maximum hit / 2)
```

The normal OSRS accuracy roll is used:

```text
if attack roll > defence roll:
    P(hit) = 1 - (defence roll + 2) / (2 x (attack roll + 1))
otherwise:
    P(hit) = attack roll / (2 x (defence roll + 1))
```

Player attack rolls use the boosted combat level, offensive prayer, stance and
exact bonuses of the equipped Gauntlet gear. Hunlleff's defence is level 240
with +20 defence bonuses. Powered staves use their fixed 23 / 31 / 39 base
maximum hits and current Magic-prayer damage bonuses.

Incoming rolls use Hunlleff's style and difficulty-specific attack bonus
against the player's current Ranged or Magic defence. Magic defence uses the
standard 70% Magic / 30% Defence effective level. Correct protection prayer
and armour tier sum select the applicable damage cap.

Stomps use the complete integer damage distribution from zero through the
difficulty's unprotected maximum hit. Tornado and floor expectation uses the
midpoint of the documented inclusive range. This keeps actual and expected
damage taken like-for-like.

## Kill-time expected values

The result deliberately separates three different measurements:

- `Actual` is the real encounter duration, including the real hit RNG and every
  delay.
- `Performance EV` is the expected kill time for the attack setup and
  inter-attack delays observed in that fight. It answers how long that level of
  execution would take with average RNG.
- `No-loss EV` uses the same observed attack setup but removes the inter-attack
  delays. It answers how long the same choices would take with average RNG and
  every attack made as soon as its weapon cooldown permits.

Neither expected value is a theoretical perfect kill. Suboptimal weapons,
incorrect styles, prayer choices and the observed weapon mix remain part of
both models. A perfect benchmark would instead need to prescribe an optimal
strategy.

These benchmarks are not rough `boss HP / average DPS` divisions.

Each modelled player attack is stored with its chance to hit, maximum hit and
weapon speed. At the end of the encounter, the observed attack sequence is
repeated as a cycle against 600 or 1,000 HP with:

- every attack made as soon as its weapon cooldown permits;
- a four-tick allowance before the first attack, matching the encounter's
  existing no-tick-loss baseline;
- misses and accurate zero-damage rolls included;
- every integer damage roll from zero through maximum hit included;
- protection-prayer blocks included; and
- overkill handled by the remaining-HP probability distribution.

The no-loss result is the average duration of that exact attack mix with no
idle, movement or eating gaps. The performance result preserves the observed
first-attack time and gaps between attacks. If alternate RNG requires more
attacks than occurred in the real fight, the observed attack sequence and
cadence repeat as a representative cycle. Changing weapon tiers, prayers,
levels, styles, attack delays or the ratio of weapon types changes the relevant
benchmark. The actual time is only shown for a detected Hunlleff kill; an
abandoned or failed encounter is shown as `DNF`.

## Lethal opportunities and death chance

A lethal opportunity is a Hunlleff standard attack or stomp whose maximum hit
could kill the player at the Hitpoints they had when that attack began.
Tornadoes and damaging floor are intentionally excluded: the metric describes
Hunlleff's direct attack opportunities, like the KO-chance statistic in a PvP
performance review.

For a normal attack:

```text
P(lethal) = P(hit) x (max hit - current HP + 1) / (max hit + 1)
```

The second factor counts the inclusive integer damage rolls that would be
lethal. Stomps use the same damage-roll calculation with 100% accuracy. The
displayed death chance is:

```text
P(at least one lethal attack) = 1 - product(1 - P(lethal attack))
```

This is an observed-opportunity metric: it uses the HP, gear and prayer state
seen before each real attack. It does not invent an alternative fight timeline
where earlier hypothetical damage changes later HP or eating decisions.

## Optional fish analysis

Enable `Show fish analysis` to add a post-fight supply estimate. It reports:

```text
actual fish equivalent =
    max(0, actual damage + ending HP - starting HP) / 20
clean-mechanics EV =
    max(0, clean EV damage + ending HP - starting HP) / 20
observed-mechanics EV =
    max(0, observed-state EV damage + ending HP - starting HP) / 20
healing required by mistakes =
    max(0, observed-mechanics EV - clean-mechanics EV)
```

These are 20-HP paddlefish equivalents: the amount of standard paddlefish
healing required to reconcile the damage taken with the player's observed
starting and ending Hitpoints. Unused starting health therefore absorbs damage
before any fish-equivalent healing is required. Fractional values are retained
because this is a healing-load comparison, not a count of whole food items.
The overlay shows the captured start and end HP alongside the result so the
inputs can be checked.
The actual figure is based on every real point of damage taken. It is not a
count of food clicks or a claim about the minimum food required to survive.
Overhealing and passive Hitpoints regeneration are not modelled separately.

The clean-mechanics EV uses the same observed Hunlleff Ranged and Magic
attacks, gear, stats and defensive bonuses, but models the correct protection
prayer on each attack. Stomp, tornado and damaging-floor damage is excluded
because it is avoidable. It retains the number of standard attacks observed in
the real fight, including any additional attacks caused by a longer encounter.
This is an expected value rather than a fictional exact counterfactual damage
roll.

`Healing required by mistakes` does not subtract the clean EV directly from
the actual result, because that would mix mistakes with favourable or
unfavourable damage RNG. Instead, it compares two expected values: expected
healing with the observed prayer and hazard exposure versus expected healing
with correct protection prayers and no stomp, tornado or floor exposure.

Crystallised and corrupted paddlefish heal 16 HP and can be combo-eaten, so
they are deliberately not treated as one standard 20-HP fish in this estimate.

## Interpretation and edge cases

- For damage dealt, a positive difference is favourable. For damage taken, a
  negative difference is favourable. Comparing actual time with Performance EV
  primarily shows hit RNG; comparing Performance EV with No-loss EV isolates
  the cost of observed attack delays.
- Expected values describe a probability distribution, not what the next hit
  should be. Short fights can legitimately vary substantially.
- Vengeance and other non-weapon damage creates a real outgoing hitsplat and is
  included in actual damage. It has no weapon-roll expectation, so it increases
  the displayed damage difference.
- Unsupported player attacks produce a visible `Damage model coverage` warning
  instead of receiving a fabricated expectation.
- Formula constants are self-contained and unit tested. They should be
  reviewed if Jagex changes equipment, prayers or Hunlleff mechanics.

The armour caps and hazard ranges are documented on the
[OSRS Wiki Gauntlet page](https://oldschool.runescape.wiki/w/The_Gauntlet).
The combat formula follows the open-source
[OSRS Wiki DPS Calculator](https://github.com/weirdgloop/osrs-dps-calc).

## Run locally

### Requirements

- A JDK. Java 11 is the Plugin Hub target.
- Git, if cloning rather than downloading the source archive.

The Gradle wrapper is included, so a separate Gradle installation is not
needed.

### Windows

```powershell
cd C:\Users\grego\Documents\HunleffED
.\gradlew.bat test
.\gradlew.bat run
```

### macOS or Linux

```bash
./gradlew test
./gradlew run
```

The `run` task launches a RuneLite developer client with the plugin sideloaded.
Search the plugin configuration for `Hunlleff Expected Damage` and enable it.

For a Jagex Account:

1. Run `RuneLite (configure)` from the Windows Start menu.
2. Add `--insecure-write-credentials` to `Client arguments` and save.
3. Launch RuneLite once through the Jagex Launcher, then close it.
4. Run the Gradle `run` task above.
5. After testing, remove the argument and delete
   `.runelite/credentials.properties`.

See RuneLite's
[Jagex Account development guide](https://github.com/runelite/wiki/blob/master/Using-Jagex-Accounts.md).
The credentials file is a secret and must never be shared or committed.

## Suggested test

1. Complete one normal and one Corrupted Gauntlet.
2. Use two weapon styles so both expected values sample a realistic attack mix.
3. At low HP, allow one correctly prayed Hunlleff attack whose maximum hit can
   kill you, then eat. Confirm one lethal opportunity and a non-zero percentage.
4. Deliberately touch a tornado or floor tile. Damage expectation should change,
   but direct lethal opportunities should not.
5. Confirm the lobby result shows separate `Actual`, `Performance EV` and
   `No-loss EV` kill-time lines, followed by lethal opportunities, death
   chance, and any coverage warning.
6. Enable `Show fish analysis`. Note starting HP, ending HP and total damage;
   confirm the actual value matches
   `max(0, damage + ending HP - starting HP) / 20`. Deliberately take avoidable
   damage and confirm it increases `Healing required by mistakes`, but not
   `Expected with clean play`.

## Build and verify

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
```

The plugin JAR is written under `build/libs`. Use the `run` task for local
testing rather than copying the JAR into an installed RuneLite client.

## Plugin Hub notes

The repository uses Java 11, `build=standard`, a single public plugin entry
point, BSD 2-Clause licensing and no external services or runtime dependencies.
See the current
[RuneLite Plugin Hub guide](https://github.com/runelite/plugin-hub/blob/master/README.md)
before submitting.

RuneLite recommends contributing fitting features to existing plugins to avoid
fragmentation. Since this is complementary expected-value analysis for an
already-listed tracker, integration into Gauntlet Performance Tracker may be a
smoother review route than a second listing.

## Acknowledgements

Encounter lifecycle concepts are based on
[Gauntlet Performance Tracker](https://github.com/powerus117/RLCGPerformanceTracker).
The expected-value and lethal-opportunity presentation was inspired by
[PvP Performance Tracker](https://github.com/Matsyir/pvp-performance-tracker).

Licensed under the [BSD 2-Clause License](LICENSE).
