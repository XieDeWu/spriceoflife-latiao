package com.xdw.spiceoflifelatiao.util;

import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record EatHistory(ArrayList<Integer> foodHash, ArrayList<Float> hunger, ArrayList<Float> saturation, ArrayList<Float> eaten, Float hungerRoundErr) implements Serializable  {
    @Serial private static final long serialVersionUID = 100L;
    public static Optional<LivingEntity> recentEntity = Optional.empty();
    public Optional<byte[]> toBytes() {
        try (ByteArrayOutputStream a = new ByteArrayOutputStream();
             ObjectOutputStream b = new ObjectOutputStream(a)) {
            b.writeObject(this);
            return Optional.of(a.toByteArray());
        } catch (IOException e) {
            return Optional.empty();
        }
    }
    public static Optional<EatHistory> fromBytes(byte[] bytes) {
        try (ByteArrayInputStream a = new ByteArrayInputStream(bytes);
             ObjectInputStream b = new ObjectInputStream(a)) {
            Object obj = b.readObject();
            if (obj instanceof EatHistory) {
                return Optional.of((EatHistory) obj);
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static final Map<Integer,Integer> cachedFoodHash =  new HashMap<>(64);
    public static int getFoodHash(@NotNull Item item){
        var id = System.identityHashCode(item);
        var rt = cachedFoodHash.get(id);
        if (rt == null) {
            rt = MurmurHash3.hash32x86(LevelOrgFoodValue.getCachedRegName(item).getBytes(StandardCharsets.UTF_8));
            cachedFoodHash.put(id, rt);
        }
        return rt;
    }
}