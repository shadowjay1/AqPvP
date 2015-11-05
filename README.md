# AqPvP

## Classes

AqPvP has a loosely enforced class system, which means although it's possible to merge elements of different classes, these classes are some of the theoretically optimal builds.

### Fighter

This class would take items most similar to the vanilla PvP fighting kit. There is only one change in place for this class.

**Time-based attack scaling:**

Whenever you hit an opponent, your damage is multiplied by the time since the last attack (up to a limit) divided by said limit.

For mathy people (t: time since last attack, x: damage):

![equation](http://i.imgur.com/pOtnt68.png)

This will effectively reduce the skill/clickiness required for simple melee combat, making melee combat one of the easiest of the classes to master.

### Mage

The mage will use different wands (blaze rods by default), to cast different spells. Spells consume hunger as a resource, and players cannot carry food while having a wand in their inventory. Because of this, players who want to cast lots of spells during a fight will need to keep plenty of saturation potions in their combat inventories.

Most spells have some type of armor scaling. All armor parts add armor bonus equal to the components required to build that armor part divided by the components required to build the full set of armor times the constant for that armor material. Armor bonus will be referred to throughout this documentation as Ab.

Material | Constant
--- | ---
Diamond | 0
Iron | 1
Gold | 2
Leather | 1
Chainmail | 3

For example, an iron helmet would give an Ab of (5 / 24) * 1 = ~.208

Now on to the spells...

**Smite:**

Deals damage at a low cooldown at long range.

Damage: 4 + 2Ab  
Cooldown: 0.5s  
Cost: 1

**Launch:**

Launches target away from the caster.

Velocity: 1.5 + .5Ab  
Cooldown: 0  
Cost: 2

**Wither:**

Withers a target.  
Cooldown: 10s  
Cost: 15

Wither duration: 200 + 300Ab ticks

**Snare:**

Launches a projectile that snares a target.  
Cooldown: 5s  
Cost: 0

Snare length: 3 + 2Ab seconds

**Drain:**

Drains health from a target.

Drain damage/healing: 1 + 1Ab  
Cooldown: 0  
Cost: 5

**Rupture:**

Opens a rupture in the dimensional plane or something. Effects unknown.

Cooldown: 60
Cost: 10
