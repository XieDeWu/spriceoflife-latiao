package com.example.examplemod.util;

import java.util.Optional;

public interface EatHistoryAcessor {
    Optional<byte[]> getEatHistory();
    void setEatHistory(byte[] eatHistoryBytes);
}
