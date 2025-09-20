package com.xdw.spiceoflifelatiao.util;

import java.util.Optional;

public interface IEatHistoryAcessor {
    Optional<byte[]> getEatHistory();
    void setEatHistory(byte[] eatHistoryBytes);
    Optional<byte[]> addEatHistory(Integer foodID,Float hunger,float saturation,float eaten,float hungerRoundErr);
}
