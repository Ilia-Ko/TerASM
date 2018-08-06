package lines;

import engine.Processor;
import types.DataType;

public class DataLine extends AsmLine {

    public DataLine(String line, Processor proc, int lineNum) throws Exception {
        super(line, proc, lineNum);
        proc.getData().add(this);
    }

    @Override public int compile(int address) throws Exception {
        // split to parts
        String[] parts = line.split("[\\t ,]");
        if (parts.length < 2) throw new Exception("Data line too short: " + line);

        // get data type
        DataType type = DataType.parseByName(parts[0]);
        if (type == null) throw new Exception("Data type not recognized: " + parts[0]);

        // compile trytes and remember links
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (Processor.isValidLabelName(parts[i])) trytes.add(parts[i]);
            else trytes.addAll(type.compile(parts[i], lineNum));
        }

        // update address
        this.address = address;
        return address + trytes.size();
    }

}
