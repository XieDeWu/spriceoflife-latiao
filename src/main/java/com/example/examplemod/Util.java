package com.example.examplemod;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Util {
    public record EatHistory(LinkedList<String> food, LinkedList<Float> hunger,LinkedList<Float> saturation) implements Serializable {
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
    public static Optional<byte[]> UtilQueueSerialize(List<?> list) {
        try (var baos = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(list);
            return Optional.of(baos.toByteArray());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    public static Optional<List<?>> UtilQueueDeserialize(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return Optional.ofNullable((List<?>) ois.readObject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
