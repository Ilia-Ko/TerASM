import java.io.*;
import java.util.*;

class Processor {

    private static final String SECTION_DATA = ".data";
    private static final String SECTION_CODE = ".code";

    private File source, destination;
    private HashMap<String, AsmLine> labels;
    private ArrayList<AsmLine> data, code;

    Processor(File source, File destination) {
        this.source = source;
        this.destination = destination;
        labels = new HashMap<>();
        data = new ArrayList<>();
        code = new ArrayList<>();
    }

    void parse() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(source));
        while (reader.ready()) {
            String line = reader.readLine().trim();
            if (line.equals(SECTION_DATA))
                parseData(reader);
            else if (line.equals(SECTION_CODE))
                parseCode(reader);
        }
        reader.close();
    }
    void compile() throws Exception {
        // compile everything
        int address = 0;
        for (AsmLine codeUnit : code) address = codeUnit.compile(address);
        for (AsmLine dataUnit : data) address = dataUnit.compile(address);
    }
    void output() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(destination));
        int index = 0;

        // output everything
        for (AsmLine line : code)
            for (String tryte : line.output()) {
                writer.write(tryte);
                if (index++ % 27 == 0) writer.newLine();
                else writer.write(' ');
            }
        for (AsmLine line : data)
            for (String tryte : line.output()) {
                writer.write(tryte);
                if (index++ % 27 == 0) writer.newLine();
                else writer.write(' ');
            }

        writer.flush();
        writer.close();
    }

    private void parseData(BufferedReader reader) throws Exception {
        while (reader.ready()) {
            // prepare line
            String line = reader.readLine().trim();
            if (line.contains("//")) line = line.substring(0, line.indexOf('/'));
            if (line.equals(SECTION_CODE)) return;
            if (line.isEmpty()) continue;
            // init data line
            new DataLine(line);
        }
    }
    private void parseCode(BufferedReader reader) throws Exception {
        while (reader.ready()) {
            // prepare line
            String line = reader.readLine().trim();
            if (line.contains("//")) line = line.substring(0, line.indexOf('/')); // remove comments
            if (line.equals(SECTION_CODE)) return;
            if (line.isEmpty()) continue;
            // init data line
            new CodeLine(line);
        }
    }

    // code / data lines
    private abstract class AsmLine {

        String line;
        int address;
        ArrayList<String> trytes;
        ArrayList<Integer> links;

        private AsmLine(String line) throws Exception {
            trytes = new ArrayList<>();
            links = new ArrayList<>();
            address = 0;
            // extract label
            int labelMark = line.indexOf(':');
            if (labelMark != -1) {
                String label = line.substring(0, labelMark);
                if (!isValidLabelName(label)) throw new Exception("Invalid label name: " + label);
                labels.put(label, this);
                line = line.substring(labelMark + 1);
            }
            this.line = line.trim();
        }

        abstract int compile(int address) throws Exception;

        private ArrayList<String> output() {
            for (int link : links) {
                String label = trytes.get(link);
                trytes.set(link, labels.get(label).getAddress());
            }
            return trytes;
        }
        private String getAddress() {
            return DataType.TRYTE.compile(Integer.toString(address)).get(0);
        }

    }
    private class DataLine extends AsmLine {

        private DataLine(String line) throws Exception {
            super(line);
            data.add(this);
        }

        @Override int compile(int address) throws Exception {
            // split to parts
            String[] parts = line.split("[ ,]");
            if (parts.length < 2) throw new Exception("Data line too short: " + line);

            // get data type
            DataType type = DataType.parseByName(parts[0]);
            if (type == null) throw new Exception("Data type not recognized: " + parts[0]);

            // compile trytes and remember links
            for (int i = 1; i < parts.length; i++) {
                if (isValidLabelName(parts[i])) {
                    links.add(trytes.size());
                    trytes.add(parts[i]);
                } else trytes.addAll(type.compile(parts[i]));
            }

            // update address
            this.address = address;
            return address + trytes.size();
        }

    }
    private class CodeLine extends AsmLine {

        private CodeLine(String line) throws Exception {
            super(line);
            code.add(this);
        }

        @Override int compile(int address) throws Exception {
            // split to parts
            String[] parts = line.split("[ ,→]");

            // get code type
            CodeType type = CodeType.parseByName(parts[0]);
            if (type == null) throw new Exception("Instruction not recognized: " + parts[0]);

            // assemble parts
            for (int i = 1; i < parts.length; i++) {
                boolean isAddress = parts[i].startsWith("[") && parts[i].endsWith("]");
                parts[i] = parts[i].replace("[", "");
                parts[i] = parts[i].replace("]", "");
                if (isValidLabelName(parts[i])) type.addImmediate(parts[i], isAddress);
                else {
                    parts[i] = parts[i].toUpperCase();
                    switch (parts[i]) {
                        case "RZ":
                            type.addRegister('λ', isAddress);
                            break;
                        case "R0":
                            type.addRegister('0', isAddress);
                            break;
                        case "R1":
                            type.addRegister('1', isAddress);
                            break;
                        default:
                            type.addImmediate(DataType.TRYTE.compile(parts[i]).get(0), isAddress);
                    }
                }
            }
            trytes = type.compile();

            // update address
            this.address = address;
            return address + trytes.size();
        }

    }
    // code / data types
    private enum DataType {
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
        private static DataType parseByName(String name) {
            name = name.toLowerCase();
            for (DataType type : values())
                if (type.longName.equals(name) || type.shortName.equals(name))
                    return type;
            return null;
        }

        private ArrayList<String> compile(String number) throws NumberFormatException {
            ArrayList<String> trytes = new ArrayList<>(length);

            if (number.startsWith("0t")) {
                // raw ternary number
                number = number.substring(2);
                while (number.length() < length * 6) number = "0".concat(number);
                for (int i = length * 6; i > 0; i -= 6) trytes.add(number.substring(i - 6, i));
            } else if (number.startsWith("0x")) {
                // septemvigesimal number
                number = number.substring(2);
                while (number.length() < length * 2) number = "0".concat(number);
                for (int i = length * 2; i > 0; i -= 2) {
                    SepDigit d0 = SepDigit.parseBySymbol(number.substring(i, i + 1));
                    SepDigit d1 = SepDigit.parseBySymbol(number.substring(i - 1, i));
                    if (d0 == null || d1 == null) throw new NumberFormatException(number.substring(i-1, i+1));
                    trytes.add(d1.code + d0.code);
                }
            } else {
                // decimal number
                long value = Integer.parseInt(number);
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
    @SuppressWarnings("SpellCheckingInspection")
    private enum CodeType {

        MOV("mov") {
            @Override ArrayList<String> compile() {
                ArrayList<String> trytes = new ArrayList<>();
                char r1 = rawRegs[0], r2 = rawRegs[1], r3 = rawRegs[2];

                if (numR == 2 && numI == 0 && adrReg == 0 && adrImm.isEmpty()) {
                    trytes.add("λ0000λ");
                    trytes.add("00000" + r1);
                    trytes.add("0" + r2 + "1000");
                } else if (numR == 1 && numI == 0 && adrReg != 0 && adrImm.isEmpty()) {
                    trytes.add("λ0000λ");
                    trytes.add("0000" + r1 + "0");
                    trytes.add("λ" + r2 + "1000");
                } else if (numR == 1 && numI == 0 && adrReg == 0 && !adrImm.isEmpty()) {
                    trytes.add("10000λ");
                    trytes.add("000000");
                    trytes.add("λ" + r1 + "1010");
                    trytes.add("000000");
                    trytes.add(adrImm);
                }
                // TODO: complete compilation
                return trytes;
            }
        },
        FILLN("filln") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        FILLZ("fillz") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        FILLP("fillp") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        NTI("nti") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        STI("sti") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        PTI("pti") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        NAND("nand") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        NOR("nor") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        NCON("ncon") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        NANY("nany") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        ADD("add") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        ADC("adc") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        TMUL("tmul") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        CMP("cmp") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        JMP("jmp") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        JL("jl") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        JE("je") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        JG("jg") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        JEG("jnl") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        JLG("jne") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        JLE("jng") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        },
        RESTART("restart") {
            @Override
            ArrayList<String> compile() {
                return null;
            }
        };

        private String name;
        protected String rawImms[], adrImm;
        protected char rawRegs[], adrReg;
        protected int numR, numI;

        CodeType(String name) {
            this.name = name;
            rawImms = new String[2];
            rawRegs = new char[3];
            adrReg = 0;
            adrImm = "";
            numR = 0;
            numI = 0;
        }
        private static CodeType parseByName(String name) {
            name = name.toLowerCase();
            for (CodeType codeType : values())
                if (codeType.name.equals(name))
                    return codeType;
            return null;
        }

        private void addImmediate(String imm, boolean isAddress) throws Exception {
            if (isAddress) {
                if (adrImm.isEmpty() && adrReg == 0) adrImm = imm;
                else throw new Exception("There should be no more than one [address] statement in a code line.");
            } else {
                rawImms[numI++] = imm;
            }
        }
        private void addRegister(char reg, boolean isAddress) throws Exception {
            if (isAddress) {
                if (adrReg == 0 && adrImm.isEmpty()) adrReg = reg;
                else throw new Exception("There should be no more than one [address] statement in a code line.");
            } else {
                rawRegs[numR++] = reg;
            }
        }
        abstract ArrayList<String> compile();

    }
    // ternary NS
    private enum SepDigit {
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

        private static SepDigit parseBySymbol(String symbol) {
            symbol = symbol.toUpperCase();
            for (SepDigit digit : values())
                if (digit.symbol.equals(symbol))
                    return digit;
            return null;
        }

    }

    // utils
    private static boolean isValidLabelName(String name) {
        for (int codePoint : name.codePoints().toArray()) {
            if (!Character.isAlphabetic(codePoint) && !Character.isDigit(codePoint) && codePoint != '_') return false;
        }
        return Character.isAlphabetic(name.codePointAt(0));
    }

}
