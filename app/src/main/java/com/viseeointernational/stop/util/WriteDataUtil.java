package com.viseeointernational.stop.util;

public class WriteDataUtil {

    /**
     * 获取完整数据
     *
     * @param validData 除了开头 长度 校验之后的有效数据
     * @return
     */
    public static byte[] getWriteData(byte[] validData) {
        byte[] ret = new byte[validData.length + 4];
        ret[0] = (byte) 0xff;
        ret[1] = (byte) 0xaa;
        ret[2] = (byte) (validData.length + 1);
        ret[ret.length - 1] = ret[2];
        for (byte b : validData) {
            ret[ret.length - 1] += b;
        }
        System.arraycopy(validData, 0, ret, 3, validData.length);
        return ret;
    }
}
