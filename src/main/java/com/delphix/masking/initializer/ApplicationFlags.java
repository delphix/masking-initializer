package com.delphix.masking.initializer;

import com.delphix.masking.initializer.exception.InputException;
import lombok.Getter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * This class is responsible for parsing the command line input and doing some validation.
 */
public class ApplicationFlags {

    private static final Logger logger = LogManager.getLogger(ApplicationFlags.class);

    private static final String HOST_OPTION = "H";
    private static final String PORT_OPTION = "p";
    private static final String API_PATH_OPTION = "a";
    private static final String FILE_NAME_OPTION = "f";
    private static final String USERNAME_OPTION = "u";
    private static final String PASSWORD_OPTION = "P";
    private static final String SETUP_OPTION = "s";
    private static final String BACKUP_OPTION = "b";
    private static final String OVERWRITE_OPTION = "o";
    private static final String GLOBAL_OPTION = "g";
    private static final String ENGINE_SYNC_OPTION = "e";
    private static final String SCALE_OPTION = "c";
    private static final String REPLACE_OPTION = "r";
    private static final String MASKED_COLUMN_OPTION = "m";
    private static final String LOG_LEVEL_OPTION = "l";
    private static final String IGNORE_ERRORS = "i";
    private static final String REDACT_USER_INFO = "R";
    private static final String AUTHTOKEN_OPTION = "t";
    private static final String EXECUTIONS_OPTION = "x";

    private static final String MASKING_USER = "MASKING_USER";
    private static final String MASKING_PASSWORD = "MASKING_PASSWORD";
    private static final String MASKING_HOST = "MASKING_HOST";
    private static final String MASKING_PORT = "MASKING_PORT";
    private static final String AUTH_TOKEN = "AUTH_TOKEN";


    @Getter
    Path file;
    @Getter
    String host;
    @Getter
    String port;
    @Getter
    String username;
    @Getter
    String password;
    @Getter
    String apiPath = "masking";
    @Getter
    Boolean overwrite;
    @Getter
    Boolean global;
    @Getter
    Boolean redactUserInfo;
    @Getter
    Boolean sync;
    @Getter
    Boolean scaled;
    @Getter
    Boolean replace = false;
    @Getter
    Boolean maskedColumn = false;
    @Getter
    boolean isBackup = false;
    @Getter
    boolean ignoreErrors = false;
    @Getter
    String authToken;
    @Getter
    Boolean execution;

    public ApplicationFlags(String args[]) throws ParseException, InputException {

        // Set the command line options
        Options options = new Options();

        options.addOption(HOST_OPTION, true, "Host machine to backup/restore");
        options.addOption(PORT_OPTION, true, "Port on host machine masking app is located");
        options.addOption(API_PATH_OPTION, true, "Path in URL, either 'dmsuite' or 'masking'");
        options.addRequiredOption(FILE_NAME_OPTION, "fileName", true, "File to read or write to");
        options.addOption(USERNAME_OPTION, true, "Masking engine username");
        options.addOption(PASSWORD_OPTION, true, "Masking engine password");
        options.addOption(SETUP_OPTION, false, "Run setup");
        options.addOption(BACKUP_OPTION, false, "Run backup");
        options.addOption(OVERWRITE_OPTION, false, "Overwrites the file in backup mode if the file exists");
        options.addOption(GLOBAL_OPTION, false, "Specifies that global objects should be backed up");
        options.addOption(ENGINE_SYNC_OPTION, false, "Specifies that engine sync objects should be backed up");
        options.addOption(SCALE_OPTION, false, "Specifies that the scaled option should be used.");
        options.addOption(REPLACE_OPTION, false, "Specifies that objects that already exist should be overwritten");
        options.addOption(MASKED_COLUMN_OPTION, false, "Only backup masked columns");
        options.addOption(LOG_LEVEL_OPTION, true, "Level of logging to use");
        options.addOption(IGNORE_ERRORS, false, "Specifies that errors should be ignored");
        options.addOption(REDACT_USER_INFO, false, "Specifies that user info should be redacted");
        options.addOption(AUTHTOKEN_OPTION, true, "Authorization token");
        options.addOption(EXECUTIONS_OPTION, false, "Specifies that execution objects should be backed up");


        // Read in the command line options and parse them
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine = commandLineParser.parse(options, args);

        Configurator.setRootLevel(Level.INFO);
        if (commandLine.hasOption(LOG_LEVEL_OPTION)) {
            Level level;
            switch (commandLine.getOptionValue(LOG_LEVEL_OPTION).toUpperCase()) {
                case "INFO":
                    level = Level.INFO;
                    break;
                case "DEBUG":
                    level = Level.DEBUG;
                    break;
                case "TRACE":
                    level = Level.TRACE;
                    break;
                case "ERROR":
                    level = Level.ERROR;
                    break;
                case "WARN:":
                    level = Level.WARN;
                    break;
                default:
                    logger.error("Invalid log level: {}", commandLine.getOptionValue(LOG_LEVEL_OPTION));
                    throw new InputException("Invalid log level");
            }
            Configurator.setRootLevel(level);
        }

        logger.debug("Starting application with args {}", Arrays.toString(args));

        logger.trace("Parsing command line options");

        boolean runSetup = commandLine.hasOption(SETUP_OPTION);
        boolean runBackup = commandLine.hasOption(BACKUP_OPTION);
        // Only Setup or backup can be called at once, and one must be called
        if (!runSetup ^ runBackup) {
            printErrorMessage(
                    options,
                    "Must set exactly one of %s or %s",
                    SETUP_OPTION,
                    BACKUP_OPTION);
        }

        // File name is required so it always exists at this point
        file = Paths.get(commandLine.getOptionValue(FILE_NAME_OPTION));

        username = getValue(MASKING_USER, USERNAME_OPTION, commandLine);
        password = getValue(MASKING_PASSWORD, PASSWORD_OPTION, commandLine);
        port = getValue(MASKING_PORT, PORT_OPTION, commandLine);
        host = getValue(MASKING_HOST, HOST_OPTION, commandLine);
        authToken = getValue(AUTH_TOKEN, AUTHTOKEN_OPTION, commandLine);


        /*
         * Backup requires host/port/username/password being all set. An authtoken can be provided instead of a username
         * and password.
         */

        if (runBackup) {
            logger.trace("Parsing flags for backup mode");
            isBackup = true;

            if (((username == null || password == null) && authToken == null) || port == null || host == null) {
                printErrorMessage(options,
                        "All of the following options must be set for backup mode",
                        HOST_OPTION,
                        PORT_OPTION,
                        USERNAME_OPTION,
                        PASSWORD_OPTION);
            }

            // If the file already exists, make sure the user has specified the overwrite flag to overwrite the file
            if (file.toFile().exists() && !commandLine.hasOption(OVERWRITE_OPTION)) {
                printErrorMessage(
                        options,
                        "File already exists, please specify %s to overwrite file contents",
                        OVERWRITE_OPTION);
            }
            scaled = commandLine.hasOption(SCALE_OPTION);
            maskedColumn = commandLine.hasOption(MASKED_COLUMN_OPTION);
            /*
             * Back up each section of the masking engine.
             * By default only backup masking job objects and require flags to backup global objects
             */
            global = commandLine.hasOption(GLOBAL_OPTION);
            sync = commandLine.hasOption(ENGINE_SYNC_OPTION);
            redactUserInfo = commandLine.hasOption(REDACT_USER_INFO);
            execution = commandLine.hasOption(EXECUTIONS_OPTION);

        } else {
            logger.trace("Parsing flags for setup mode");
            // Running setup

            // File that we are restoring from must exist
            if (!file.toFile().exists()) {
                logger.error("Unable to find a file " + file);
                return;
            }

            replace = commandLine.hasOption(REPLACE_OPTION);

            if (commandLine.hasOption(API_PATH_OPTION)) {
                apiPath = commandLine.getOptionValue(API_PATH_OPTION);
            }
            if (commandLine.hasOption(IGNORE_ERRORS)) {
                ignoreErrors = true;
            }

        }

    }

    /*
     * Prints an error message and lists any arguments that are a part of the problem
     */
    private static void printErrorMessage(Options options, String formated, String... args) throws InputException {
        String error = String.format(formated, (Object[]) args);
        logger.error(error);
        for (String arg : args) {
            logger.info(options.getOption(arg).getDescription());
        }
        throw new InputException(error);
    }

    /*
     * Gets the value for the given option. If a flag for this option is set, return that flag, otherwise if an
     * environment variable for this flag is set, return that. Otherwise, return null.
     */
    private static String getValue(String envVariableName, String argOption, CommandLine commandLine) {
        if (commandLine.hasOption(argOption)) {
            return commandLine.getOptionValue(argOption);
        }

        if (System.getenv(envVariableName) != null) {
            return System.getenv(envVariableName);
        }

        return null;
    }

}
