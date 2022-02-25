package java_polytech.leo.executor;

import com.java_polytech.pipeline_interfaces.RC;

import java.util.Arrays;

public abstract class ArithmeticCodingProcessor {
    protected static final int ALPHABET_LEN = 256;
    protected static final int ALPHABET_WITH_END_SYM_LEN = ALPHABET_LEN + 1;
    protected static final int SPLIT_POINTS_SIZE = ALPHABET_WITH_END_SYM_LEN + 1;
    protected static final int NARROW_COEF = 2;

    protected static final double WEIGHTS_MIN = 1;
    private static final double NORMAL_WEIGHTS_MAX = 190;
    private static final double CEILING_WEIGHTS_MAX = 256;
    private static final double WEIGHT_ADDITION = 1;

    protected static final double SEGM_MIN = 0;
    protected static final double FIRST_QTR_MAX = 0.25;
    protected static final double SECOND_QTR_MAX = 0.5;
    protected static final double THIRD_QTR_MAX = 0.75;
    protected static final double SEGM_MAX = 1;

    protected final double[] weights;
    protected final double[] splitPoints;
    protected final CodingProcessorBuffer out;

    protected double workingLow;
    protected double workingHigh;

    public abstract RC putByte(byte val);

    public abstract RC finish();

    protected ArithmeticCodingProcessor(CodingProcessorBuffer out) {
        this.weights = new double[ALPHABET_WITH_END_SYM_LEN];
        Arrays.fill(this.weights, WEIGHTS_MIN);
        splitPoints = new double[SPLIT_POINTS_SIZE];
        updateSplitPoints();
        this.out = out;


        workingLow = SEGM_MIN;
        workingHigh = SEGM_MAX;
    }

    protected void updateSplitPoints() {
        double fullSum = 0;
        for (double weight : weights) {
            fullSum += weight;
        }
        double sum = 0;
        splitPoints[0] = 0;
        for (int i = 0; i < ALPHABET_WITH_END_SYM_LEN; i++) {
            sum += weights[i];
            splitPoints[i + 1] = sum / fullSum;
        }
    }

    protected void fixWeights() {
        double multiplier = NORMAL_WEIGHTS_MAX / CEILING_WEIGHTS_MAX;
        for (int i = 0; i < ALPHABET_WITH_END_SYM_LEN; i++) {
            weights[i] = Double.max(weights[i] * multiplier, WEIGHTS_MIN);
        }
    }

    protected void updateWeight(int index) {
        weights[index] += WEIGHT_ADDITION;
        if (weights[index] >= CEILING_WEIGHTS_MAX) {
            fixWeights();
        }
        updateSplitPoints();
    }
}
