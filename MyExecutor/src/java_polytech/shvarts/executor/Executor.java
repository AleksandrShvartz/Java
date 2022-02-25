package java_polytech.leo.executor;

import com.java_polytech.pipeline_interfaces.*;
import java_polytech.leo.universal_config.Grammar;
import java_polytech.leo.universal_config.ISyntaxAnalyzer;
import java_polytech.leo.universal_config.SyntaxAnalyzer;

public class Executor implements IExecutor {
    private static final String MODE_STRING = "mode";
    private static final String BUFFER_SIZE_STRING = "buffer_size";
    private static final String CODING_MODE_STRING = "coding";
    private static final String DECODING_MODE_STRING = "decoding";
    private static final int SIZE_OF_INT = 4;

    private static final int MAX_BUFFER_SIZE = 1000000;
    private static final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY};
    private byte[] processedData;
    private TYPE pickedType;
    private IMediator mediator;


    private ArithmeticCodingProcessor codingProcessor;

    IConsumer consumer;

    @Override
    public RC setConfig(String s) {
        ISyntaxAnalyzer config = new SyntaxAnalyzer(RC.RCWho.EXECUTOR,
                new Grammar(MODE_STRING, BUFFER_SIZE_STRING));
        RC rc = config.readConfig(s);
        if (!rc.isSuccess()) {
            return rc;
        }

        int bufferSize;
        try {
            bufferSize = Integer.parseInt(config.getParam(BUFFER_SIZE_STRING));
        } catch (NumberFormatException e) {
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        if (bufferSize % SIZE_OF_INT != 0) {
            // Проверка для массива char-ов сюда включается, поскольку если он делится на 4, то и на 2 поделится
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Size of buffer doesn't fit for ints");
        }
        if (bufferSize <= 0 || bufferSize > MAX_BUFFER_SIZE) {
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }

        switch (config.getParam(MODE_STRING)) {
            case CODING_MODE_STRING:
                codingProcessor = new ArithmeticCoder(new CodingProcessorBuffer(bufferSize, bytes -> processedData = bytes, consumer::consume));
                break;
            case DECODING_MODE_STRING:
                codingProcessor = new ArithmeticDecoder(new CodingProcessorBuffer(bufferSize, bytes -> processedData = bytes, consumer::consume));
                break;
            default:
                return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }

        return RC.RC_SUCCESS;
    }

    @Override
    public RC consume() {
        byte[] data = (byte[]) mediator.getData();
        RC rc = RC.RC_SUCCESS;
        if (data == null) {
            rc = codingProcessor.finish();
            if (!rc.isSuccess()) {
                return rc;
            }
            processedData = null;
            return consumer.consume();
        }
        for (byte b : data) {
            rc = codingProcessor.putByte(b);
            if (!rc.isSuccess()) {
                return rc;
            }
        }
        return rc;
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
        if (type.equals(TYPE.BYTE_ARRAY)) {
            return new ByteMediator();
        }
        return null;
    }

    @Override
    public RC setProvider(IProvider provider) {
        for (TYPE prType : provider.getOutputTypes()) {
            for (TYPE supType : supportedTypes) {
                if (prType.equals(supType)) {
                    pickedType = prType;
                    break;
                }
            }
        }

        if (pickedType == null) {
            return RC.RC_WRITER_TYPES_INTERSECTION_EMPTY_ERROR;
        }

        mediator = provider.getMediator(pickedType);
        return RC.RC_SUCCESS;
    }

    private class ByteMediator implements IMediator {
        @Override
        public Object getData() {
            if (processedData == null) {
                return null;
            }

            byte[] outputData = new byte[processedData.length];
            System.arraycopy(processedData, 0, outputData, 0, processedData.length);
            return outputData;
        }
    }
}
