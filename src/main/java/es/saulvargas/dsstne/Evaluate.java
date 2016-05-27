package es.saulvargas.dsstne;

import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.core.preference.SimplePreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;

import static org.ranksys.formats.parsing.Parsers.*;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

public class Evaluate {

    public static void main(String[] args) throws Exception {
        String[] recs = args;

        String testDataPath = "test.data";
        PreferenceData<Long, Long> testData = SimplePreferenceData.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp));

        int cutoff = 10;
        Double threshold = 4.0;
        SystemMetric<Long, Long> precision = new AverageRecommendationMetric<>(new Precision(cutoff, new BinaryRelevanceModel<>(false, testData, threshold)), testData.numUsersWithPreferences());

        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp, lp);

        for (String rec : recs) {
            precision.reset();
            format.getReader(rec).readAll().forEach(precision::add);

            System.out.println(rec + "\t" + precision.evaluate());
        }
    }
}
