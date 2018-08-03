package types;

import engine.Processor;

import java.util.ArrayList;

public enum CodeType {

    MOV("mov") {
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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
            boolean srcReg = Processor.isValidRegName(src);
            boolean srcImm = !srcReg && Processor.isValidLabelName(src);
            boolean dstReg = Processor.isValidRegName(dst);
            boolean dstImm = !dstReg && Processor.isValidLabelName(dst);
            if (srcReg) src = Processor.parseReg(src);
            if (dstReg) dst = Processor.parseReg(dst);
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
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = Processor.isValidRegName(op2);
            boolean op2Imm = !op2Reg && Processor.isValidLabelName(op2);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (twoOps && op2Reg) op2 = Processor.parseReg(op2);
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
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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
            boolean srcReg = Processor.isValidRegName(src);
            boolean srcImm = !srcReg && Processor.isValidLabelName(src);
            boolean dstReg = Processor.isValidRegName(dst);
            boolean dstImm = !dstReg && Processor.isValidLabelName(dst);
            if (srcReg) src = Processor.parseReg(src);
            if (dstReg) dst = Processor.parseReg(dst);
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
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = Processor.isValidRegName(op2);
            boolean op2Imm = !op2Reg && Processor.isValidLabelName(op2);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (op2Reg) op2 = Processor.parseReg(op2);
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
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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
            boolean op1Reg = Processor.isValidRegName(op1);
            boolean op1Imm = !op1Reg && Processor.isValidLabelName(op1);
            boolean op2Reg = twoOps && Processor.isValidRegName(op2);
            boolean op2Imm = twoOps && !op2Reg && Processor.isValidLabelName(op2);
            if (op1Reg) op1 = Processor.parseReg(op1);
            if (twoOps && op2Reg) op2 = Processor.parseReg(op2);
            if (op1Imm) {
                op1 = "$" + op1;
            } else if (!op1Reg) {
                op1 = DataType.TRYTE.compile(op1).get(0);
                op1Imm = true;
            }
            if (twoOps && op2Imm) {
                op2 = "$" + op2;
            } else if (twoOps && !op2Reg) {
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
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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
        @SuppressWarnings("SpellCheckingInspection")
        @Override public ArrayList<String> compile(String realName, ArrayList<String> ops, boolean hasDst) throws Exception {
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

    public abstract ArrayList<String> compile(String realName, ArrayList<String> operands, boolean hasDestination) throws Exception;

}
