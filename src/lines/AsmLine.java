package lines;

import engine.Processor;
import types.DataType;

import java.util.ArrayList;

public abstract class AsmLine {

    private static final int BASE_ADDRESS = -364;

    String line;
    ArrayList<String> trytes;
    int address;
    private Processor processor;

    AsmLine(String line, Processor proc) throws Exception {
        processor = proc;
        trytes = new ArrayList<>();
        address = 0;
        // extract label
        int labelMark = line.indexOf(':');
        if (labelMark != -1) {
            String label = line.substring(0, labelMark);
            if (!Processor.isValidLabelName(label)) throw new Exception("Invalid label name: " + label);
            processor.getLabels().put(label, this);
            line = line.substring(labelMark + 1);
        }
        this.line = line.trim();
    }

    public abstract int compile(int address) throws Exception;

    public ArrayList<String> output() throws Exception {
        // dereference labels if present
        for (int i = 0; i < trytes.size(); i++) {
            String tryte = trytes.get(i);
            if (tryte.charAt(0) == '$') { // link is absolute, should be relative
                int currLen = tryte.charAt(1) - '0'; // account additional trytes of the current instruction
                tryte = tryte.substring(2);
                int relAddress = processor.getLabels().get(tryte).address - address - currLen;
                trytes.set(i, DataType.TRYTE.compile(Integer.toString(relAddress)).get(0));
            } else if (Processor.isValidLabelName(tryte)) {
                AsmLine link = processor.getLabels().get(tryte);
                if (link == null) throw new Exception("Undefined label: " + tryte);
                trytes.set(i, processor.getLabels().get(tryte).getAddress());
            }
        }
        return trytes;
    }
    private String getAddress() {
        return DataType.TRYTE.compile(Integer.toString(address + BASE_ADDRESS)).get(0);
    }

}
