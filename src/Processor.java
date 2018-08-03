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
            if (line.equals(SECTION_DATA)) parseData(reader);
            else if (line.equals(SECTION_CODE)) parseCode(reader);
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
            if (line.contains(";")) line = line.substring(0, line.indexOf(';')); // remove comments
            if (line.equals(SECTION_CODE)) {
                parseCode(reader);
                continue;
            }
            if (line.isEmpty()) continue;
            // init data line
            new DataLine(line);
        }
    }
    private void parseCode(BufferedReader reader) throws Exception {
        while (reader.ready()) {
            // prepare line
            String line = reader.readLine().trim();
            if (line.contains(";")) line = line.substring(0, line.indexOf(';')); // remove comments
            if (line.equals(SECTION_DATA)) {
                parseData(reader);
                continue;
            }
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

        private AsmLine(String line) throws Exception {
            trytes = new ArrayList<>();
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
            // dereference labels if present
            for (int i = 0; i < trytes.size(); i++) {
                String tryte = trytes.get(i);
                if (isValidLabelName(tryte))
                    trytes.set(i, labels.get(tryte).getAddress());
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
                if (parts[i].isEmpty()) continue;
                if (isValidLabelName(parts[i])) trytes.add(parts[i]);
                else trytes.addAll(type.compile(parts[i]));
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

            // assemble
            ArrayList<String> operands = new ArrayList<>();
            for (int i = 1; i < parts.length; i++)
                if (!parts[i].isEmpty()) operands.add(parts[i]);
            trytes = type.compile(parts[0], operands, line.contains("→"));

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
                    SepDigit d0 = SepDigit.parseBySymbol(number.substring(i - 1, i));
                    SepDigit d1 = SepDigit.parseBySymbol(number.substring(i - 2, i - 1));
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
    private enum CodeType {

        MOV("mov") {
            @Override ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                // extract SRC and DST
                if (!hasDst) throw new Exception("MOV instruction must have destination.");
                if (ops.size() != 2) throw new Exception("MOV instruction must have exactly two operands: source and destination.");
                String src = ops.get(0);
                String dst = ops.get(1);
                boolean srcAdr = src.startsWith("[") && src.endsWith("]");
                boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
                if (srcAdr && dstAdr) throw new Exception("MOV mem → mem not allowed.");
                if (srcAdr) src = CodeType.dereference(src);
                if (dstAdr) dst = CodeType.dereference(dst);
                boolean srcReg = isValidRegName(src);
                boolean srcImm = !srcReg && isValidLabelName(src);
                boolean dstReg = isValidRegName(dst);
                boolean dstImm = !dstReg && isValidLabelName(dst);
                if (srcReg) src = parseReg(src);
                if (dstReg) dst = parseReg(dst);
                if (!srcImm && !srcReg) { // src is raw number
                    src = DataType.TRYTE.compile(src).get(0);
                    srcImm = true;
                }
                if (!dstImm && !dstReg) { // dst is raw number
                    dst = DataType.TRYTE.compile(dst).get(0);
                    dstImm = true;
                }
                // assemble
                ArrayList<String> trytes = new ArrayList<>();
                if (srcReg & !srcAdr & dstReg & !dstAdr) {
                    // mov b -> a
                    trytes.add(CodeType.asm("λ", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", src));
                    trytes.add(CodeType.asm("0", dst, "1", "0", "0", "0"));
                } else if (srcReg & srcAdr & dstReg & !dstAdr) {
                    // mov [b] -> a
                    trytes.add(CodeType.asm("λ", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", src, "0"));
                    trytes.add(CodeType.asm("λ", dst, "1", "0", "0", "0"));
                } else if (srcImm & srcAdr & dstReg & !dstAdr) {
                    // mov [i1] -> a
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(CodeType.asm("λ", dst, "1", "0", "1", "0"));
                    trytes.add(src);
                } else if (srcReg & !srcAdr & dstReg & dstAdr) {
                    // mov b -> [a]
                    trytes.add(CodeType.asm("λ", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", dst, src));
                    trytes.add(CodeType.asm("0", "0", "0", "λ", "0", "1"));
                } else if (srcImm & !srcAdr & dstReg & dstAdr) {
                    // mov i2 -> [a]
                    trytes.add(CodeType.asm("1", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", dst, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "1"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(src);
                } else if (srcReg & !srcAdr & dstImm & dstAdr) {
                    // mov a -> [i1]
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", src));
                    trytes.add(CodeType.asm("0", "0", "0", "λ", "1", "1"));
                    trytes.add(dst);
                } else if (srcImm & !srcAdr & dstImm & dstAdr) {
                    // mov i2 -> [i1]
                    trytes.add(CodeType.asm("1", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "1", "1"));
                    trytes.add(dst);
                    trytes.add(src);
                } else throw new Exception("MOV " + ops.get(0) + " → " + ops.get(1) + " not allowed.");
                return trytes;
            }
        },
        FILLX("fill") {
            @Override ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                // extract VAL, OP1 and OP2
                if (hasDst) throw new Exception("FILLx instruction must not have '→' symbol, because every its operand is destination itself.");
                realName = realName.toLowerCase();
                String val;
                if ("filln".endsWith(realName)) val = "λ";
                else if ("fillz".endsWith(realName)) val = "0";
                else if ("fillp".endsWith(realName)) val = "1";
                else throw new Exception("Unknown FILLx type: " + realName);
                if (ops.size() > 2 || ops.size() == 0) throw new Exception("FILLx instruction must have either one or two operands.");
                boolean twoOps = ops.size() == 2;
                String op1 = ops.get(0);
                String op2 = twoOps ? ops.get(1) : "";
                boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
                boolean op2Adr = twoOps && (op2.startsWith("[") && op2.endsWith("]"));
                if (op1Adr) op1 = CodeType.dereference(op1);
                if (twoOps && op2Adr) op2 = CodeType.dereference(op2);
                boolean op1Reg = isValidRegName(op1);
                boolean op1Imm = !op1Reg && isValidLabelName(op1);
                boolean op2Reg = isValidRegName(op2);
                boolean op2Imm = !op2Reg && isValidLabelName(op2);
                if (op1Reg) op1 = parseReg(op1);
                if (twoOps && op2Reg) op2 = parseReg(op2);
                if (!op1Imm && !op1Reg) {
                    op1 = DataType.TRYTE.compile(op1).get(0);
                    op1Imm = true;
                }
                if (twoOps && !op2Imm && !op2Reg) {
                    op2 = DataType.TRYTE.compile(op2).get(0);
                    op2Imm = true;
                }
                // assemble
                ArrayList<String> trytes = new ArrayList<>();
                if (op1Reg & !op1Adr & !twoOps) {
                    // fillx a
                    trytes.add(CodeType.asm("λ", "0", val, "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(CodeType.asm("0", op1, "λ", "0", "0", "0"));
                } else if (op1Reg & op1Adr & !twoOps) {
                    // fillx [a]
                    trytes.add(CodeType.asm("λ", "0", val, "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", op1, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                } else if (op1Imm & op1Adr & !twoOps) {
                    // fillx [i1]
                    trytes.add(CodeType.asm("0", "0", val, "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "1", "λ"));
                    trytes.add(op1);
                } else if (op1Reg & !op1Adr & twoOps & op2Reg & op2Adr) {
                    // fill a, [b]
                    trytes.add(CodeType.asm("λ", "0", val, "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", op2, "0"));
                    trytes.add(CodeType.asm("0", op1, "λ", "0", "0", "λ"));
                } else if (op1Reg & !op1Adr & twoOps & op2Imm & op2Adr) {
                    // fill a, [i1]
                    trytes.add(CodeType.asm("0", "0", val, "0", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(CodeType.asm("0", op1, "λ", "0", "1", "λ"));
                    trytes.add(op1);
                } else throw new Exception("FILLx " + ops.get(0) + ", " + (twoOps ? ops.get(1) : "") + " not allowed.");
                return trytes;
            }
        },
        XTI("ti") {
            @Override
            ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                // extract ACT, SRC and DST
                if (!hasDst) throw new Exception("xTI instruction must have destination.");
                realName = realName.toLowerCase();
                String act;
                if ("nti".equals(realName)) act = "λ";
                else if ("sti".equals(realName)) act = "0";
                else if ("pti".equals(realName)) act = "1";
                else throw new Exception("Unknown xTI type: " + realName);
                if (ops.size() != 2) throw new Exception("xTI instruction must have exactly two operands: source and destination.");
                String src = ops.get(0);
                String dst = ops.get(1);
                boolean srcAdr = src.startsWith("[") && src.endsWith("]");
                boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
                if (srcAdr && dstAdr) throw new Exception("'xTI mem → mem' not allowed.");
                if (srcAdr) src = CodeType.dereference(src);
                if (dstAdr) dst = CodeType.dereference(dst);
                boolean srcReg = isValidRegName(src);
                boolean srcImm = !srcReg && isValidLabelName(src);
                boolean dstReg = isValidRegName(dst);
                boolean dstImm = !dstReg && isValidLabelName(dst);
                if (srcReg) src = parseReg(src);
                if (dstReg) dst = parseReg(dst);
                if (!srcImm && !srcReg) { // src is raw number
                    src = DataType.TRYTE.compile(src).get(0);
                    srcImm = true;
                }
                if (!dstImm && !dstReg) { // dst is raw number
                    dst = DataType.TRYTE.compile(dst).get(0);
                    dstImm = true;
                }
                // assemble
                // trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                ArrayList<String> trytes = new ArrayList<>();
                if (srcReg & !srcAdr & dstReg & !dstAdr) {
                    // xti a -> b
                    trytes.add(CodeType.asm("λ", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("λ", act, "0", "0", "0", src));
                    trytes.add(CodeType.asm("1", dst, "1", "1", "0", "0"));
                } else if (srcReg & !srcAdr & dstReg & dstAdr) {
                    // xti a -> [b]
                    trytes.add(CodeType.asm("λ", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("λ", act, "0", "0", dst, src));
                    trytes.add(CodeType.asm("0", "0", "0", "1", "0", "1"));
                } else if (srcReg & !srcAdr & dstImm & dstAdr) {
                    // xti a -> [i1]
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("λ", act, "0", "0", "0", src));
                    trytes.add(CodeType.asm("0", "0", "0", "1", "1", "1"));
                    trytes.add(dst);
                } else if (srcReg & srcAdr & dstReg & !dstAdr) {
                    // xti [a] -> b
                    trytes.add(CodeType.asm("λ", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("λ", act, "0", "0", src, "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "0", "0"));
                } else if (srcImm & srcAdr & dstReg & !dstAdr) {
                    // xti [i1] -> b
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm("λ", act, "0", "0", "0", "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "1", "0"));
                    trytes.add(src);
                } else throw new Exception("xTI " + ops.get(0) + " → " + ops.get(1) + " not allowed.");
                return trytes;
            }
        },
        ADX_INSTR("ad") {
            @Override ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                // extract ACT, OP1, OP2 and DST
                if (!hasDst) throw new Exception("ADx instruction must have destination.");
                if (ops.size() != 3) throw new Exception("ADx instruction must have exactly three operands: two arguments and destination.");
                realName = realName.toLowerCase();
                String act;
                if ("add".equals(realName)) act = "λ";
                else if ("adc".equals(realName)) act = "1";
                else throw new Exception("Unknown ADx type: " + realName);
                String op1 = ops.get(0);
                String op2 = ops.get(1);
                String dst = ops.get(2);
                boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
                boolean op2Adr = op2.startsWith("[") && op2.endsWith("]");
                boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
                if (op1Adr && op2Adr || op2Adr && dstAdr || op1Adr && dstAdr) throw new Exception("ADx can have no more than one [address] operand.");
                if (op1Adr) op1 = CodeType.dereference(op1);
                if (op2Adr) op2 = CodeType.dereference(op2);
                if (dstAdr) dst = CodeType.dereference(dst);
                boolean op1Reg = isValidRegName(op1);
                boolean op1Imm = !op1Reg && isValidLabelName(op1);
                boolean op2Reg = isValidRegName(op2);
                boolean op2Imm = !op2Reg && isValidLabelName(op2);
                boolean dstReg = isValidRegName(dst);
                boolean dstImm = !dstReg && isValidLabelName(dst);
                if (op1Reg) op1 = parseReg(op1);
                if (op2Reg) op2 = parseReg(op2);
                if (dstReg) dst = parseReg(dst);
                if (!op1Imm && !op1Reg) { // op1 is raw number
                    op1 = DataType.TRYTE.compile(op1).get(0);
                    op1Imm = true;
                }
                if (!op2Imm && !op2Reg) { // op2 is raw number
                    op2 = DataType.TRYTE.compile(op2).get(0);
                    op2Imm = true;
                }
                if (!dstImm && !dstReg) { // dst is raw number
                    dst = DataType.TRYTE.compile(dst).get(0);
                    dstImm = true;
                }

                // assemble
                // trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                ArrayList<String> trytes = new ArrayList<>();
                if (op1Reg & !op1Adr & op2Reg & !op2Adr & dstReg & !dstAdr) {
                    // adx a, b -> c
                    trytes.add(CodeType.asm("λ", "0", "0", act, "0", "λ"));
                    trytes.add(CodeType.asm("1", "0", "0", "0", op2, op1));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "0", "0"));
                } else if (op1Reg & !op1Adr & op2Reg & !op2Adr & dstImm & dstAdr) {
                    // adx a, b -> [i1]
                    trytes.add(CodeType.asm("0", "0", "0", act, "0", "λ"));
                    trytes.add(CodeType.asm("1", "0", "0", "0", op2, op1));
                    trytes.add(CodeType.asm("0", "0", "0", "1", "1", "1"));
                    trytes.add(dst);
                } else if (op1Reg & !op1Adr & op2Imm & !op2Adr & dstReg & !dstAdr) {
                    // adx a, i1 -> b
                    trytes.add(CodeType.asm("0", "0", "0", act, "0", "λ"));
                    trytes.add(CodeType.asm("1", "0", "0", "1", "0", op1));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "0", "0"));
                    trytes.add(op2);
                } else if (op1Reg & !op1Adr & op2Imm & !op2Adr & dstReg & dstAdr) {
                    // adx a, i1 -> [b]
                    trytes.add(CodeType.asm("0", "0", "0", act, "0", "λ"));
                    trytes.add(CodeType.asm("1", "0", "0", "0", dst, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "1", "0", "1"));
                    trytes.add(op2);
                } else if (op1Reg & op1Adr & op2Imm & !op2Adr & dstReg & !dstAdr) {
                    // adx [a], i1 -> b
                    trytes.add(CodeType.asm("0", "0", "0", act, "0", "λ"));
                    trytes.add(CodeType.asm("1", "0", "λ", "1", op1, "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "0", "0"));
                    trytes.add(op2);
                } else if (op1Imm & op1Adr & op2Imm & !op2Adr & dstReg & !dstAdr) {
                    // adx [i1], i2 -> a
                    trytes.add(CodeType.asm("1", "0", "0", act, "0", "λ"));
                    trytes.add(CodeType.asm("1", "0", "λ", "λ", "0", "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "1", "0"));
                    trytes.add(op1);
                    trytes.add(op2);
                } else if (op1Imm & op1Adr & op2Reg & !op2Adr & dstReg & !dstAdr) {
                    // adx [i1], a -> b
                    trytes.add(CodeType.asm("0", "0", "0", act, "0", "λ"));
                    trytes.add(CodeType.asm("1", "0", "λ", "0", op2, "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "1", "0"));
                    trytes.add(op1);
                } else throw new Exception("ADx " + ops.get(0) + ", " + ops.get(1) + " → " + ops.get(2) + " not allowed.");
                return trytes;
            }
        },
        CMP("cmp") {
            @Override ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                // extract SRC and DST
                if (hasDst) throw new Exception("CMP must not have destination.");
                if (ops.size() != 2) throw new Exception("CMP must have exactly two operands.");
                String op1 = ops.get(0);
                String op2 = ops.get(1);
                boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
                boolean op2Adr = op2.startsWith("[") && op2.endsWith("]");
                if (op1Adr && op2Adr) throw new Exception("CMP mem, mem not allowed.");
                if (op1Adr) op1 = CodeType.dereference(op1);
                if (op2Adr) op2 = CodeType.dereference(op2);
                boolean op1Reg = isValidRegName(op1);
                boolean op1Imm = !op1Reg && isValidLabelName(op1);
                boolean op2Reg = isValidRegName(op2);
                boolean op2Imm = !op2Reg && isValidLabelName(op2);
                if (op1Reg) op1 = parseReg(op1);
                if (op2Reg) op2 = parseReg(op2);
                if (!op1Imm && !op1Reg) { // op1 is raw number
                    op1 = DataType.TRYTE.compile(op1).get(0);
                    op1Imm = true;
                }
                if (!op2Imm && !op2Reg) { // op2 is raw number
                    op2 = DataType.TRYTE.compile(op2).get(0);
                    op2Imm = true;
                }
                // assemble
                ArrayList<String> trytes = new ArrayList<>();
                if (op1Reg & !op1Adr & op2Reg & !op2Adr) {
                    // cmp a, b
                    trytes.add(CodeType.asm("λ", "0", "0", "λ", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", op2, op1));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                } else if (op1Reg & !op1Adr & op2Imm & !op2Adr) {
                    // cmp a, i1
                    trytes.add(CodeType.asm("0", "0", "0", "λ", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "0", "1", "0", op1));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(op2);
                } else if (op1Reg & op1Adr & op2Imm & !op2Adr) {
                    // cmp [a], i1
                    trytes.add(CodeType.asm("0", "0", "0", "λ", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "λ", "1", op1, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(op2);
                } else if (op1Imm & op1Adr & op2Reg & !op2Adr) {
                    // cmp [i1], a
                    trytes.add(CodeType.asm("0", "0", "0", "λ", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "λ", "0", op2, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "1", "0"));
                    trytes.add(op1);
                } else if (op1Imm & op1Adr & op2Imm & !op2Adr) {
                    // cmp [i1], i2
                    trytes.add(CodeType.asm("1", "0", "0", "λ", "0", "λ"));
                    trytes.add(CodeType.asm("0", "0", "λ", "λ", "0", "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "1", "0"));
                    trytes.add(op1);
                    trytes.add(op2);
                }
                return trytes;
            }
        },
        JXX("j") {
            @Override ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                // extract TYPE, CON, OP1 and OP2
                if (hasDst) throw new Exception("Jxx instruction must not have '→' symbol, because every its operand is destination itself.");
                realName = realName.toLowerCase();
                String typ, con;
                if ("jmp".equals(realName)) {
                    typ = "λ";
                    con = "0";
                } else if ("jl".equals(realName)) {
                    typ = "0";
                    con = "λ";
                } else if ("je".equals(realName)) {
                    typ = "0";
                    con = "0";
                } else if ("jg".equals(realName)) {
                    typ = "0";
                    con = "1";
                } else if ("jeg".equals(realName) || "jnl".equals(realName)) {
                    typ = "1";
                    con = "λ";
                } else if ("jlg".equals(realName) || "jne".equals(realName)) {
                    typ = "1";
                    con = "0";
                } else if ("jle".equals(realName) || "jng".equals(realName)) {
                    typ = "1";
                    con = "1";
                } else throw new Exception("Unknown Jxx type: " + realName);
                if (ops.size() > 2 || ops.size() == 0) throw new Exception("Jxx instruction must have either one or two operands.");
                boolean twoOps = ops.size() == 2;
                String op1 = ops.get(0);
                String op2 = twoOps ? ops.get(1) : "";
                boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
                boolean op2Adr = twoOps && (op2.startsWith("[") && op2.endsWith("]"));
                if (op1Adr || op2Adr) throw new Exception("Jxx operands can not be [addresses].");
                boolean op1Reg = isValidRegName(op1);
                boolean op1Imm = !op1Reg && isValidLabelName(op1);
                boolean op2Reg = isValidRegName(op2);
                boolean op2Imm = !op2Reg && isValidLabelName(op2);
                if (op1Reg) op1 = parseReg(op1);
                if (twoOps && op2Reg) op2 = parseReg(op2);
                if (!op1Imm && !op1Reg) {
                    op1 = DataType.TRYTE.compile(op1).get(0);
                    op1Imm = true;
                }
                if (twoOps && !op2Imm && !op2Reg) {
                    op2 = DataType.TRYTE.compile(op2).get(0);
                    op2Imm = true;
                }
                // assemble
                ArrayList<String> trytes = new ArrayList<>();
                if (op1Reg & !twoOps) {
                    // jxx a
                    trytes.add(CodeType.asm("λ", "1", "0", "0", con, typ));
                    trytes.add(CodeType.asm("1", "0", "1", "0", op1, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                } else if (op1Imm & !twoOps) {
                    // jxx i1
                    trytes.add(CodeType.asm("0", "1", "0", "0", con, typ));
                    trytes.add(CodeType.asm("1", "0", "1", "1", "0", "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(op1);
                } else if (op1Imm & op2Reg) {
                    // jxx i1, a
                    trytes.add(CodeType.asm("0", "1", "0", "0", con, typ));
                    trytes.add(CodeType.asm("1", "0", "1", "0", op1, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(op1);
                } else if (op1Imm & op2Imm) {
                    // jxx i1, i2
                    trytes.add(CodeType.asm("1", "1", "0", "0", con, typ));
                    trytes.add(CodeType.asm("1", "0", "1", "λ", "0", "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                    trytes.add(op1);
                    trytes.add(op2);
                }
                return trytes;
            }
        },
        RESTART("restart") {
            @Override ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                if (hasDst) throw new Exception("RESTART instruction cannot have destination.");
                if (ops.size() != 0) throw new Exception("RESTART instruction cannot have operands.");
                // assemble
                ArrayList<String> trytes = new ArrayList<>();
                trytes.add(CodeType.asm("λ", "λ", "0", "λ", "0", "λ"));
                trytes.add(CodeType.asm("1", "0", "0", "0", "0", "0"));
                trytes.add(CodeType.asm("0", "0", "λ", "0", "0", "0"));
                return trytes;
            }
        },
        ALU_INSTR("") {
            @Override ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
                // extract AL0, AL1, OP1, OP2 and DST
                if (!hasDst) throw new Exception("ALU instruction must have destination.");
                if (ops.size() != 3) throw new Exception("ALU instruction must have exactly three operands: two arguments and destination.");
                realName = realName.toLowerCase();
                String al0, al1;
                if ("nand".equals(realName)) {
                    al0 = "0";
                    al1 = "λ";
                } else if ("nor".equals(realName)) {
                    al0 = "0";
                    al1 = "0";
                } else if ("ncon".equals(realName)) {
                    al0 = "0";
                    al1 = "1";
                } else if ("nany".equals(realName)) {
                    al0 = "1";
                    al1 = "λ";
                } else if ("tmul".equals(realName)) {
                    al0 = "1";
                    al1 = "1";
                } else throw new Exception("Unknown ALU instruction: " + realName);
                String op1 = ops.get(0);
                String op2 = ops.get(1);
                String dst = ops.get(2);
                boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
                boolean op2Adr = op2.startsWith("[") && op2.endsWith("]");
                boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
                if (op1Adr && op2Adr || op2Adr && dstAdr || op1Adr && dstAdr) throw new Exception("ALU instruction can have no more than one [address] operand.");
                if (op1Adr) op1 = CodeType.dereference(op1);
                if (op2Adr) op2 = CodeType.dereference(op2);
                if (dstAdr) dst = CodeType.dereference(dst);
                boolean op1Reg = isValidRegName(op1);
                boolean op1Imm = !op1Reg && isValidLabelName(op1);
                boolean op2Reg = isValidRegName(op2);
                boolean op2Imm = !op2Reg && isValidLabelName(op2);
                boolean dstReg = isValidRegName(dst);
                boolean dstImm = !dstReg && isValidLabelName(dst);
                if (op1Reg) op1 = parseReg(op1);
                if (op2Reg) op2 = parseReg(op2);
                if (dstReg) dst = parseReg(dst);
                if (!op1Imm && !op1Reg) { // op1 is raw number
                    op1 = DataType.TRYTE.compile(op1).get(0);
                    op1Imm = true;
                }
                if (!op2Imm && !op2Reg) { // op2 is raw number
                    op2 = DataType.TRYTE.compile(op2).get(0);
                    op2Imm = true;
                }
                if (!dstImm && !dstReg) { // dst is raw number
                    dst = DataType.TRYTE.compile(dst).get(0);
                    dstImm = true;
                }
                // assemble
                ArrayList<String> trytes = new ArrayList<>();
                if (op1Reg & !op1Adr & op2Reg & !op2Adr & dstReg & !dstAdr) {
                    // xxxx a, b -> c
                    trytes.add(CodeType.asm("λ", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm(al0, al1, "0", "0", op2, op1));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "0", "0"));
                } else if (op1Reg & !op1Adr & op2Reg & !op2Adr & dstImm & dstAdr) {
                    // xxxx a, b -> [i1]
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm(al0, al1, "0", "0", op2, op1));
                    trytes.add(CodeType.asm("0", "0", "0", "1", "1", "1"));
                    trytes.add(dst);
                } else if (op1Reg & !op1Adr & op2Imm & !op2Adr & dstReg & !dstAdr) {
                    // xxxx a, i1 -> b
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm(al0, al1, "0", "1", "0", op1));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "0", "0"));
                    trytes.add(op2);
                } else if (op1Reg & !op1Adr & op2Imm & !op2Adr & dstReg & dstAdr) {
                    // xxxx a, i1 -> [b]
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm(al0, al1, "0", "0", dst, "0"));
                    trytes.add(CodeType.asm("0", "0", "0", "1", "0", "1"));
                    trytes.add(op2);
                } else if (op1Reg & op1Adr & op2Imm & !op2Adr & dstReg & !dstAdr) {
                    // xxxx [a], i1 -> b
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm(al0, al1, "λ", "1", op1, "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "0", "0"));
                    trytes.add(op2);
                } else if (op1Imm & op1Adr & op2Imm & !op2Adr & dstReg & !dstAdr) {
                    // xxxx [i1], i2 -> a
                    trytes.add(CodeType.asm("1", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm(al0, al1, "λ", "λ", "0", "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "1", "0"));
                    trytes.add(op1);
                    trytes.add(op2);
                } else if (op1Imm & op1Adr & op2Reg & !op2Adr & dstReg & !dstAdr) {
                    // xxxx [i1], a -> b
                    trytes.add(CodeType.asm("0", "0", "0", "0", "0", "λ"));
                    trytes.add(CodeType.asm(al0, al1, "λ", "0", op2, "0"));
                    trytes.add(CodeType.asm("1", dst, "1", "0", "1", "0"));
                    trytes.add(op1);
                } else throw new Exception("ADx " + ops.get(0) + ", " + ops.get(1) + " → " + ops.get(2) + " not allowed.");
                return trytes;
            }
        };

        private String name;

        CodeType(String name) {
            this.name = name;
        }
        private static CodeType parseByName(String name) {
            name = name.toLowerCase();
            for (CodeType codeType : values())
                if (name.startsWith(codeType.name) || name.endsWith(codeType.name))
                    return codeType;
            return null;
        }

        private static String dereference(String operand) {
            return operand.replaceAll("[\\[\\]]", "");
        }
        private static String asm(String... trits) {
            StringBuilder builder = new StringBuilder(6);
            for (String arg : trits) builder.append(arg);
            return builder.toString();
        }

        abstract ArrayList<String> compile(String realName, ArrayList<String> operands, boolean hasDestination) throws Exception;

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
    private static boolean isValidRegName(String reg) {
        String r = reg.toLowerCase();
        return "rz".equals(r) || "r0".equals(r) || "r1".equals(r);
    }
    private static String parseReg(String reg) {
        String r = reg.toLowerCase();
        switch (r) {
            case "rz":
                return "λ";
            case "r0":
                return "0";
            case "r1":
                return "1";
            default:
                return "?";
        }
    }

}
