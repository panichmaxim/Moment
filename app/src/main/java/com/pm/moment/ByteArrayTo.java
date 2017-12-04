package com.pm.moment;

public class ByteArrayTo {

    static public long convertLongToLittleEndian(long value) {
        return ((value >> 24) & 0xFF) | ((value << 8) & 0xFF0000) |
                ((value >> 8) & 0xFF00) | ((value << 24) & 0xFF000000);
    }

    static public int convertIntToLittleEndian(int value) {
        return ((value >> 24) & 0xFF) | ((value << 8) & 0xFF0000) |
                ((value >> 8) & 0xFF00) | ((value << 24) & 0xFF000000);
    }

    static public short convertShortToLittleEndian(short value) {
        return (short)((value >> 8) | (value << 8));
    }

    static public long convertToLong(byte[] array) {
        if (array.length == 8) {
            return (((array[0] & 255) << 56) | ((array[1] & 255) << 48) |
                    ((array[2] & 255) << 40) | ((array[3] & 255) << 32) |
                    ((array[4] & 255) << 24) | ((array[5] & 255) << 16) |
                    ((array[6] & 255) << 8) | ((array[7]) & 255));
        }
        return -1;
    }

    static public int convertToInt(byte[] array) {
        if (array.length == 4) {
            return (((array[0] & 255) << 24) | ((array[1] & 255) << 16) |
                    ((array[2] & 255) << 8) | array[3] & 255);
        }
        return -1;
    }

    static public short convertToShort(byte[] array) {
        if (array.length == 2) {
            return (short)(((array[0] & 255) << 8) | (array[1] & 255));
        }
        return -1;
    }
}
