![Dark Doppelganger](https://imgur.com/xTB0ZXo.png)

# Dark Doppelganger

A multi-phase boss fight for Minecraft, built on [Iron's Spells 'n Spellbooks](https://modrinth.com/mod/irons-spells-n-spellbooks).

The Dark Doppelganger is a spellcaster that copies whoever summons it: their look, their gear and their weapon. It calls in minions, adapts to how you fight and gets more dangerous as the fight goes on. It's meant for late-game players, alone or in a group.

**Download:** [Modrinth](https://modrinth.com/mod/dark-doppelganger) · [CurseForge](https://www.curseforge.com/projects/1110803)

## The fight

The boss fights in three phases:

- **Phase 1:** Spells and melee attacks.
- **Phase 2:** More aggressive, with new spell combinations.
- **Final phase:** Summons minions often and drains life. Expect constant pressure.

Knock it into the void and it teleports back. The fight has its own music and boss bar, and it's built for several players at once.

On 1.21.1 the boss also has an intro cutscene and voice lines, and it taunts you during the fight.

## Summoning

1. Craft a **Shadow Altar** and a **Shadow Orb**.
2. Place the altar in the End.
3. Put the orb on the altar. The boss copies whoever offered it.

## Minions

During the fight, the boss summons minions that go after the nearest players. They won't attack the boss or each other, and they disappear when the boss dies.

## Items

![Items](https://i.imgur.com/vTjl5SU.jpeg)

- **Shadow Orb:** Summons the boss.
- **Doppelganger Ring:** +150 mana, +30 health.
- **Elder's Necklace:** +200 mana, +25% Eldritch spell power.
- **Summoner's Necklace:** +225 mana, +25% summon damage.

## Spells

The boss uses two custom spells. Neither can be crafted or found in normal loot, but the boss has a chance to drop either one as a scroll.

- **Portal Jump:** A quick jump to dodge attacks.
- **Summon Minion:** Calls in one of the boss's minions.

## Configuration

You can configure the boss's health, damage, armor and spell power, its spell lists, the minions' strength and the Hard Mode options. On 1.21.1 server owners can also turn off the intro cutscene.

## Versions and branches

| Minecraft | Loader | Branch |
| --- | --- | --- |
| 1.21.1 | NeoForge | [`1.21`](https://github.com/Bandit-bytes/darkdoppelganger/tree/1.21) |
| 1.20.1 | Forge | [`master`](https://github.com/Bandit-bytes/darkdoppelganger/tree/master) |

**1.21.1 needs:** NeoForge 21.1+, Iron's Spells 'n Spellbooks 3.15.0+, GeckoLib 4.7.5.1+ and Player Animator 2.0.1+.

**1.20.1 needs:** Forge 47.4.10+, Iron's Spells 'n Spellbooks 3.15.0+, Curios API 5.7.0+, Caelus API 3.1.0+, GeckoLib 4.4.5+ and Player Animator 1.0.2-rc1+.

## Building

The 1.21.1 branch needs Java 21. The 1.20.1 branch needs Java 17.

```
./gradlew build
```

The jar ends up in `build/libs/`. On 1.21.1 you can run the game tests with `./gradlew runGameTestServer`.

## Bugs and contributions

Report bugs on the [issue tracker](https://github.com/Bandit-bytes/darkdoppelganger/issues). Pull requests are welcome. Open them against the branch for the Minecraft version you're changing.

## License

All rights reserved.

---

[![BisectHosting](https://i.imgur.com/2oU2tyH.png)](https://www.bisecthosting.com/p/pixeldream)
