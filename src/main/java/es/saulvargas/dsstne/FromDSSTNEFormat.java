package es.saulvargas.dsstne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.stream.Stream;
import org.jooq.lambda.Unchecked;

public class FromDSSTNEFormat {

    public static void main(String[] args) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
            reader.lines().forEach(line -> {
                String[] toks = line.split("\t");
                
                String user = toks[0];
                Stream.of(toks[1].split(":")).forEach(Unchecked.consumer(prediction -> {
                    String[] toktoks = prediction.split(",");
                    writer.write(user + "\t" + toktoks[0] + "\t" + toktoks[1]);
                    writer.newLine();
                }));
            });
        }
    }
}
