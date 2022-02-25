package java_polytech.leo.manager;

import com.java_polytech.pipeline_interfaces.RC;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {
    private static final int ARG_NUM_FOR_FILENAME = 0;
    private static final String LOGGER_NAME = "Pipeline Leo";
    private static final String LOG_OUT_FILENAME = "log.txt";

    private static Logger createLogger() throws IOException {
        Logger logger = Logger.getLogger(LOGGER_NAME);
        FileHandler fh;
        fh = new FileHandler(LOG_OUT_FILENAME);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        // Убираем вывод из консоли
        logger.setUseParentHandlers(false);

        return logger;
    }

    public static void main(String[] args) {
        Logger logger;
        try {
            logger = createLogger();
        } catch (IOException e) {
            System.out.println("Error: Can't create logger. Pipeline will not be created!");
            return;
        }

        String finMsg;
        boolean isBadEnd = false;
        if (args[ARG_NUM_FOR_FILENAME] != null) {
            Manager manager = new Manager(logger);
            RC rc = manager.run(args[ARG_NUM_FOR_FILENAME]);

            finMsg = rc.info;
            if (!rc.isSuccess()) {
                finMsg = "Error: " + rc.who.get() + ": " + rc.info;
                isBadEnd = true;
            }
        } else {
            finMsg = RC.RC_MANAGER_INVALID_ARGUMENT.info;
            isBadEnd = true;
        }

        if (isBadEnd) {
            logger.log(Level.SEVERE, finMsg);
        } else {
            logger.log(Level.INFO, finMsg);
        }

        System.out.println(finMsg);
    }
}