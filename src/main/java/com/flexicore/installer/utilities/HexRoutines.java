package com.flexicore.installer.utilities;

public final class HexRoutines {

    private static final byte[] correspondingNibble = new byte['f' + 1];

    // -------------------------- STATIC METHODS --------------------------

    static {
        // only 0..9 A..F a..f have meaning. rest are errors.
        for (int i = 0; i <= 'f'; i++) {
            correspondingNibble[i] = -1;
        }
        for (int i = '0'; i <= '9'; i++) {
            correspondingNibble[i] = (byte) (i - '0');
        }
        for (int i = 'A'; i <= 'F'; i++) {
            correspondingNibble[i] = (byte) (i - 'A' + 10);
        }
        for (int i = 'a'; i <= 'f'; i++) {
            correspondingNibble[i] = (byte) (i - 'a' + 10);
        }
    }

    private static int charToNibble(char c) {
        if (c > 'f') {
            throw new IllegalArgumentException("Invalid hex character: " + c);
        }
        int nibble = correspondingNibble[c];
        if (nibble < 0) {
            throw new IllegalArgumentException("Invalid hex character: " + c);
        }
        return nibble;
    }

    public static byte[] fromHexString(String s) {
        if (s == null) {
            return null;
        }
        int stringLength = s.length();
        if ("0".equals(s)) {
            return null;
        }
        if ("".equals(s)) {
            return null;
        }

        if ((stringLength & 0x1) != 0) {
            throw new IllegalArgumentException("fromHexString requires an even number of hex characters, hex has: "
                    + stringLength + ", and String was: " + s);
        }
        byte[] bytes = new byte[stringLength / 2];
        for (int i = 0, j = 0; i < stringLength; i += 2, j++) {
            int high = charToNibble(s.charAt(i));
            int low = charToNibble(s.charAt(i + 1));
            // You can store either unsigned 0..255 or signed -128..127 bytes in
            // a byte type.
            bytes[j] = (byte) ((high << 4) | low);
        }
        return bytes;
    }

    public static String toHexString(byte[] b) {
        if (b!=null) {
            StringBuffer sb = new StringBuffer(b.length * 2);
            for (int i = 0; i < b.length; i++) {
                // look up high nibble char
                sb.append(hexChar[(b[i] & 0xf0) >>> 4]);

                // look up low nibble char
                sb.append(hexChar[b[i] & 0x0f]);
            }
            return sb.toString();
        }else return "";
    }
    public static String toHexString(byte[] b,String delimiter) {
        if (b!=null) {
            StringBuffer sb = new StringBuffer(b.length * 2);
            for (int i = 0; i < b.length; i++) {
                // look up high nibble char
                sb.append(hexChar[(b[i] & 0xf0) >>> 4]);

                // look up low nibble char
                sb.append(hexChar[b[i] & 0x0f]);
                if (i<b.length-1 && delimiter!=null && !delimiter.isEmpty()) {
                    sb.append(delimiter);
                }
            }
            return sb.toString();
        }else return "";
    }
    public static String toHexString(byte[] b, int length) {
        StringBuffer sb = new StringBuffer(length * 2);
        for (int i = 0; i < length; i++) {
            // look up high nibble char
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);

            // look up low nibble char
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }
    public static String toHexString(byte[] b,int start, int length) {
        StringBuffer sb = new StringBuffer(length * 2);
        for (int i = start; i < length; i++) {
            // look up high nibble char
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);

            // look up low nibble char
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }
    /**
     * table to convert a nibble to a hex char.
     */
    static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
}
