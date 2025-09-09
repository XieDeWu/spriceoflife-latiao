package com.xdw.spiceoflife.latiao.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.LinkedList;
import java.util.Optional;

public record EatHistory(LinkedList<Integer> foodHash, LinkedList<Float> hunger,LinkedList<Float> saturation) implements Serializable  {
    @Serial private static final long serialVersionUID = 100L;
    public static Optional<Player> recentPlayer = Optional.empty();
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
    public static int getFoodHash(@NotNull Item item){
        return item.toString().replace(" ", "").hashCode();
    }
}