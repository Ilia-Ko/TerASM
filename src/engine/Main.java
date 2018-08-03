package engine;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // get files
        File source = null;
        File destination = null;
        if (args.length > 0) {
            source = new File(args[0]);
            if (!source.exists()) System.out.println("Source file not found.");
            if (args.length > 1) destination = new File(args[1]);
            else destination = new File(args[0].replace(".asm", "") + ".ter");
            try {
                if (destination.exists() && !destination.delete()) {
                    System.out.printf("Output file '%s' already exists and cannot be deleted.\n", destination);
                    destination = null;
                } else if (!destination.createNewFile()) {
                    System.out.printf("Output file '%s' cannot be created.\n", destination);
                    destination = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("IO error occurred.");
            }
        } else System.out.println("Please, pass TerASM source file as an argument.");

        // process code
        if (source != null && destination != null) {
            // init TerASM processor
            Processor processor = new Processor(source, destination);
            try {
                // process code
                processor.parse();
                processor.compile();
                processor.output();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Compilation error occurred.");
                System.exit(-1);
            }
        }
    }

}
