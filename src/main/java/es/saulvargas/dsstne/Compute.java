package es.saulvargas.dsstne;

import static org.ranksys.formats.parsing.Parsers.lp;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.ranksys.mf.Factorization;
import es.uam.eps.ir.ranksys.mf.als.PZTFactorizer;
import es.uam.eps.ir.ranksys.mf.rec.MFRecommender;
import es.uam.eps.ir.ranksys.nn.user.UserNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.TopKUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import es.uam.eps.ir.ranksys.nn.user.sim.VectorCosineUserSimilarity;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.fast.basic.PopularityRecommender;
import es.uam.eps.ir.ranksys.rec.fast.basic.RandomRecommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jooq.lambda.Unchecked;
import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

public class Compute {

    public static void main(String[] args) throws IOException {

        String userPath = "users.txt";
        String itemPath = "items.txt";
        String trainDataPath = "train.data";
        String testDataPath = "test.data";
        FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
        FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));
        FastPreferenceData<Long, Long> trainData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(trainDataPath, lp, lp), userIndex, itemIndex);
        FastPreferenceData<Long, Long> testData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp), userIndex, itemIndex);

        //////////////////
        // RECOMMENDERS //
        //////////////////
        Map<String, Supplier<Recommender<Long, Long>>> recMap = new HashMap<>();

        // random recommendation
        recMap.put("rnd", () -> {
            return new RandomRecommender<>(trainData, trainData);
        });

        // most-popular recommendation
        recMap.put("pop", () -> {
            return new PopularityRecommender<>(trainData);
        });

        // user-based nearest neighbors
        recMap.put("ub", () -> {
            double alpha = 0.5;
            int k = 100;
            int q = 1;

            UserSimilarity<Long> sim = new VectorCosineUserSimilarity<>(trainData, alpha, true);
            UserNeighborhood<Long> neighborhood = new TopKUserNeighborhood<>(sim, k);

            return new UserNeighborhoodRecommender<>(trainData, neighborhood, q);
        });

        // implicit matrix factorization of Pilaszy et al. 2010
        recMap.put("mf", () -> {
            int k = 50;
            double lambda = 0.1;
            double alpha = 1.0;
            DoubleUnaryOperator confidence = x -> 1 + alpha * x;
            int numIter = 20;

            Factorization<Long, Long> factorization = new PZTFactorizer<Long, Long>(lambda, confidence, numIter).factorize(k, trainData);

            return new MFRecommender<>(userIndex, itemIndex, factorization);
        });

        ////////////////////////////////
        // GENERATING RECOMMENDATIONS //
        ////////////////////////////////
        Set<Long> targetUsers = testData.getUsersWithPreferences().collect(Collectors.toSet());
        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp, lp);
        Function<Long, IntPredicate> filter = FastFilters.notInTrain(trainData);
        int maxLength = 100;
        RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(userIndex, itemIndex, targetUsers.stream(), filter, maxLength);

        recMap.forEach(Unchecked.biConsumer((name, recommender) -> {
            System.out.println("Running " + name);
            try (RecommendationFormat.Writer<Long, Long> writer = format.getWriter(name)) {
                runner.run(recommender.get(), writer);
            }
        }));
    }
}
