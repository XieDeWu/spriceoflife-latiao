package com.xdw.spiceoflifelatiao.util;

import java.util.Optional;

public interface IEatHistoryAcessor {
    Optional<byte[]> getEatHistory_Bin();
    void setEatHistory_Bin(byte[] eatHistoryBytes);
    Optional<byte[]> addEatHistory_Mem(Integer foodID, Float hunger, float saturation, float eaten, float hungerRoundErr);
    EatHistory getEatHistory_Mem();
}
