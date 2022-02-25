package java_polytech.leo.reader;

import com.java_polytech.pipeline_interfaces.*;
import java_polytech.leo.universal_config.Grammar;
import java_polytech.leo.universal_config.ISyntaxAnalyzer;
import java_polytech.leo.universal_config.SyntaxAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

public class Reader implements IReader {
    private static final String BUFFER_SIZE_STRING = "buffer_size";
    private static final int MAX_BUFFER_SIZE = 1000000;
    private static final int SIZE_OF_CHAR = 2;
    private static final int SIZE_OF_INT = 4;


    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    private TYPE pickedType;
    private InputStream inputStream;
    private byte[] readData;
    private int bufferSize;
    private IConsumer consumer;

    @Override
    public RC setInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return RC.RC_READER_FAILED_TO_READ;
        }

        this.inputStream = inputStream;
        return RC.RC_SUCCESS;
    }

    @Override
    public RC run() {
        byte[] buffer = new byte[bufferSize];
        int readCount;
        try {
            while ((readCount = inputStream.read(buffer)) > 0) {
                if (readCount == bufferSize) {
                    readData = buffer;
                } else {
                    readData = new byte[readCount];
                    System.arraycopy(buffer, 0, readData, 0, readCount);
                }

                if ((pickedType.equals(TYPE.INT_ARRAY) && readData.length % SIZE_OF_INT != 0)
                        || (pickedType.equals(TYPE.CHAR_ARRAY) && readData.length % SIZE_OF_CHAR != 0)) {
                    return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Read portion of bytes can't be converted to chosen type");
                }

                RC rc = consumer.consume();
                if (!rc.isSuccess()) {
                    return rc;
                }
            }
        } catch (IOException e) {
            return RC.RC_READER_FAILED_TO_READ;
        }

        readData = null;
        return consumer.consume();
    }

    @Override
    public RC setConfig(String s) {
        ISyntaxAnalyzer config = new SyntaxAnalyzer(RC.RCWho.READER, new Grammar(BUFFER_SIZE_STRING));
        RC rc = config.readConfig(s);
        if (!rc.isSuccess()) {
            return rc;
        }

        try {
            bufferSize = Integer.parseInt(config.getParam(BUFFER_SIZE_STRING));
        } catch (NumberFormatException e) {
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }
        if (bufferSize % SIZE_OF_INT != 0) {
            // Проверка для массива char-ов сюда включается, поскольку если он делится на 4, то и на 2 поделится
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Size of buffer doesn't fit for ints");
        }
        if (bufferSize <= 0 || bufferSize > MAX_BUFFER_SIZE) {
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }


        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConsumer(IConsumer consumer) {
        this.consumer = consumer;
        return consumer.setProvider(this);
    }

    @Override
    public TYPE[] getOutputTypes() {
        return supportedTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        switch (type) {
            case BYTE_ARRAY:
                pickedType = TYPE.BYTE_ARRAY;
                return new ByteMediator();
            case CHAR_ARRAY:
                pickedType = TYPE.CHAR_ARRAY;
                return new CharMediator();
            case INT_ARRAY:
                pickedType = TYPE.INT_ARRAY;
                return new IntMediator();
        }
        return null;
    }

    private class ByteMediator implements IMediator {

        @Override
        public Object getData() {
            if (readData == null) {
                return null;
            }

            byte[] outputData = new byte[readData.length];
            System.arraycopy(readData, 0, outputData, 0, readData.length);
            return outputData;
        }
    }

    private class IntMediator implements IMediator {

        @Override
        public Object getData() {
            if (readData == null) {
                return null;
            }

            IntBuffer intBuffer = ByteBuffer.wrap(readData).asIntBuffer();
            int[] outputData = new int[intBuffer.remaining()];
            intBuffer.get(outputData);
            return outputData;
        }
    }

    private class CharMediator implements IMediator {

        @Override
        public Object getData() {
            if (readData == null) {
                return null;
            }

            return new String(readData, StandardCharsets.UTF_8).toCharArray();
        }
    }

}
