package com.example.examplemod.util;

import java.io.*;
import java.util.LinkedList;
import java.util.Optional;

public record EatHistory(LinkedList<String> food, LinkedList<Float> hunger,LinkedList<Float> saturation) implements Serializable  {
    @Serial private static final long serialVersionUID = 100L;
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

}