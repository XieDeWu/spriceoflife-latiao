# 生活调味料：辣条版
## 概述
一个生活调味料系列的生活质量调整Mod，将放缓生活节奏，继承
“[生活调味料：经典版](https://www.curseforge.com/minecraft/mc-mods/the-spice-of-life)” ，
“[MITE](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1294284-minecraft-is-too-easy-mite-mod)”
思想，此模组偏向难度调整，饮食饥饿曲线将偏向"真实"¿，如下属性将受到动态影响：

#### 自然饥饿，饱食度，饱和度，食用时间

本模组的饥饿默认缓动曲线经多次调整优先适用于中短期高难度生存整合包以及长期农夫乐事整合包，以及条件适用原版生存
，但普适面较窄，故若遇到平衡性问题需修改配置中的计算公式，所需计算上下文已详细给出。

若用于长期原版生存，建议最大历史长度降低至一半以下。

使用[Exp4j](https://www.objecthunter.net/exp4j/)公式进行计算，本模组仅额外扩展了max与min函数，若上下文计算异常将恢复原版数值。

本项目为临时起意的实验性项目，初用于个人单机生存。
因工作原因，作者不承诺长期维护，主版本维护并不会及时，亦无能力进行新旧版本迁移与维护工作。

代码遵循「MIT 协议」，并按原样提供，如急需新旧版本维护或长期使用，自行fork并维护副本，PR请求响应不及时请见谅。

[GitHub](https://github.com/XieDeWu/spriceoflife-latiao.git)
[Curseforge]()
[Modrinth]()

### 默认自然饥饿公式 LOSS：
#### 固定系数、饥饿程度、短期营养、短期摄入、过饱和、盔甲值、亮度、湿漉、方块速度系数，玩家状态数、玩家睡眠中
```text
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
### 默认饱食度公式 HUNGER：
#### 短期饮食、长期饮食、最低点
```text
HUNGER = HUNGER_ORG*0.4*max((0.9^EATEN_SHORT),max(1-HUNGER_LEVEL/12,0))
+HUNGER_ORG*0.4*max(0,1-EATEN_LONG/max(16,64-2*HUNGER_ORG-SATURATION_ORG))
+HUNGER_ORG*0.2
```
### 默认饱和度公式 SATURATION：
#### 短期饮食、长期饮食、最低点
```text
SATURATION = SATURATION_ORG
*(0.9^EATEN_SHORT)
*max(0,1-EATEN_LONG/max(16,32+8*(SATURATION_ORG-HUNGER_ORG)))
+(HUNGER_ORG*0.2+SATURATION_ORG*0.2)*max(1-HUNGER_LEVEL/12,0)
```
### 默认食用时间公式 EAT_SECONDS：
#### 食物原饱食度与饱食偏移、饥饿程度、食物效果数
```text
EAT_SECONDS = EAT_SECONDS_ORG
*(0.5+0.1*(2*HUNGER_ORG-HUNGER))
*(49/30*(10/7)^(HUNGER_LEVEL/10)-4/3)
*(1/(1+FOOD_BUFF))*(2-1/(1+FOOD_DEBUFF))
```
### 可用上下文及其计算顺序
#### 默认数值均为0.0，仅支持向前引用，将按如下内容进行顺序计算
```text
HUNGER_LEVEL 玩家饱食度
SATURATION_LEVEL 玩家饱和度
SUM_HUNGER_SHORT 玩家饱食短期累计
SUM_HUNGER_LONG 玩家饱食长期累计
SUM_SATURATION_SHORT 玩家饱和短期累计
SUM_SATURATION_LONG 玩家饱和长期累计
ARMOR 玩家盔甲值
LIGHT 光照
IS_WET 是否湿漉
RAIN_LEVEL 下雨强度
THUNDER_LEVEL 雷雨强度
BLOCK_SPEED_FACTOR 方块速度系数
PLAYER_BUFF 玩家增益数
PLAYER_DEBUFF 玩家减益数
PLAYER_ZZZ 玩家睡眠中
LOSS 自然饥饿
HUNGER_ORG 食物原饱食度
SATURATION_ORG 食物原饱和度
EAT_SECONDS_ORG 食物原食用时间
FOOD_BUFF 食物可能增益数
FOOD_DEBUFF 食物可能减益数
HUNGER_SHORT 食物饱食短期累计
HUNGER_LONG 食物饱食长期累计
SATURATION_SHORT 食物饱和短期累计
SATURATION_LONG 食物饱和长期累计
EATEN_SHORT 食物短期食用数
EATEN_LONG 食物长期食用数
HUNGER 食物饱食度
SATURATION 食物饱和度
EAT_SECONDS 食物食用时间
```