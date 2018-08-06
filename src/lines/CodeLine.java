package lines;

import engine.Processor;
import types.CodeType;

import java.util.ArrayList;

public class CodeLine extends AsmLine {

    public CodeLine(String line, Processor proc, int lineNum) throws Exception {
        super(line, proc, lineNum);
        proc.getCode().add(this);
    }

    @Override public int compile(int address) throws Exception {
        // split to parts
        String[] parts = line.split("[ ,→]");

        // get code type
        CodeType type = CodeType.parseByName(parts[0]);
        if (type == null) throw new Exception("Instruction not recognized: " + parts[0]);

        // assemble
        ArrayList<String> operands = new ArrayList<>();
        for (int i = 1; i < parts.length; i++)
            if (!parts[i].isEmpty()) operands.add(parts[i]);
        trytes = type.compile(parts[0], operands, line.contains("→"), lineNum);

        // update address
        this.address = address;
        return address + trytes.size();
    }

}
