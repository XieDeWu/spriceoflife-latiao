package com.xdw.spiceoflife.latiao.util;

import java.util.Optional;

public interface EatHistoryAcessor {
    Optional<byte[]> getEatHistory();
    void setEatHistory(byte[] eatHistoryBytes);
    Optional<byte[]> addEatHistory(Integer foodID,Float hunger,float saturation);
}
