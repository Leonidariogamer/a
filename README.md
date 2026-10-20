# Recruits x Flint'N'Powder

Forge 1.20.1 compatibility mod: **Villager Recruits crossbowmen can load, aim and fire
[Flint'N'Powder](https://www.curseforge.com/minecraft/mc-mods/flintnpowder) guns.**

Requires Villager Recruits 1.15.x and Flint'N'Powder 1.0.0 (both on client and server).

## How to use
1. Put the gun in a **Crossbowman** recruit's weapon slot (or in its inventory; it equips itself).
2. Give it **gunpowder** and the gun's **shot** (round shot, buckshot, ...). A ramrod is optional by default.
3. Let it fight. Between fights it reloads on its own.

Supported guns (muzzle loaders): Musket, Bayonet Musket, Rifle, Pistol, Arquebus, Hand Gonne,
Blunderbuss, Bruttbuss, Big Game, Flinter. Cartridge/magazine guns are not supported yet.
Settings are in `config/recruits_flintnpowder-common.toml`.

## How it works
Flint'N'Powder runs each gun's right click through `<Gun>RightclickedProcedure.execute(...)`. The
mod calls that same procedure reflectively with the recruit as the shooter, lending a recruit's
offhand to each reload step (gunpowder, shot, ramrod), then trigger. A step counts as accepted when the gun's NBT changes,
so damage, projectiles, sounds and enchantments are all Flint'N'Powder's own.

## Building
`./gradlew build` (JDK 17). The jar is in `build/libs/`. Every push also builds it in GitHub
Actions (Actions tab -> latest run -> Artifacts). Project layout and Gradle setup follow
[Recruits Use Boomsticks](https://github.com/iwosw/recruits-use-boomsticks) (MIT).

## Status
Written against the decompiled jars but **not yet compiled or tested in-game**; the first CI run
and an in-game test are the next steps.
