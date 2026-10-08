import json, os, struct, zlib

ROOT = "/home/claude/ironmod/src/main/resources"
ASSETS = f"{ROOT}/assets/ironmod"
DATA = f"{ROOT}/data/ironmod"

# ---------------------------------------------------------------- PNG helpers
class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[(0, 0, 0, 0)] * w for _ in range(h)]
    def rect(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if 0 <= xx < self.w and 0 <= yy < self.h:
                    self.px[yy][xx] = c
    def save(self, path):
        raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in self.px)
        def chunk(t, d):
            c = struct.pack(">I", len(d)) + t + d
            return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
        png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
        with open(path, "wb") as f:
            f.write(png)

def rgba(r, g, b): return (r, g, b, 255)
WHITE = rgba(255, 255, 255)

SUITS = {
    "mark_3":  dict(name="Mark III", R=rgba(176, 30, 30), G=rgba(224, 176, 48), D=rgba(60, 10, 10),  C=rgba(140, 220, 255), tier="minecraft:iron_ingot"),
    "mark_7":  dict(name="Mark VII", R=rgba(150, 18, 22), G=rgba(214, 168, 40), D=rgba(50, 8, 10),   C=rgba(120, 230, 255), tier="minecraft:diamond"),
    "mark_42": dict(name="Mark XLII", R=rgba(168, 26, 30), G=rgba(232, 190, 70), D=rgba(55, 10, 12), C=rgba(150, 235, 255), tier="minecraft:emerald"),
    "mark_50": dict(name="Mark L", R=rgba(138, 12, 20), G=rgba(212, 164, 52), D=rgba(40, 6, 10), C=rgba(110, 255, 255), tier="minecraft:amethyst_shard"),
    "mark_85": dict(name="Mark LXXXV", R=rgba(120, 12, 20), G=rgba(196, 150, 40), D=rgba(35, 6, 10), C=rgba(80, 255, 255),  tier="minecraft:netherite_ingot"),
}

# ---------------------------------------------------------------- item icons (16x16 ASCII art)
ICONS = {
"helmet": [
"................","................","....DDDDDDDD....","...DRRRRRRRRD...","..DRRRRRRRRRRD..","..DRRRRRRRRRRD..",
"..DRGGGGGGGGRD..","..DRGGGGGGGGRD..","..DRGCCGGCCGRD..","..DRGGGGGGGGRD..","..DRGGGGGGGGRD..","..DRRGGGGGGRRD..",
"...DRRGGGGRRD...","....DDDDDDDD....","................","................"],
"chestplate": [
"................",".DDDD......DDDD.","DRRRRD....DRRRRD","DRRRRRDDDDRRRRRD","DRRRRRRRRRRRRRRD","DRRRGGGGGGGGRRRD",
".DRRGGGCCGGGRRD.",".DRRGGCCCCGGRRD.",".DRRGGGCCGGGRRD.",".DRRRGGGGGGRRRD.",".DRRRRRRRRRRRRD.","..DRRRRRRRRRRD..",
"..DRRGGGGGGRRD..","...DDDDDDDDDD...","................","................"],
"leggings": [
"................","..DDDDDDDDDDDD..","..DGGGGGGGGGGD..","..DRRRRRRRRRRD..","..DRRRRRRRRRRD..","..DRRRRDDRRRRD..",
"..DRRRD..DRRRD..","..DRRRD..DRRRD..","..DRRRD..DRRRD..","..DRRRD..DRRRD..","..DRRGD..DGRRD..","..DRRRD..DRRRD..",
"..DRRRD..DRRRD..","..DDDDD..DDDDD..","................","................"],
"boots": [
"................","................","................","................","................","................",
"..DDDD....DDDD..","..DRRD....DRRD..","..DRRD....DRRD..","..DRRD....DRRD..",".DRRRRD..DRRRRD.",".DRGGGD..DGGGRD.",
".DRRRRD..DRRRRD.",".DDDDDD..DDDDDD.","................","................"],
}
for k, rows in ICONS.items():
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), k

def icon_canvas(rows, pal):
    cv = Canvas(16, 16)
    m = {"R": pal["R"], "G": pal["G"], "D": pal["D"], "C": pal["C"], "W": WHITE}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in m: cv.px[y][x] = m[ch]
    return cv

def reactor_canvas(size, pal, bg=None):
    cv = Canvas(size, size)
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = ((x - c) ** 2 + (y - c) ** 2) ** 0.5 / (size / 2)
            if d < 0.25: col = WHITE
            elif d < 0.55: col = pal["C"]
            elif d < 0.75: col = pal["G"]
            elif d < 0.88: col = pal["D"]
            else: col = bg or (0, 0, 0, 0)
            cv.px[y][x] = col
    return cv

# ---------------------------------------------------------------- armor layers (64x32)
def armor_layers(p):
    R, G, D, C = p["R"], p["G"], p["D"], p["C"]
    l1 = Canvas(64, 32); l2 = Canvas(64, 32)
    # --- head (helmet)
    l1.rect(0, 0, 32, 16, R)
    l1.rect(11, 0, 2, 8, G)            # top stripe
    l1.rect(9, 9, 6, 7, G)             # faceplate
    l1.rect(9, 11, 2, 1, C); l1.rect(13, 11, 2, 1, C)   # eyes
    l1.rect(11, 14, 2, 1, D)           # mouth line
    l1.rect(0, 14, 8, 2, G); l1.rect(16, 14, 8, 2, G); l1.rect(24, 14, 8, 2, G)
    # --- torso (chestplate)
    l1.rect(16, 16, 24, 16, R)
    l1.rect(20, 20, 8, 2, G)           # collar
    l1.rect(22, 23, 4, 4, C); l1.rect(23, 24, 2, 2, WHITE)   # arc reactor
    l1.rect(21, 29, 6, 3, G)           # abdomen plate
    l1.rect(35, 20, 2, 10, G)          # back stripe
    # --- arms (shared UV for both arms)
    l1.rect(40, 16, 16, 16, R)
    l1.rect(40, 20, 16, 2, G)          # shoulder trim
    l1.rect(40, 28, 16, 4, G)          # gauntlet
    # --- boots (lowest 4 rows of the leg UV)
    l1.rect(4, 16, 4, 4, R); l1.rect(8, 16, 4, 4, D)
    l1.rect(0, 28, 16, 4, R); l1.rect(0, 30, 16, 2, G)
    # --- leggings (layer 2)
    l2.rect(0, 16, 16, 16, R)
    l2.rect(0, 20, 16, 2, G)           # thigh trim
    l2.rect(0, 25, 16, 2, G)           # knee plates
    l2.rect(16, 28, 24, 4, R)          # waist (body UV, bottom 4 rows only)
    l2.rect(16, 28, 24, 1, G)          # belt
    return l1, l2

# ---------------------------------------------------------------- write assets
os.makedirs(f"{ASSETS}/textures/item", exist_ok=True)
os.makedirs(f"{ASSETS}/textures/models/armor", exist_ok=True)

reactor_canvas(16, SUITS["mark_3"]).save(f"{ASSETS}/textures/item/arc_reactor.png")
reactor_canvas(128, SUITS["mark_3"], bg=rgba(18, 24, 32)).save(f"{ASSETS}/icon.png")

lang = {
    "itemGroup.ironmod.suits": "Iron Man Suits",
    "item.ironmod.arc_reactor": "Arc Reactor",
    "key.categories.ironmod": "Iron Man Mod",
    "key.ironmod.ability_1": "Suit Ability 1",
    "key.ironmod.ability_2": "Suit Ability 2",
    "key.ironmod.ability_3": "Suit Ability 3",
    "key.ironmod.ability_4": "Suit Ability 4",
    "ability.ironmod.flight": "Flight",
    "ability.ironmod.repulsor": "Repulsor",
    "ability.ironmod.unibeam": "Unibeam",
    "ability.ironmod.boost": "Thruster Boost",
    "ability.ironmod.missiles": "Micro-Missiles",
    "hud.ironmod.arc": "ARC %s/%s",
    "message.ironmod.low_energy": "Arc reactor energy too low!",
    "message.ironmod.flight_on": "Flight systems online (double-tap jump, hold W to thrust)",
    "message.ironmod.flight_off": "Flight systems offline",
}
pieces = [("helmet", "Helmet"), ("chestplate", "Chestplate"), ("leggings", "Leggings"), ("boots", "Boots")]
model = lambda tex: {"parent": "minecraft:item/generated", "textures": {"layer0": f"ironmod:item/{tex}"}}
json.dump(model("arc_reactor"), open(f"{ASSETS}/models/item/arc_reactor.json", "w"), indent=2)

def recipe(name, pattern, key, result):
    r = {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
         "key": {k: {"item": v} for k, v in key.items()}, "result": {"id": f"ironmod:{result}", "count": 1}}
    json.dump(r, open(f"{DATA}/recipe/{name}.json", "w"), indent=2)

recipe("arc_reactor", ["GRG", "RDR", "GRG"],
       {"G": "minecraft:gold_ingot", "R": "minecraft:redstone", "D": "minecraft:diamond"}, "arc_reactor")

for sid, pal in SUITS.items():
    l1, l2 = armor_layers(pal)
    l1.save(f"{ASSETS}/textures/models/armor/{sid}_layer_1.png")
    l2.save(f"{ASSETS}/textures/models/armor/{sid}_layer_2.png")
    key = {"I": pal["tier"], "G": "minecraft:gold_ingot", "A": "ironmod:arc_reactor"}
    patterns = {
        "helmet":     ["IGI", "I I"],
        "chestplate": ["I I", "IAI", "IGI"],
        "leggings":   ["IGI", "I I", "I I"],
        "boots":      ["I I", "G G"],
    }
    for piece, label in pieces:
        item = f"{sid}_{piece}"
        lang[f"item.ironmod.{item}"] = f"{pal['name']} {label}"
        icon_canvas(ICONS[piece], pal).save(f"{ASSETS}/textures/item/{item}.png")
        json.dump(model(item), open(f"{ASSETS}/models/item/{item}.json", "w"), indent=2)
        k = dict(key)
        if "A" not in "".join(patterns[piece]): k.pop("A")
        recipe(item, patterns[piece], k, item)

json.dump(lang, open(f"{ASSETS}/lang/en_us.json", "w"), indent=2)

# ---------------------------------------------------------------- suit sets (the "Fisk-style" data)
def ab(**kw): return kw
suits = {
 "mark_3": {
   "max_energy": 800, "energy_regen_per_tick": 0.4, "fall_damage_immunity": True,
   "passive_effects": [{"effect": "minecraft:strength", "amplifier": 0}],
   "abilities": [
     ab(type="flight",   speed=0.6, energy_per_tick=0.6),
     ab(type="repulsor", damage=6,  range=32, cooldown=8,  energy_cost=15),
     ab(type="boost",    speed=1.6, cooldown=60, energy_cost=60)]},
 "mark_7": {
   "max_energy": 1200, "energy_regen_per_tick": 0.6, "fall_damage_immunity": True,
   "passive_effects": [{"effect": "minecraft:strength", "amplifier": 0}, {"effect": "minecraft:resistance", "amplifier": 0}],
   "abilities": [
     ab(type="flight",   speed=0.8, energy_per_tick=0.7),
     ab(type="repulsor", damage=8,  range=40, cooldown=6,  energy_cost=15),
     ab(type="unibeam",  damage=22, range=48, cooldown=160, energy_cost=300, explosion_power=2.0, ignite_seconds=4),
     ab(type="boost",    speed=2.0, cooldown=50, energy_cost=60)]},
 "mark_42": {
   "max_energy": 1500, "energy_regen_per_tick": 0.8, "fall_damage_immunity": True,
   "passive_effects": [{"effect": "minecraft:strength", "amplifier": 0}, {"effect": "minecraft:resistance", "amplifier": 0}],
   "abilities": [
     ab(type="flight",   speed=0.9, energy_per_tick=0.7),
     ab(type="repulsor", damage=9,  range=40, cooldown=5,  energy_cost=15),
     ab(type="missiles", damage=6, range=48, cooldown=100, energy_cost=200, count=6, explosion_power=1.5),
     ab(type="boost",    speed=2.2, cooldown=45, energy_cost=60)]},
 "mark_50": {
   "max_energy": 1800, "energy_regen_per_tick": 0.9, "fall_damage_immunity": True,
   "passive_effects": [{"effect": "minecraft:strength", "amplifier": 1}, {"effect": "minecraft:resistance", "amplifier": 0},
                       {"effect": "minecraft:regeneration", "amplifier": 0}],
   "abilities": [
     ab(type="flight",   speed=1.0, energy_per_tick=0.75),
     ab(type="repulsor", damage=11, range=44, cooldown=4,  energy_cost=15),
     ab(type="unibeam",  damage=30, range=56, cooldown=130, energy_cost=350, explosion_power=2.5, ignite_seconds=5),
     ab(type="missiles", damage=7,  range=52, cooldown=90,  energy_cost=220, count=8, explosion_power=1.5)]},
 "mark_85": {
   "max_energy": 2000, "energy_regen_per_tick": 1.0, "fall_damage_immunity": True,
   "passive_effects": [{"effect": "minecraft:strength", "amplifier": 1}, {"effect": "minecraft:resistance", "amplifier": 1},
                       {"effect": "minecraft:fire_resistance", "amplifier": 0}, {"effect": "minecraft:water_breathing", "amplifier": 0}],
   "abilities": [
     ab(type="flight",   speed=1.0, energy_per_tick=0.8),
     ab(type="repulsor", damage=12, range=48, cooldown=4,  energy_cost=15),
     ab(type="unibeam",  damage=38, range=64, cooldown=120, energy_cost=400, explosion_power=3.0, ignite_seconds=6),
     ab(type="boost",    speed=2.4, cooldown=40, energy_cost=60)]},
}
for sid, d in suits.items():
    json.dump(d, open(f"{DATA}/suit_sets/{sid}.json", "w"), indent=2)

# ---------------------------------------------------------------- suit stand block
os.makedirs(f"{ASSETS}/textures/block", exist_ok=True)
os.makedirs(f"{ASSETS}/blockstates", exist_ok=True)
os.makedirs(f"{ASSETS}/models/block", exist_ok=True)
os.makedirs(f"{DATA}/loot_table/blocks", exist_ok=True)
os.makedirs(f"{ROOT}/data/minecraft/tags/block/mineable", exist_ok=True)

pal = SUITS["mark_3"]
tex = Canvas(16, 16)
tex.rect(0, 0, 16, 16, rgba(52, 56, 66))
tex.rect(0, 0, 16, 1, pal["G"]); tex.rect(0, 15, 16, 1, pal["G"])
tex.rect(0, 0, 1, 16, pal["G"]); tex.rect(15, 0, 1, 16, pal["G"])
tex.rect(2, 2, 12, 12, rgba(36, 40, 48))
tex.rect(6, 6, 4, 4, pal["C"]); tex.rect(7, 7, 2, 2, WHITE)
tex.save(f"{ASSETS}/textures/block/suit_stand.png")

def cube(frm, to):
    face = {"texture": "#0"}
    return {"from": frm, "to": to, "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")}}
json.dump({"parent": "minecraft:block/block", "textures": {"0": "ironmod:block/suit_stand", "particle": "ironmod:block/suit_stand"},
           "elements": [cube([2, 0, 2], [14, 2, 14]), cube([5, 2, 5], [11, 4, 11]), cube([7, 4, 7], [9, 16, 9])]},
          open(f"{ASSETS}/models/block/suit_stand.json", "w"), indent=2)
json.dump({"parent": "ironmod:block/suit_stand"}, open(f"{ASSETS}/models/item/suit_stand.json", "w"), indent=2)
json.dump({"variants": {"": {"model": "ironmod:block/suit_stand"}}}, open(f"{ASSETS}/blockstates/suit_stand.json", "w"), indent=2)
json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"}],
           "entries": [{"type": "minecraft:item", "name": "ironmod:suit_stand"}]}]},
          open(f"{DATA}/loot_table/blocks/suit_stand.json", "w"), indent=2)
json.dump({"values": ["ironmod:suit_stand"]}, open(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", "w"), indent=2)
recipe("suit_stand", ["GAG", " I ", "III"],
       {"G": "minecraft:gold_ingot", "A": "ironmod:arc_reactor", "I": "minecraft:iron_ingot"}, "suit_stand")
lang["block.ironmod.suit_stand"] = "Suit-Up Stand"
lang["message.ironmod.stand_incomplete"] = "The stand needs a helmet, chestplate, leggings and boots."
json.dump(lang, open(f"{ASSETS}/lang/en_us.json", "w"), indent=2)

print("assets generated")
