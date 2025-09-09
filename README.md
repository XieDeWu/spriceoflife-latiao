# 生活调味料：辣条版
## 概述
继承“[生活调味料：经典版](https://www.curseforge.com/minecraft/mc-mods/the-spice-of-life)”思想，此模组将提高食物需求，动态计算食物的饱食度，饱和度，食用时间。

本模组的默认参数经多次调整适用于轻度农夫乐事衍生，与高难度生存整合包，若用于原版，建议最大历史长度降低一半。

使用[Exp4j](https://www.objecthunter.net/exp4j/)公式进行计算，本模组仅额外扩展了max与min函数，可用计算上下文将在后文提及，若计算异常将恢复原版机制。

 [GitHub]()

### 默认自然饥饿公式 LOSS：
#### 固定系数、饥饿程度、短期营养、短期摄入、过饱和惩罚
```text
LOSS = 0.005
*(2^(HUNGER_LEVEL/10-1))
*(0.5^(SUM_SATURATION_SHORT/max(SUM_HUNGER_SHORT,1)-1))
*(2^((SUM_SATURATION_SHORT+SUM_HUNGER_SHORT)/128 -1))
*(1+2^(SATURATION_LEVEL/4-4)-0.0625)
```
### 默认饱食度公式 HUNGER：
#### 短期饮食、饥饿程度、长期饮食、最低点
```text
HUNGER = HUNGER_ORG*0.4*max((0.8^EATEN_SHORT),max(1-HUNGER_LEVEL/12,0))
+HUNGER_ORG*0.4*max(1-EATEN_LONG/64,0)
+HUNGER_ORG*0.2
```
### 默认饱和度公式 SATURATION：
#### 短期饮食、长期饮食、饥饿程度
```text
SATURATION = SATURATION_ORG
*(0.9^EATEN_SHORT)
*max(1-EATEN_LONG/64,0)
+(HUNGER_ORG*0.2+SATURATION_ORG*0.2)*max(1-HUNGER_LEVEL/12,0)
```
### 默认食用时间公式 EAT_SECONDS：
#### 食物原食用时间、食物原饱食度与饱食偏移、饥饿程度、食物效果数
```text
EAT_SECONDS_ORG
*(0.5+0.1*(2*HUNGER_ORG-HUNGER))
*(49/30*(10/7)^(HUNGER_LEVEL/10)-4/3)
*(1/(1+BUFF))*(2-1/(1+DEBUFF))
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
LOSS 自然饥饿
HUNGER_ORG 食物原饱食度
SATURATION_ORG 食物原饱和度
EAT_SECONDS_ORG 食物原食用时间
BUFF 食物可能增益数
DEBUFF 食物可能减益数
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