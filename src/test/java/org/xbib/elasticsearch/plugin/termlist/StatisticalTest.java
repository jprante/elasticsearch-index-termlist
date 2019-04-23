package org.xbib.elasticsearch.plugin.termlist;

import org.junit.Test;
import org.xbib.elasticsearch.plugin.termlist.common.math.SummaryStatistics;

import static org.junit.Assert.assertEquals;

public class StatisticalTest {

    @Test
    public void testOneSequence() throws Exception {
        int[] numbers = new int[] { 32, 34, 34, 42};
        SummaryStatistics stat = new SummaryStatistics();
        for (int n : numbers) {
            stat.addValue(n);
        }
        assertEquals(stat.getN(), 4);
        assertEquals(stat.getSum(), 142.0);
        assertEquals(stat.getSumsq(), 5100.0);
        assertEquals(stat.getMean(), 35.5);
        assertEquals(stat.getSigma(), 4.43471156521669);
        assertEquals(stat.getVariance(), 19.666666666666654);
    }

    @Test
    public void testTwoSequences() throws Exception {
        int[] numbers1 = new int[] { 32, 34 };
        SummaryStatistics stat = new SummaryStatistics();
        for (int n : numbers1) {
            stat.addValue(n);
        }
        int[] numbers2 = new int[] { 34, 42 };
        SummaryStatistics stat2 = new SummaryStatistics();
        for (int n : numbers2) {
            stat2.addValue(n);
        }

        stat.update(stat2);

        assertEquals(stat.getN(), 4);
        assertEquals(stat.getSum(), 142.0);
        assertEquals(stat.getSumsq(), 5100.0);
        assertEquals(stat.getMean(), 35.5);
        assertEquals(stat.getSigma(), 4.43471156521669);
        assertEquals(stat.getVariance(), 19.666666666666668); // rounding error
    }

}
