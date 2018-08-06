package types;

import engine.SepDigit;

import java.util.ArrayList;

public enum DataType {

    TRYTE(1, "tryte", "dt"),
    PAIR(2, "pair", "dp"),
    TRIPLE(3, "triple", "d3"),
    QUAD(4, "quad", "dq");

    private int length;
    private String shortName, longName;

    DataType(int length, String longName, String shortName) {
        this.length = length;
        this.shortName = shortName;
        this.longName = longName;
    }

    public static DataType parseByName(String name) {
        name = name.toLowerCase();
        for (DataType type : values())
            if (type.longName.equals(name) || type.shortName.equals(name))
                return type;
        return null;
    }

    public ArrayList<String> compile(String number, int line) throws NumberFormatException {
        ArrayList<String> trytes = new ArrayList<>(length);
        if (number.startsWith("0t")) {
            // raw ternary number
            int maxLen = length * 6;
            if (number.length() > maxLen + 2)
                throw new NumberFormatException(String.format("Line #%d: value '%s' too big for %s (max %d trits).", line, number, longName, maxLen));
            number = number.substring(2);
            while (number.length() < maxLen) number = "0".concat(number);
            for (int i = maxLen; i > 0; i -= 6) trytes.add(number.substring(i - 6, i));
        } else if (number.startsWith("0x")) {
            // septemvigesimal number
            int maxLen = length * 2;
            if (number.length() > maxLen + 2)
                throw new NumberFormatException(String.format("Line #%d: value '%s' too big for %s (max %d sep digits).", line, number, longName, maxLen));
            number = number.substring(2);
            while (number.length() < length * 2) number = "0".concat(number);
            for (int i = length * 2; i > 0; i -= 2) {
                SepDigit d0 = SepDigit.parseBySymbol(number.substring(i - 1, i));
                SepDigit d1 = SepDigit.parseBySymbol(number.substring(i - 2, i - 1));
                if (d0 == null || d1 == null) throw new NumberFormatException(String.format("Line #%d: invalid sep value '%s'.", line, number));
                trytes.add(d1.getCode() + d0.getCode());
            }
        } else {
            // decimal number
            long value = Integer.parseInt(number);
            long maxValue = ((long) Math.pow(3.0, length * 6) - 1L) / 2L;
            if (value > maxValue || value < -maxValue)
                throw new NumberFormatException(String.format("Line #%d: value %s too big for %s (±%d).", line, number, longName, maxValue));
            boolean isNegative = value < 0;
            if (isNegative) value = -value;
            int[] digits = new int[length * 6 + 1];
            StringBuilder tryte = new StringBuilder();
            for (int i = 0; i < length * 6;) {
                // compute next ternary digit
                digits[i] += (int) (value % 3L);
                value /= 3L;
                // translate to SBTNS
                if (digits[i] > 1) {
                    digits[i] -= 3;
                    digits[i+1]++;
                }
                if (isNegative) digits[i] *= -1;
                // append digit
                char d;
                if (digits[i] == -1) d = 'λ';
                else d = (char) (digits[i] + 0x30);
                tryte.insert(0, d);
                if (++i % 6 == 0) {
                    trytes.add(tryte.toString());
                    tryte = new StringBuilder();
                }
            }
        }
        return trytes;
    }

}
