package engine;

public enum SepDigit {

    D_F("F", "λλλ"),
    D_G("G", "λλ0"),
    D_H("H", "λλ1"),
    D_K("K", "λ0λ"),
    D_N("N", "λ00"),
    D_P("P", "λ01"),
    D_R("R", "λ1λ"),
    D_S("S", "λ10"),
    D_T("T", "λ11"),
    D_U("U", "0λλ"),
    D_V("V", "0λ0"),
    D_Y("Y", "0λ1"),
    D_Z("Z", "00λ"),
    D_0("0", "000"),
    D_1("1", "001"),
    D_2("2", "01λ"),
    D_3("3", "010"),
    D_4("4", "011"),
    D_5("5", "1λλ"),
    D_6("6", "1λ0"),
    D_7("7", "1λ1"),
    D_8("8", "10λ"),
    D_9("9", "100"),
    D_A("A", "101"),
    D_B("B", "11λ"),
    D_C("C", "110"),
    D_D("D", "111");

    private String symbol;
    private String code;

    SepDigit(String symbol, String code) {
        this.symbol = symbol;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SepDigit parseBySymbol(String symbol) {
        symbol = symbol.toUpperCase();
        for (SepDigit digit : values())
            if (digit.symbol.equals(symbol))
                return digit;
        return null;
    }

}
