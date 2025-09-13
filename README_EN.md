> ⚠️ **This document is translated by AI. If anything is unclear, please
> refer to the original Chinese documentation.**

# Spice of Life: Latiao Edition

## Overview

A quality-of-life adjustment mod in the "Spice of Life" series that
slows down the pace of gameplay.\
It inherits ideas from\
- [Spice of Life: Classic
Edition](https://www.curseforge.com/minecraft/mc-mods/the-spice-of-life)\
- [MITE (Minecraft is Too
Easy)](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1294284-minecraft-is-too-easy-mite-mod)

This mod focuses on increasing difficulty and making the hunger and diet
curve feel more "realistic."\
The following attributes will be dynamically affected:

#### Natural hunger, food level, saturation, and eating time

The default hunger easing curve has been tuned multiple times to suit
mid-to-short-term high-difficulty survival modpacks, as well as
long-term farming and lifestyle modpacks.\
It can also be used in vanilla survival, though its general
applicability is limited.\
If balance issues arise, you can modify the calculation formulas in the
config, and the calculation context has been provided in detail.

For long-term vanilla survival, it is recommended to reduce the maximum
history length to half or less.

This mod uses [Exp4j](https://www.objecthunter.net/exp4j/) for formula
calculations, and only adds `max` and `min` as extra functions.\
If any calculation error occurs, the game will fall back to vanilla
values.

This project is a spontaneous experimental project originally made for
personal singleplayer survival.\
Due to work reasons, the author does not promise long-term maintenance.\
Major version updates will not be timely, and the author has no capacity
to backport or maintain multiple versions.

The code is licensed under the **MIT License** and provided as-is.\
If you need long-term support or multi-version maintenance, please fork
and maintain your own copy.\
PR requests may not be responded to promptly --- please understand.

[GitHub](https://github.com/XieDeWu/spriceoflife-latiao.git)\
[Curseforge]()\
[Modrinth]()

------------------------------------------------------------------------

### Default Natural Hunger Formula --- LOSS

#### Includes: constant factor, hunger level, short-term nutrition, short-term intake, oversaturation, armor value, light level, wetness, block speed factor, player buffs, player debuffs, sleeping state

``` text
LOSS = 0.005
*2^(HUNGER_LEVEL/20-1)
*0.5^(SUM_SATURATION_SHORT/max(SUM_HUNGER_SHORT,1)-1)
*2^((SUM_SATURATION_SHORT+SUM_HUNGER_SHORT)/128 -1)
*(1+2^(SATURATION_LEVEL/4-4)-0.0625)
*(1+(ARMOR/20)^2.41)
*(1.5-0.5*LIGHT/15)
*(1+0.5*IS_WET*(IS_WET+RAIN_LEVEL+THUNDER_LEVEL))
*(2-BLOCK_SPEED_FACTOR)
*0.8^PLAYER_BUFF
*1.5^PLAYER_DEBUFF
*(1-0.4*PLAYER_ZZZ)
```

### Default Food Level Formula --- HUNGER

#### Includes: short-term diet, long-term diet, minimum base

``` text
HUNGER = HUNGER_ORG*0.4*max((0.9^EATEN_SHORT),max(1-HUNGER_LEVEL/12,0))
+HUNGER_ORG*0.4*max(0,1-EATEN_LONG/max(16,64-2*HUNGER_ORG-SATURATION_ORG))
+HUNGER_ORG*0.2
```

### Default Saturation Formula --- SATURATION

#### Includes: short-term diet, long-term diet, minimum base

``` text
SATURATION = SATURATION_ORG
*(0.9^EATEN_SHORT)
*max(0,1-EATEN_LONG/max(16,32+8*(SATURATION_ORG-HUNGER_ORG)))
+(HUNGER_ORG*0.2+SATURATION_ORG*0.2)*max(1-HUNGER_LEVEL/12,0)
```

### Default Eating Time Formula --- EAT_SECONDS

#### Includes: original food values, hunger level, number of effects on the food

``` text
EAT_SECONDS = EAT_SECONDS_ORG
*(0.5+0.1*(2*HUNGER_ORG-HUNGER))
*(49/30*(10/7)^(HUNGER_LEVEL/10)-4/3)
*(1/(1+FOOD_BUFF))*(2-1/(1+FOOD_DEBUFF))
```

------------------------------------------------------------------------

### Available Calculation Context (in evaluation order)

#### All default to `0.0`. Only forward references are supported. Values are calculated in the following order:

``` text
HUNGER_LEVEL          Player food level
SATURATION_LEVEL      Player saturation
SUM_HUNGER_SHORT      Player short-term hunger total
SUM_HUNGER_LONG       Player long-term hunger total
SUM_SATURATION_SHORT  Player short-term saturation total
SUM_SATURATION_LONG   Player long-term saturation total
ARMOR                 Player armor value
LIGHT                 Light level
IS_WET                Whether the player is wet
RAIN_LEVEL            Rain intensity
THUNDER_LEVEL         Thunderstorm intensity
BLOCK_SPEED_FACTOR    Block speed factor
PLAYER_BUFF            Number of active buffs on player
PLAYER_DEBUFF          Number of active debuffs on player
PLAYER_ZZZ             Whether the player is sleeping
LOSS                   Natural hunger
HUNGER_ORG              Food’s original hunger value
SATURATION_ORG          Food’s original saturation value
EAT_SECONDS_ORG          Food’s original eating time
FOOD_BUFF                 Number of potential food buffs
FOOD_DEBUFF               Number of potential food debuffs
HUNGER_SHORT               Food short-term hunger contribution
HUNGER_LONG                Food long-term hunger contribution
SATURATION_SHORT            Food short-term saturation contribution
SATURATION_LONG              Food long-term saturation contribution
EATEN_SHORT                   Number of short-term consumptions of the food
EATEN_LONG                     Number of long-term consumptions of the food
HUNGER                          Food hunger value
SATURATION                       Food saturation value
EAT_SECONDS                        Food eating time
```
