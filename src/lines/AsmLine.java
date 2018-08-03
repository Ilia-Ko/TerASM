package lines;

import engine.Processor;
import types.DataType;

import java.util.ArrayList;

public abstract class AsmLine {

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

    public ArrayList<String> output() {
        // dereference labels if present
        for (int i = 0; i < trytes.size(); i++) {
            String tryte = trytes.get(i);
            if (tryte.charAt(0) == '$') { // link is absolute, should be relative
                int relAddress = processor.getLabels().get(tryte.substring(1)).address - address;
                trytes.set(i, DataType.TRYTE.compile(Integer.toString(relAddress)).get(0));
            } else if (Processor.isValidLabelName(tryte)) {
                trytes.set(i, processor.getLabels().get(tryte).getAddress());
            }
        }
        return trytes;
    }
    private String getAddress() {
        return DataType.TRYTE.compile(Integer.toString(address)).get(0);
    }

}
