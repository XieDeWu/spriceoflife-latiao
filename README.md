⚠️ This section is translated by AI. If anything is unclear, please refer to the original Chinese documentation.

This mod is an extension of the Spice of Life series and adds three new mechanics along with their corresponding configuration files:

### 1. Dynamic adjustment of food values
The hunger (food level), saturation, and eating time of foods will be dynamically modified.
Repeatedly eating the same food will gradually reduce its restoration amount.

### 2. Natural hunger
Over time, the player’s food level will slowly decrease.

### 3. Sleep
After waking up from sleep, the player will lose food level proportionally to the amount of time slept.

---

本模组为生活调味料系列的补充，添加三个机制与其相应的配置文件。

### 1.动态修改食物的饱食度，饱和度，食用时间
多次食用相同食物的回复量将逐渐减少。

### 2.自然饥饿
随着时间流逝，降低玩家饱食度。

### 3.睡眠
睡醒后将移除睡眠时间相应的玩家饱食度
### 4. 手动可吃物品数据库与指令

服务器会创建并读取：`config/spiceoflifelatiao/edible_items.json`。
这个文件用于保存管理员手动添加的可吃物品，避免再去世界存档里的隐藏附件数据中查找和手改。

新增配置项：

- `enable_auto_block_food_collect`：是否继续自动学习/记录方块食物。遇到非食物被误判可关闭。
- `enable_manual_food_file`：是否启用上面的 JSON 手动数据库。
- `enable_food_id_safety_check`：是否启用物品注册名安全校验。默认开启，防止 hash/旧缓存误判；如果希望保留作者原本“命中缓存就可吃”的宽松逻辑，可以关闭。

常用指令（需要 OP 权限 2）：

- `/sol_latiao food path`：查看 JSON 配置文件位置。
- `/sol_latiao food list current [页码]`：查看当前世界可吃名单，包含原版、手动、自动学习条目。
- `/sol_latiao food list all [页码]`：查看所有已加载世界的手动/自动学习条目，兼容多世界加载场景。
- `/sol_latiao food show current <物品ID>`：查看当前世界某个物品的原版、手动、自动学习配置。
- `/sol_latiao food add current <物品ID> <饱食度> <饱和度> [食用秒数]`：给当前世界添加手动可吃物品。
- `/sol_latiao food add global <物品ID> <饱食度> <饱和度> [食用秒数]`：添加全局手动可吃物品。
- `/sol_latiao food set current <物品ID> nutrition|saturation|eatSeconds|canAlwaysEat|usingConvertsTo|bites|bitesOffset|bitesType <值>`：修改当前世界条目。
- `/sol_latiao food effect add current <物品ID> <效果ID> <持续tick> <等级> [概率]`：添加食后效果。
- `/sol_latiao food effect remove current <物品ID> <效果ID>`：删除指定效果。
- `/sol_latiao food effect clear current <物品ID>`：清空效果。
- `/sol_latiao food remove current <物品ID>`：删除当前世界手动条目。
- `/sol_latiao food reload` / `/sol_latiao food save`：重载/保存 JSON。
- `/sol_latiao food importLegacy current|all`：把旧的隐藏世界附件条目尽量导入到 JSON 文件，方便后续维护。

`/spiceoflifelatiao food ...` 也可以作为同义指令使用。
