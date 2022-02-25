package java_polytech.leo.executor;

import com.java_polytech.pipeline_interfaces.RC;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CodingProcessorBuffer {
    private static final int BITS_IN_BYTE_COUNT = 8;
    private static final int SHIFT = 1;

    private final Supplier<RC> getConsumerRC;
    private final Consumer<byte[]> setOutputData;
    private final byte[] buffer;
    private int filled;
    private int bufferByte;
    private int bufferByteFreeBits = BITS_IN_BYTE_COUNT;

    CodingProcessorBuffer(int bufferSize, Consumer<byte[]> setOutputData, Supplier<RC> getConsumerRC) {
        buffer = new byte[bufferSize];
        this.getConsumerRC = getConsumerRC;
        this.setOutputData = setOutputData;
    }

    public RC writeBit(int bit) {
        RC rc = RC.RC_SUCCESS;
        if (bufferByteFreeBits == 0) {
            // bufferByteFreeBits to default need to avoid recursion in case of last byte during writing bit
            bufferByteFreeBits = BITS_IN_BYTE_COUNT;
            rc = writeByte(bufferByte);
            if (!rc.isSuccess()) {
                return rc;
            }

            clearBuffByte();
        }
        bufferByte = ((bufferByte << SHIFT) | bit);
        bufferByteFreeBits--;
        return rc;
    }

    public RC writeByte(int sym) {
        buffer[filled++] = (byte) sym;
        if (filled == buffer.length) {
            return flush();
        }
        return RC.RC_SUCCESS;
    }

    public RC flush() {
        if (bufferByteFreeBits != BITS_IN_BYTE_COUNT) {
            // Case when some bits are written in bufferByte
            bufferByte <<= bufferByteFreeBits;
            // bufferByteFreeBits to default need to avoid recursion in case of last byte during writing bit
            bufferByteFreeBits = BITS_IN_BYTE_COUNT;
            RC rc = writeByte(bufferByte);
            if (!rc.isSuccess()) {
                return rc;
            }

            clearBuffByte();
        }

        byte[] res = buffer;
        if (filled < buffer.length) {
            res = Arrays.copyOf(buffer, filled);
        }
        setOutputData.accept(res);
        filled = 0;

        return getConsumerRC.get();
    }


    private void clearBuffByte() {
        bufferByte = 0;
        bufferByteFreeBits = BITS_IN_BYTE_COUNT;
    }
}
