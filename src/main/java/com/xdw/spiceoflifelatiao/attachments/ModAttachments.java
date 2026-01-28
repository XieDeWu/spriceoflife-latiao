package com.xdw.spiceoflifelatiao.attachments;

import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SpiceOfLifeLatiao.MODID);
    public static final Supplier<AttachmentType<LevelOrgFoodValue>> LEVEL_ORG_FOOD_VALUE =
            ATTACHMENTS.register("level_org_food_value", () -> AttachmentType.builder(LevelOrgFoodValue::new)
                    .serialize(LevelOrgFoodValue.CODEC)      // 存档用 Codec
                    .sync(LevelOrgFoodValue.STREAM_CODEC)    // 同步用 StreamCodec
                    .build());
    public static final Supplier<AttachmentType<PlayerUnSleepTimeRecord>> PLAYER_UN_SLEEPTIME =
            ATTACHMENTS.register("player_un_sleeptime", () -> AttachmentType.builder(() -> new PlayerUnSleepTimeRecord(0L))
                    .serialize(PlayerUnSleepTimeRecord.CODEC)      // 存档用 Codec
                    .sync(PlayerUnSleepTimeRecord.STREAM_CODEC)    // 同步用 StreamCodec
                    .build());
    public static final FifoHashMap<Integer,Long> server_cached_player_un_sleeptime = new FifoHashMap<>(256);
}

