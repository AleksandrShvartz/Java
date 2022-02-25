package java_polytech.leo.executor;

import com.java_polytech.pipeline_interfaces.RC;

public class ArithmeticCoder extends ArithmeticCodingProcessor {
    private static final int MASK_TO_UNSIGN = 0xFF;

    private int bitsToFollow;

    public ArithmeticCoder(CodingProcessorBuffer out) {
        super(out);
        bitsToFollow = 0;
    }

    @Override
    public RC putByte(byte val) {
        int index = val & MASK_TO_UNSIGN;
        updateWorkingRange(index);
        RC rc = tryPutBits();
        if (rc.isSuccess()) {
            updateWeight(index);
        }

        return rc;
    }

    @Override
    public RC finish() {
        updateWorkingRange(ALPHABET_LEN);
        RC rc = tryPutBits();
        if (!rc.isSuccess()) {
            return rc;
        }
        return writeFinalByte();
    }

    private void updateWorkingRange(int index) {
        double range = workingHigh - workingLow;
        workingHigh = workingLow + splitPoints[index + 1] * range;
        workingLow = workingLow + splitPoints[index] * range;
    }

    private RC tryPutBits() {
        RC rc = RC.RC_SUCCESS;
        do {
            if (workingHigh < SECOND_QTR_MAX) {
                getCloser(SEGM_MIN);
                rc = writeBitPlusFollow(0);
            } else if (workingLow >= SECOND_QTR_MAX) {
                getCloser(SEGM_MAX);
                rc = writeBitPlusFollow(1);
            } else if (workingLow >= FIRST_QTR_MAX && workingHigh < THIRD_QTR_MAX) {
                getCloser(SECOND_QTR_MAX);
                bitsToFollow++;
            } else {
                break;
            }
        } while (rc.isSuccess());

        return rc;
    }

    private void getCloser(double point) {
        workingLow = NARROW_COEF * workingLow - point;
        workingHigh = NARROW_COEF * workingHigh - point;
    }

    private RC writeFinalByte() {
        boolean isLeftInFirstQtr = (workingLow < FIRST_QTR_MAX);
        RC rc = writeBitPlusFollow(isLeftInFirstQtr ? 0 : 1);
        if (!rc.isSuccess()) {
            return rc;
        }

        rc = writeBitPlusFollow(!isLeftInFirstQtr ? 0 : 1);
        if (!rc.isSuccess()) {
            return rc;
        }

        return out.flush();
    }

    private RC writeBitPlusFollow(int bit) {
        RC rc = out.writeBit(bit);
        if (!rc.isSuccess()) {
            return rc;
        }

        while (bitsToFollow > 0) {
            rc = out.writeBit(bit == 0 ? 1 : 0);
            if (!rc.isSuccess()) {
                return rc;
            }

            bitsToFollow--;
        }

        return rc;
    }
}
