package java_polytech.leo.manager;

import com.java_polytech.pipeline_interfaces.*;
import java_polytech.leo.universal_config.Grammar;
import java_polytech.leo.universal_config.ISyntaxAnalyzer;
import java_polytech.leo.universal_config.SyntaxAnalyzer;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Manager {
    private static final String INPUT_FILE_STRING = "input_file";
    private static final String OUTPUT_FILE_STRING = "output_file";
    private static final String READER_CONFIG_FILE_STRING = "reader_config_file";
    private static final String WRITER_CONFIG_FILE_STRING = "writer_config_file";
    private static final String EXECUTOR_CONFIG_FILE_LIST_STRING = "executor_config_file_list";
    private static final String READER_CLASS_STRING = "reader_class";
    private static final String WRITER_CLASS_STRING = "writer_class";
    private static final String EXECUTOR_CLASS_LIST_STRING = "executor_class_list";

    private static final String SPLITTER_FOR_EXECUTORS = ",";
    private static final String SPACE = " ";
    private static final String EMPTY_STRING = "";

    private final Logger logger;
    private ISyntaxAnalyzer config;

    private InputStream inputStream;
    private OutputStream outputStream;

    private IReader reader;
    private IWriter writer;
    private IExecutor[] executors;

    public Manager(Logger logger) {
        this.logger = logger;
    }

    public RC run(String configFilename) {
        RC rc = setConfig(configFilename);
        if (!rc.isSuccess()) {
            return rc;
        }

        rc = buildPipeline();
        if (!rc.isSuccess()) {
            return rc;
        }

        StringBuilder finMsgBuilder = new StringBuilder("Pipeline was successfully built, order of Executors: \n");
        for (IExecutor executor : executors) {
            finMsgBuilder.append(executor).append("\n");
        }
        logger.log(Level.INFO, finMsgBuilder.toString());

        rc = reader.run();
        if (!rc.isSuccess()) {
            return rc;
        }

        return closeStreams();
    }

    private RC setConfig(String s) {
        logger.log(Level.INFO, "Setting config...");
        config = new SyntaxAnalyzer(RC.RCWho.MANAGER,
                new Grammar(INPUT_FILE_STRING, OUTPUT_FILE_STRING, READER_CONFIG_FILE_STRING, WRITER_CONFIG_FILE_STRING,
                        EXECUTOR_CONFIG_FILE_LIST_STRING, READER_CLASS_STRING, WRITER_CLASS_STRING, EXECUTOR_CLASS_LIST_STRING
                ));
        return config.readConfig(s);
    }

    private RC buildPipeline() {
        logger.log(Level.INFO, "Building pipeline...");
        RC rc;
        if (!(rc = openStreams()).isSuccess()) {
            return rc;
        }
        if (!(rc = setParticipants()).isSuccess()) {
            return rc;
        }
        if (!(rc = reader.setConsumer(executors[0])).isSuccess()) {
            return rc;
        }
        for (int i = 0; i < executors.length - 1; i++) {
            //Каждому предудущему executor-у делаем consumer-ом следующий executor
            if (!(rc = executors[i].setConsumer(executors[i + 1])).isSuccess()) {
                return rc;
            }
        }
        if (!(rc = executors[executors.length - 1].setConsumer(writer)).isSuccess()) {
            return rc;
        }
        if (!(rc = reader.setConfig(config.getParam(READER_CONFIG_FILE_STRING))).isSuccess()) {
            return rc;
        }
        if (!(rc = setExecutorsConfigs()).isSuccess()) {
            return rc;
        }
        if (!(rc = writer.setConfig(config.getParam(WRITER_CONFIG_FILE_STRING))).isSuccess()) {
            return rc;
        }
        if (!(rc = reader.setInputStream(inputStream)).isSuccess()) {
            return rc;
        }

        return writer.setOutputStream(outputStream);
    }

    private RC openStreams() {
        logger.log(Level.INFO, "Opening streams...");
        try {
            inputStream = new FileInputStream(config.getParam(INPUT_FILE_STRING));
        } catch (FileNotFoundException ex) {
            return RC.RC_MANAGER_INVALID_INPUT_FILE;
        }
        try {
            outputStream = new FileOutputStream(config.getParam(OUTPUT_FILE_STRING));
        } catch (IOException ex) {
            return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
        }

        return RC.RC_SUCCESS;
    }

    private RC closeStreams() {
        logger.log(Level.INFO, "Closing streams...");
        boolean isClosed = true;
        try {
            inputStream.close();
        } catch (IOException ex) {
            isClosed = false;
        }
        try {
            outputStream.close();
        } catch (IOException ex) {
            isClosed = false;
        }

        if (!isClosed) {
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Error during closing stream");
        }
        return RC.RC_SUCCESS;
    }

    private RC setParticipants() {
        logger.log(Level.INFO, "Setting participants of pipeline...");
        reader = (IReader) getInstance(config.getParam(READER_CLASS_STRING), IReader.class);
        if (reader == null) {
            return RC.RC_MANAGER_INVALID_READER_CLASS;
        }

        writer = (IWriter) getInstance(config.getParam(WRITER_CLASS_STRING), IWriter.class);
        if (writer == null) {
            return RC.RC_MANAGER_INVALID_WRITER_CLASS;
        }

        executors = getExecutors(config.getParam(EXECUTOR_CLASS_LIST_STRING));
        if (executors == null) {
            return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
        }

        return RC.RC_SUCCESS;
    }

    /**
     * Creates list of all executors from execList that will be used in pipeline
     *
     * @param execList - string with all executors, splitted by ","
     * @return list of executors if instance of them all was successful
     * null otherwise
     */
    private IExecutor[] getExecutors(String execList) {
        execList = execList.replaceAll(SPACE, EMPTY_STRING);
        String[] executorNames = execList.split(SPLITTER_FOR_EXECUTORS);
        IExecutor[] executors = new IExecutor[executorNames.length];
        for (int i = 0; i < executors.length; i++) {
            executors[i] = (IExecutor) getInstance(executorNames[i], IExecutor.class);
            if (executors[i] == null) {
                return null;
            }
        }

        return executors;
    }

    /**
     * sets all executor configs from config(ISyntaxAnalyzer) to executors
     *
     * @return RC.RC_SUCCESS if all configs was sat ok
     * RC with error otherwise
     */
    private RC setExecutorsConfigs() {
        String execConfigsList = config.getParam(EXECUTOR_CONFIG_FILE_LIST_STRING).replaceAll(SPACE, EMPTY_STRING);
        String[] execConfigs = execConfigsList.split(SPLITTER_FOR_EXECUTORS);
        if (execConfigs.length != executors.length) {
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "The count of configs for executors not equal to count of classes");
        }

        RC rc = RC.RC_SUCCESS;
        for (int i = 0; i < execConfigs.length; i++) {
            rc = executors[i].setConfig(execConfigs[i]);
            if (!rc.isSuccess()) {
                return rc;
            }
        }

        return rc;
    }

    private Object getInstance(String className, Class<?> inter) {
        Object ans = null;
        try {
            Class<?> clazz = Class.forName(className);
            if (inter.isAssignableFrom(clazz)) {
                ans = clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            return null;
        }

        return ans;
    }
}
