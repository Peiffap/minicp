package minicp.examples;

import com.github.guillaumederval.javagrading.GradeClass;
import minicp.util.io.InputReader;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@GradeClass(totalValue = 1, defaultCpuTimeout = 5000)
public class QAPCustomBranchingTest {
    @Test
    public void simpleTest() {
        InputReader reader = new InputReader("data/qap.txt");

        int n = reader.getInt();
        // Weights
        int[][] w = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                w[i][j] = reader.getInt();
            }
        }
        // Distance
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = reader.getInt();
            }
        }

        List<Integer> solutions = QAP.solve(n, w, d, true, stats -> stats.numberOfNodes() > 19400);
        assertEquals((int)solutions.get(solutions.size()-1), 9552);
    }
}
