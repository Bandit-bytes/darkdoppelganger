# Changelog

## 10.1.0-1.20.1

### Changed
- The Dark Doppelganger now copies the summoner fully and is stronger than them by default.
  - Copied armor protects it. The new `copied_armor_effectiveness` option (default `0.5`) sets how much of your armor, toughness and Protection counts. Its own base armor always counts in full.
  - It copies your offhand (shield, totem, tipped arrows), both at summon and when it targets you. Turn this off with the new `copy_player_offhand` option.
  - Spell power is no longer capped by default: `max_copied_spell_power` now defaults to `0` (no cap).
  - `doppelgangerSpellPowerMultiplier` now defaults to `1.25`, so it casts a quarter stronger than you.
  - `banned_armor` now defaults to an empty list, so it copies netherite helmets too.

**Upgrading:** an existing `darkdoppelganger-common.toml` keeps its old values for `max_copied_spell_power`, `doppelgangerSpellPowerMultiplier` and `banned_armor`. Change them by hand, or delete the file to get the new defaults. The two new options are added automatically.
