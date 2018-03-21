package com.delphix.masking.initializer;

import com.delphix.masking.initializer.maskingApi.BackupDriver;
import com.delphix.masking.initializer.maskingApi.SetupDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class main {

    private static final Logger logger = LogManager.getLogger(main.class);

    public static void main(String[] args) throws Exception {
        try {
            ApplicationFlags applicationFlags = new ApplicationFlags(args);

            if (applicationFlags.isBackup()) {
                BackupDriver backupDriver = new BackupDriver(applicationFlags);
                backupDriver.run();
            } else {
                SetupDriver setupDriver = new SetupDriver(applicationFlags);
                setupDriver.run();
            }
        } catch (Exception e) {
            logger.error("Error while running: ", e);
        }
    }

}
