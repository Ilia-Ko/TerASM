package types;

import engine.Processor;

import java.util.ArrayList;

public enum CodeType {

    MOV("mov") {
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            // extract SRC and DST
            if (!hasDst) throw new Exception(String.format("Line #%d: <%s> must have destination.", line, name));
            if (ops.size() != 2)
                throw new Exception(String.format("Line #%d: <%s> must have exactly two operands: source and destination.", line, name));
            String src = ops.get(0);
            String dst = ops.get(1);
            boolean srcAdr = src.startsWith("[") && src.endsWith("]");
            boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
            if (srcAdr) src = CodeType.dereference(src);
            if (dstAdr) dst = CodeType.dereference(dst);
            boolean srcReg = Processor.isValidRegName(src);
            boolean srcImm = !srcReg && Processor.isValidLabelName(src);
            boolean dstReg = Processor.isValidRegName(dst);
            boolean dstImm = !dstReg && Processor.isValidLabelName(dst);
            if (srcReg) src = Processor.parseReg(src);
            if (dstReg) dst = Processor.parseReg(dst);
            if (!srcImm && !srcReg) { // src is raw number
                src = DataType.TRYTE.compile(src, line).get(0);
                srcImm = true;
            }
            if (!dstImm && !dstReg) { // dst is raw number
                dst = DataType.TRYTE.compile(dst, line).get(0);
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
            } else throw new Exception(String.format("Line #%d: <%s %s → %s> not allowed.", line, name, ops.get(0), ops.get(1)));
            return trytes;
        }
    },
    FILLX("fill") {
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            // extract VAL, OP1 and OP2
            if (hasDst) throw new Exception(String.format("Line #%d: <%s> cannot have destination, " +
                        "because each operand is a kind of destination itself.", line, name));
            name = name.toLowerCase();
            String val;
            if ("filln".endsWith(name)) val = "λ";
            else if ("fillz".endsWith(name)) val = "0";
            else if ("fillp".endsWith(name)) val = "1";
            else throw new Exception(String.format("Line #%d: invalid FILLx type: <%s> (allowed: FILLN, FILLZ, FILLP).", line, name));
            if (ops.size() > 2 || ops.size() == 0)
                throw new Exception(String.format("Line #%d: <%s> must have either one or two operands.", line, name));
            boolean twoOps = ops.size() == 2;
            String op1 = ops.get(0);
            String op2 = twoOps ? ops.get(1) : "";
            boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
            boolean op2Adr = twoOps && (op2.startsWith("[") && op2.endsWith("]"));
            if (op1Adr) op1 = CodeType.dereference(op1);
            if (twoOps && op2Adr) op2 = CodeType.dereference(op2);
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = twoOps && Processor.isValidRegName(op2);
            boolean op2Imm = twoOps && !op2Reg && Processor.isValidLabelName(op2);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (twoOps && op2Reg) op2 = Processor.parseReg(op2);
            if (!op1Imm && !op1Reg) {
                op1 = DataType.TRYTE.compile(op1, line).get(0);
                op1Imm = true;
            }
            if (twoOps && !op2Imm && !op2Reg) {
                op2 = DataType.TRYTE.compile(op2, line).get(0);
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
            } else {
                String form = "Line #%d: <%s %s";
                if (twoOps) form += ", ";
                form += "%s> not allowed.";
                throw new Exception(String.format(form, line, name, ops.get(0), (twoOps ? ops.get(1) : "")));
            }
            return trytes;
        }
    },
    XTI("ti") {
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            // extract ACT, SRC and DST
            if (!hasDst) throw new Exception(String.format("Line #%d: <%s> must have destination.", line, name));
            name = name.toLowerCase();
            String act;
            if ("nti".equals(name)) act = "λ";
            else if ("sti".equals(name)) act = "0";
            else if ("pti".equals(name)) act = "1";
            else throw new Exception(String.format("Line #%d: invalid xTI type: <%s> (allowed: NTI, STI, PTI).", line, name));
            if (ops.size() != 2)
                throw new Exception(String.format("Line #%d: <%s> must have exactly two operands: source and destination.", line, name));
            String src = ops.get(0);
            String dst = ops.get(1);
            boolean srcAdr = src.startsWith("[") && src.endsWith("]");
            boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
            if (srcAdr) src = CodeType.dereference(src);
            if (dstAdr) dst = CodeType.dereference(dst);
            boolean srcReg = Processor.isValidRegName(src);
            boolean srcImm = !srcReg && Processor.isValidLabelName(src);
            boolean dstReg = Processor.isValidRegName(dst);
            boolean dstImm = !dstReg && Processor.isValidLabelName(dst);
            if (srcReg) src = Processor.parseReg(src);
            if (dstReg) dst = Processor.parseReg(dst);
            if (!srcImm && !srcReg) { // src is raw number
                src = DataType.TRYTE.compile(src, line).get(0);
                srcImm = true;
            }
            if (!dstImm && !dstReg) { // dst is raw number
                dst = DataType.TRYTE.compile(dst, line).get(0);
                dstImm = true;
            }
            // assemble
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
            } else throw new Exception(String.format("Line #%d: <%s %s> not allowed.", line, name, ops.get(0)));
            return trytes;
        }
    },
    ADX("ad") {
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            // extract ACT, OP1, OP2 and DST
            if (!hasDst) throw new Exception(String.format("Line #%d: <%s> must have destination.", line, name));
            if (ops.size() != 3)
                throw new Exception(String.format("Line #%d: <%s> must have exactly three operands: two arguments and destination.", line, name));
            name = name.toLowerCase();
            String act;
            if ("add".equals(name)) act = "λ";
            else if ("adc".equals(name)) act = "1";
            else throw new Exception(String.format("Line #%d: invalid ADx type: <%s> (allowed: ADD, ADC).", line, name));
            String op1 = ops.get(0);
            String op2 = ops.get(1);
            String dst = ops.get(2);
            boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
            boolean op2Adr = op2.startsWith("[") && op2.endsWith("]");
            boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
            if (op1Adr) op1 = CodeType.dereference(op1);
            if (op2Adr) op2 = CodeType.dereference(op2);
            if (dstAdr) dst = CodeType.dereference(dst);
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = Processor.isValidRegName(op2);
            boolean op2Imm = !op2Reg && Processor.isValidLabelName(op2);
            boolean dstReg = Processor.isValidRegName(dst);
            boolean dstImm = !dstReg && Processor.isValidLabelName(dst);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (op2Reg) op2 = Processor.parseReg(op2);
            if (dstReg) dst = Processor.parseReg(dst);
            if (!op1Imm && !op1Reg) { // op1 is raw number
                op1 = DataType.TRYTE.compile(op1, line).get(0);
                op1Imm = true;
            }
            if (!op2Imm && !op2Reg) { // op2 is raw number
                op2 = DataType.TRYTE.compile(op2, line).get(0);
                op2Imm = true;
            }
            if (!dstImm && !dstReg) { // dst is raw number
                dst = DataType.TRYTE.compile(dst, line).get(0);
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
            } else if (op1Reg & !op1Adr & op2Imm & !op2Adr & dstImm & dstAdr) {
                // adx a, i2 -> [i1]
                trytes.add(CodeType.asm("1", "0", "0", act, "0", "λ"));
                trytes.add(CodeType.asm("1", "0", "0", "λ", "0", op1));
                trytes.add(CodeType.asm("0", "0", "0", "1", "1", "1"));
                trytes.add(dst);
                trytes.add(op2);
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
            } else throw new Exception(String.format("Line #%d: <%s %s, %s → %s> not allowed.", line, name, ops.get(0), ops.get(1), ops.get(2)));
            return trytes;
        }
    },
    CMP("cmp") {
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            // extract SRC and DST
            if (hasDst) throw new Exception(String.format("Line #%d: <%s> cannot have destination, because result is stored in ZF.", line, name));
            if (ops.size() != 2) throw new Exception(String.format("Line #%d: <%s> must have exactly two operands.", line, name));
            String op1 = ops.get(0);
            String op2 = ops.get(1);
            boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
            boolean op2Adr = op2.startsWith("[") && op2.endsWith("]");
            if (op1Adr) op1 = CodeType.dereference(op1);
            if (op2Adr) op2 = CodeType.dereference(op2);
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = Processor.isValidRegName(op2);
            boolean op2Imm = !op2Reg && Processor.isValidLabelName(op2);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (op2Reg) op2 = Processor.parseReg(op2);
            if (!op1Imm && !op1Reg) { // op1 is raw number
                op1 = DataType.TRYTE.compile(op1, line).get(0);
                op1Imm = true;
            }
            if (!op2Imm && !op2Reg) { // op2 is raw number
                op2 = DataType.TRYTE.compile(op2, line).get(0);
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
            } else throw new Exception(String.format("Line #%d: <%s %s, %s> not allowed.", line, name, ops.get(0), ops.get(1)));
            return trytes;
        }
    },
    JXX("j") {
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            // extract TYPE, CON, OP1 and OP2
            if (hasDst) throw new Exception(String.format("Line #%d: <%s> cannot have destination, " +
                    "because each operand is a kind of destination itself.", line, name));
            name = name.toLowerCase();
            String typ, con;
            if ("jmp".equals(name)) {
                typ = "λ";
                con = "0";
            } else if ("jl".equals(name)) {
                typ = "0";
                con = "λ";
            } else if ("je".equals(name)) {
                typ = "0";
                con = "0";
            } else if ("jg".equals(name)) {
                typ = "0";
                con = "1";
            } else if ("jeg".equals(name) || "jnl".equals(name)) {
                typ = "1";
                con = "λ";
            } else if ("jlg".equals(name) || "jne".equals(name)) {
                typ = "1";
                con = "0";
            } else if ("jle".equals(name) || "jng".equals(name)) {
                typ = "1";
                con = "1";
            } else throw new Exception(String.format("Line #%d: invalid Jxx type: <%s> " +
                    "(allowed: JMP, JL, JE, JG, JNL, JNE, JNG, JLE, JLG, JEG).", line, name));
            if (ops.size() > 2 || ops.size() == 0) throw new Exception(String.format("Line #%d: <%s> must have either one or two operands.", line, name));
            boolean twoOps = ops.size() == 2;
            String op1 = ops.get(0);
            String op2 = twoOps ? ops.get(1) : "";
            boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
            boolean op2Adr = twoOps && (op2.startsWith("[") && op2.endsWith("]"));
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = twoOps && Processor.isValidRegName(op2);
            boolean op2Imm = twoOps && !op2Reg && Processor.isValidLabelName(op2);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (twoOps && op2Reg) op2 = Processor.parseReg(op2);
            if (!op1Imm && !op1Reg) {
                op1 = DataType.TRYTE.compile(op1, line).get(0);
                op1Imm = true;
            }
            if (twoOps && !op2Imm && !op2Reg) {
                op2 = DataType.TRYTE.compile(op2, line).get(0);
                op2Imm = true;
            }
            // assemble
            ArrayList<String> trytes = new ArrayList<>();
            if (op1Reg & !op1Adr & !twoOps) {
                // jxx a
                trytes.add(CodeType.asm("λ", "1", "0", "0", con, typ));
                trytes.add(CodeType.asm("0", "0", "0", "0", op1, "0"));
                trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
            } else if (op1Imm & !op1Adr & !twoOps) {
                // jxx i1
                trytes.add(CodeType.asm("0", "1", "0", "0", con, typ));
                trytes.add(CodeType.asm("0", "0", "0", "1", "0", "0"));
                trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                trytes.add(op1);
            } else if (op1Imm & !op1Adr & twoOps & op2Reg & !op2Adr) {
                // jxx i1, a
                trytes.add(CodeType.asm("0", "1", "0", "0", con, typ));
                trytes.add(CodeType.asm("0", "0", "0", "0", op1, "0"));
                trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                trytes.add(op1);
            } else if (op1Imm & !op1Adr & twoOps & op2Imm & !op2Adr) {
                // jxx i1, i2
                trytes.add(CodeType.asm("1", "1", "0", "0", con, typ));
                trytes.add(CodeType.asm("0", "0", "0", "λ", "0", "0"));
                trytes.add(CodeType.asm("0", "0", "0", "0", "0", "0"));
                trytes.add(op1);
                trytes.add(op2);
            } else {
                String form = "Line #%d: <%s %s";
                if (twoOps) form += ", ";
                form += "%s> not allowed.";
                throw new Exception(String.format(form, line, name, ops.get(0), (twoOps ? ops.get(1) : "")));
            }
            return trytes;
        }
    },
    RESTART("restart") {
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            if (hasDst) throw new Exception(String.format("Line #%d: <%s> cannot have destination.", line, name));
            if (ops.size() != 0) throw new Exception(String.format("Line #%d: <%s> cannot have operands.", line, name));
            // assemble
            ArrayList<String> trytes = new ArrayList<>();
            trytes.add(CodeType.asm("λ", "λ", "0", "λ", "0", "λ"));
            trytes.add(CodeType.asm("1", "0", "0", "0", "0", "0"));
            trytes.add(CodeType.asm("0", "0", "λ", "0", "0", "0"));
            return trytes;
        }
    },
    ALU_INSTR("") {
        @SuppressWarnings("SpellCheckingInspection")
        @Override public ArrayList<String> compile(String name, ArrayList<String> ops, boolean hasDst, int line) throws Exception {
            // extract AL0, AL1, OP1, OP2 and DST
            if (!hasDst) throw new Exception(String.format("Line #%d: <%s> must have destination.", line, name));
            if (ops.size() != 3)
                throw new Exception(String.format("Line #%d: <%s> must have exactly three operands: two arguments and destination.", line, name));
            name = name.toLowerCase();
            String al0, al1;
            if ("nand".equals(name)) {
                al0 = "0";
                al1 = "λ";
            } else if ("nor".equals(name)) {
                al0 = "0";
                al1 = "0";
            } else if ("ncon".equals(name)) {
                al0 = "0";
                al1 = "1";
            } else if ("nany".equals(name)) {
                al0 = "1";
                al1 = "λ";
            } else if ("tmul".equals(name)) {
                al0 = "1";
                al1 = "1";
            } else throw new Exception(String.format("Line #%d: invalid ALU instruction type: <%s> (allowed: NAND, NOR, NCON, NANY, TMUL).", line, name));
            String op1 = ops.get(0);
            String op2 = ops.get(1);
            String dst = ops.get(2);
            boolean op1Adr = op1.startsWith("[") && op1.endsWith("]");
            boolean op2Adr = op2.startsWith("[") && op2.endsWith("]");
            boolean dstAdr = dst.startsWith("[") && dst.endsWith("]");
            if (op1Adr) op1 = CodeType.dereference(op1);
            if (op2Adr) op2 = CodeType.dereference(op2);
            if (dstAdr) dst = CodeType.dereference(dst);
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = Processor.isValidRegName(op2);
            boolean op2Imm = !op2Reg && Processor.isValidLabelName(op2);
            boolean dstReg = Processor.isValidRegName(dst);
            boolean dstImm = !dstReg && Processor.isValidLabelName(dst);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (op2Reg) op2 = Processor.parseReg(op2);
            if (dstReg) dst = Processor.parseReg(dst);
            if (!op1Imm && !op1Reg) { // op1 is raw number
                op1 = DataType.TRYTE.compile(op1, line).get(0);
                op1Imm = true;
            }
            if (!op2Imm && !op2Reg) { // op2 is raw number
                op2 = DataType.TRYTE.compile(op2, line).get(0);
                op2Imm = true;
            }
            if (!dstImm && !dstReg) { // dst is raw number
                dst = DataType.TRYTE.compile(dst, line).get(0);
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
            } else if (op1Reg & !op1Adr & op2Imm & !op2Adr & dstImm & dstAdr) {
                // xxxx a, i2 -> [i1]
                trytes.add(CodeType.asm("1", "0", "0", "0", "0", "λ"));
                trytes.add(CodeType.asm(al0, al1, "0", "λ", "0", op1));
                trytes.add(CodeType.asm("0", "0", "0", "1", "1", "1"));
                trytes.add(dst);
                trytes.add(op2);
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
            } else throw new Exception(String.format("Line #%d: <%s %s, %s → %s> not allowed.", line, name, ops.get(0), ops.get(1), ops.get(2)));
            return trytes;
        }
    };

    private String name;

    CodeType(String name) {
        this.name = name;
    }

    public static CodeType parseByName(String name) {
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

    public abstract ArrayList<String> compile(String realName, ArrayList<String> operands, boolean hasDestination, int lineNum) throws Exception;

}
