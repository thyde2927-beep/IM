# Iron Man Mod (Fabric 1.21.1)

Data-driven Iron Man suits, built in the spirit of Fisk's Superheroes: a suit is a full armor set,
and its powers/stats are defined in a JSON "suit set" instead of hard-coded.

## Build
Requires JDK 21.
    ./gradlew build          # jar ends up in build/libs/ (use the one WITHOUT -sources)
    ./gradlew runClient      # test in a dev client
Needs Fabric Loader + Fabric API 0.116.x for 1.21.1 in your mods folder.

## Playing
1. Craft an Arc Reactor (gold/redstone/diamond ring), then a full armor set
   (Mark III = iron, Mark VII = diamond, Mark XLII = emerald (adds Micro-Missiles), Mark L = amethyst shard (nanotech: regeneration, no boost, missiles + unibeam), Mark LXXXV = netherite; chestplate needs an Arc Reactor).
   Or grab them from the "Iron Man Suits" creative tab.
2. Wear all four pieces of the SAME suit. The HUD appears (arc reactor energy + abilities).
3. Default keys (rebind in Controls > Iron Man Mod):
   Z = ability 1 (Flight toggle), X = Repulsor, C = Unibeam (Mark VII+) or Boost, V = Boost.
   Slots follow the order of "abilities" in the suit JSON.
4. Flight: press Z, then double-tap Space. Hold W to thrust where you look, Sprint for afterburners.
   Flying drains arc reactor energy; it regenerates over time.

## Suit sets (data/ironmod/suit_sets/<suit_id>.json)
    max_energy, energy_regen_per_tick, fall_damage_immunity,
    passive_effects: [{effect, amplifier}],
    abilities: [{type: flight|repulsor|unibeam|missiles|boost, count (missiles), damage, range, speed, cooldown,
                 energy_cost, energy_per_tick, explosion_power, ignite_seconds, block_damage}]
Edit and run /reload to rebalance live. A data pack can override these files.

## Adding a new suit
1. Add the id to ModItems.registerSuit(...) (material + durability factor).
2. Add textures: item icons, models/armor/<id>_layer_1.png and _layer_2.png.
3. Add lang entries, recipes, and data/ironmod/suit_sets/<id>.json.
(gen_assets.py shows how the placeholder art/recipes/lang were generated.)

## Art
All textures are simple generated placeholders. Replace the PNGs with real art any time.

## Build without installing anything (GitHub Actions)
1. Create a new GitHub repo and upload the contents of this folder (the .github folder must come along).
2. Open the repo's Actions tab. The "build" workflow runs on every push (or press Run workflow).
3. When it turns green, open the run and download "ironmod-jar" under Artifacts.
   Unzip it and use ironmod-1.0.0.jar (NOT the -sources jar) in your mods folder.
If it turns red, open the failed step and paste the error to get it fixed.

## Suit-Up Stand
Craft: gold / arc reactor / gold, iron, iron row. Right-click it with a suit piece to store it (one per slot).
With all four stored, right-click with an empty hand to SUIT UP: your armor and the stand's pieces swap.
Click again to swap back. Sneak + empty hand takes every piece off the stand. Breaking it drops its contents.
