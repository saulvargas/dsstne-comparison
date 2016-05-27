package es.saulvargas.dsstne;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws Exception {
        String command = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);
        
        switch (command) {
            case "compute":
                Compute.main(args);
                break;
            case "evaluate":
                Evaluate.main(args);
                break;
            case "from-dsstne-format":
                FromDSSTNEFormat.main(args);
                break;
            case "to-dsstne-format":
                ToDSSTNEFormat.main(args);
                break;
        }
    }
}
