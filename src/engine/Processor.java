package engine;

import lines.AsmLine;
import lines.CodeLine;
import lines.DataLine;

import java.io.*;
import java.util.*;

public class Processor {

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
        Integer lineNum = 0;
        while (reader.ready()) {
            String line = reader.readLine().trim();
            lineNum++;
            if (SECTION_DATA.equals(line)) parseData(reader, lineNum);
            else if (SECTION_CODE.equals(line)) parseCode(reader, lineNum);
        }
        reader.close();
    }
    void compile() throws Exception {
        // compile everything
        int address = 0;
        for (AsmLine codeUnit : code) address = codeUnit.compile(address);
        for (AsmLine dataUnit : data) address = dataUnit.compile(address);
    }
    void output() throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(destination));

        // output everything
        for (AsmLine line : code) {
            for (String tryte : line.output()) {
                writer.write(tryte);
                writer.write(' ');
            }
            writer.newLine();
        }
        for (AsmLine line : data) {
            for (String tryte : line.output()) {
                writer.write(tryte);
                writer.write(' ');
            }
            writer.newLine();
        }

        writer.flush();
        writer.close();
    }

    private void parseData(BufferedReader reader, Integer lineNum) throws Exception {
        while (reader.ready()) {
            // prepare line
            String line = reader.readLine().trim();
            lineNum++;
            if (line.contains(";")) line = line.substring(0, line.indexOf(';')); // remove comments
            if (line.equals(SECTION_CODE)) {
                parseCode(reader, lineNum);
                continue;
            }
            if (line.isEmpty()) continue;
            // init data line
            new DataLine(line, this, lineNum);
        }
    }
    private void parseCode(BufferedReader reader, Integer lineNum) throws Exception {
        while (reader.ready()) {
            // prepare line
            String line = reader.readLine().trim();
            lineNum++;
            if (line.contains(";")) line = line.substring(0, line.indexOf(';')); // remove comments
            if (line.equals(SECTION_DATA)) {
                parseData(reader, lineNum);
                continue;
            }
            if (line.isEmpty()) continue;
            // init data line
            new CodeLine(line, this, lineNum);
        }
    }

    public HashMap<String, AsmLine> getLabels() {
        return labels;
    }
    public ArrayList<AsmLine> getData() {
        return data;
    }
    public ArrayList<AsmLine> getCode() {
        return code;
    }

    // utils
    public static boolean isValidLabelName(String name) {
        for (int codePoint : name.codePoints().toArray())
            if (!Character.isAlphabetic(codePoint) && !Character.isDigit(codePoint) && codePoint != '_') return false;
        int code0 = name.codePointAt(0);
        return Character.isAlphabetic(code0) && code0 != 'λ';
    }
    public static boolean isValidRegName(String reg) {
        String r = reg.toLowerCase();
        return "rz".equals(r) || "r0".equals(r) || "r1".equals(r);
    }
    public static String parseReg(String reg) {
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
