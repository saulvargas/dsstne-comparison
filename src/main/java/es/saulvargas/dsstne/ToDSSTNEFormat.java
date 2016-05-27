package es.saulvargas.dsstne;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import static java.util.stream.Collectors.joining;
import org.jooq.lambda.Unchecked;
import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import static org.ranksys.formats.parsing.Parsers.lp;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

public class ToDSSTNEFormat {

    public static void main(String[] args) throws IOException {
        String userPath = "users.txt";
        String itemPath = "items.txt";
        String trainDataPath = "train.data";
        FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
        FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));
        FastPreferenceData<Long, Long> trainData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("train.dsstne"))) {
            trainData.getUsersWithPreferences().forEach(Unchecked.consumer(user -> {
                writer.write(user + "\t" + trainData.getUserPreferences(user).map(p -> p.v1 + ",1").collect(joining(":")));
                writer.newLine();
            }));
        }
    }
}
