package com.delphix.masking.initializer.maskingApi;

import static com.delphix.masking.initializer.Constants.BASE_FILE;
import static com.delphix.masking.initializer.Constants.JSON_EXTENSION;
import static com.delphix.masking.initializer.Constants.MASKING;
import static com.delphix.masking.initializer.Utils.getFileName;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.delphix.masking.initializer.maskingApi.endpointCaller.GetExecutionComponents;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetExecutions;
import com.delphix.masking.initializer.pojo.Execution;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;

import com.delphix.masking.initializer.ApplicationFlags;
import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.maskingApi.endpointCaller.ApiCallDriver;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetApplications;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetColumnMetadatas;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetDatabaseConnectors;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetDatabaseRulesets;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetDomains;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetEnvironments;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileConnectors;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileFieldMetadatas;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileFormats;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileMetadatas;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileRulesets;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetMaskingJobs;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetProfileExpressions;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetProfileSets;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetProfilingJobs;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetSyncableObjects;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetTableMetadatas;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetUsers;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostExportObject;
import com.delphix.masking.initializer.pojo.Application;
import com.delphix.masking.initializer.pojo.ColumnMetadata;
import com.delphix.masking.initializer.pojo.DatabaseConnector;
import com.delphix.masking.initializer.pojo.DatabaseRuleset;
import com.delphix.masking.initializer.pojo.Domain;
import com.delphix.masking.initializer.pojo.Environment;
import com.delphix.masking.initializer.pojo.ExportObject;
import com.delphix.masking.initializer.pojo.ExportObjectMetadata;
import com.delphix.masking.initializer.pojo.FileConnector;
import com.delphix.masking.initializer.pojo.FileFormat;
import com.delphix.masking.initializer.pojo.FileMetadata;
import com.delphix.masking.initializer.pojo.FileRuleset;
import com.delphix.masking.initializer.pojo.MaskingJob;
import com.delphix.masking.initializer.pojo.MaskingSetup;
import com.delphix.masking.initializer.pojo.ProfileExpression;
import com.delphix.masking.initializer.pojo.ProfileSet;
import com.delphix.masking.initializer.pojo.ProfilingJob;
import com.delphix.masking.initializer.pojo.TableMetadata;
import com.delphix.masking.initializer.pojo.User;

/**
 * This class is responsible for backing up a masking engine. Based on the provided input it can back up
 * job specific state and global state as well (syncable objects, profile sets, domains etc).
 */
public class BackupDriver {

    private static String DATABASE_PASSWORD = "DATABASE_PASSWORD";
    private static String SFTP_PASSWORD = "SFTP_PASSWORD";
    private static String USER_PASSWORD = "USER_PASSWORD";
    private static String REDACTED = "REDACTED";

    private ApiCallDriver apiCallDriver;

    private ApplicationFlags applicationFlags;

    private GetFileConnectors getFileConnectors;
    private GetFileRulesets getFileRulesets;

    private MaskingSetup maskingSetup;

    private boolean scaled;
    private boolean isMasked;
    private Path baseFolder;

    private Map<Integer, String> fileFormatsIdToName = new HashMap<>();
    private Map<Integer, String> profileSetsIdToName = new HashMap<>();

    /**
     * Initialize the {@link #maskingSetup} object with the necessary masking engine information
     *
     * @param applicationFlags contains all info regarding engine setup and flag configs
     * @throws ApiCallException
     * @throws IOException
     */
    public BackupDriver(ApplicationFlags applicationFlags) throws ApiCallException, IOException {

        this.applicationFlags = applicationFlags;

        // Initialize the api call driver to point to the correct engine
        if (applicationFlags.getAuthToken() == null) {
            apiCallDriver = new ApiCallDriver(
                    applicationFlags.getHost(),
                    applicationFlags.getUsername(),
                    applicationFlags.getPassword(),
                    applicationFlags.getPort(),
                    applicationFlags.getApiPath(),
                    applicationFlags.getReplace(),
                    applicationFlags.getIsSslEnabled());
        } else {
            apiCallDriver = new ApiCallDriver(
                    applicationFlags.getHost(),
                    applicationFlags.getAuthToken(),
                    applicationFlags.getPort(),
                    applicationFlags.getApiPath(),
                    applicationFlags.getReplace(),
                    applicationFlags.getIsSslEnabled());
        }

        scaled = applicationFlags.getScaled();
        isMasked = applicationFlags.getMaskedColumn();

        // Initialize the masking setup object that will be written to the backup file with the engine information
        maskingSetup = new MaskingSetup();
        maskingSetup.setHost(applicationFlags.getHost());
        maskingSetup.setPort(applicationFlags.getPort());
        maskingSetup.setApiPath(applicationFlags.getApiPath());
        maskingSetup.setUsername(applicationFlags.getUsername());
        maskingSetup.setPassword(applicationFlags.getPassword());
        maskingSetup.setAuthToken(applicationFlags.getAuthToken());
        maskingSetup.setScaled(scaled);
        // Set default incase path is null
        if (maskingSetup.getApiPath() == null) {
            maskingSetup.setApiPath(MASKING);
        }

        /*
         * Get the current version of this tool and add it to the backup file so that we can make sure the version
         * is compatible in the future for backups.
         */
        InputStream inputStream = getClass().getResourceAsStream("/version.txt");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        maskingSetup.setVersion(bufferedReader.readLine());

        baseFolder = applicationFlags.getFile();
        // The provided file refers to an entire directory when running in scale mode
        if (scaled) {
            if (baseFolder.toFile().exists()) {
                FileUtils.cleanDirectory(baseFolder.toFile());
                FileUtils.deleteDirectory(baseFolder.toFile());
            }
            if (!baseFolder.toFile().mkdirs()) {
                throw new RuntimeException("Unable to make folder " + baseFolder.toFile().getAbsolutePath());
            }
        }
    }

    /**
     * Backups the masking engine initialized in the constructor and writes it to the file
     */
    public void run() {

        backupFileFormats();

        // Back up global state depending on the user input
        if (applicationFlags.getGlobal()) {
            backupProfiles();
            backupDomains();
            backupUsers();
        } else {
            /*
             * If we are not backing up profiler sets, we still need a map of profiler sets id -> names so that we can
             * properly back up any profiler jobs.
             */
            GetProfileSets getProfileSets = new GetProfileSets();
            apiCallDriver.makeGetCall(getProfileSets);
            profileSetsIdToName = getProfileSets
                    .getProfileSets()
                    .stream()
                    .collect(Collectors.toMap(ProfileSet::getProfileSetId, ProfileSet::getProfileSetName));
        }

        // Back up syncable objects depening on the user input
        if (applicationFlags.getSync()) {
            backupSync();
        }

        // Back up executions if the user asked for them. There is no restoring these object, they are only for reference
        if (applicationFlags.getExecution()) {
            backupExecution();
        }

        // Masking jobs are always backed up regardless of provided flags
        backupApplications();

        // Write the output to a file
        finishBackup();
    }

    /**
     * Called once all other backup functions have been called. Writes the contents of {@link #maskingSetup} to the
     * provided backupFile to finish the backup
     *
     * @throws IOException If any IO errors occur
     */
    private void finishBackup() {

        String backupFile = baseFolder.toString();
        if (scaled) {
            backupFile = baseFolder.resolve(BASE_FILE).toString();
        }

        try (FileWriter fileWriter = new FileWriter(backupFile)) {
            fileWriter.write(Utils.getJSONFromClass(maskingSetup));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Backup the executions
     */
    private void backupExecution() {

        GetExecutions getExecutions = new GetExecutions();
        apiCallDriver.makeGetCall(getExecutions);

        if (getExecutions.getExecutions() == null) {
            return;
        }
        getExecutions.getExecutions().forEach(this::handleExecution);
        maskingSetup.setExecutions(getExecutions.getExecutions());
    }

    private void handleExecution(Execution execution) {

        execution.setExecutionComponent(new ArrayList<>());
        GetExecutionComponents getExecutionComponents = new GetExecutionComponents();
        apiCallDriver.makeGetCall(getExecutionComponents);
        if (getExecutionComponents.getExecutionComponents() == null) {
            return;
        }
        execution.setExecutionComponent(getExecutionComponents.getExecutionComponents());
        return;
    }

    /**
     * Back up the syncable objects
     *
     * @throws ApiCallException
     */
    private void backupSync() throws ApiCallException {

        ImmutableList<String> algorithmTypes = ImmutableList.of(
                "BINARYLOOKUP",
                "DATE_SHIFT",
                "LOOKUP",
                "MAPPLET",
                "KEY",
                "SEGMENT",
                "TOKENIZATION");

        ArrayList<ExportObjectMetadata> gottenSyncObjects = new ArrayList<>();

        algorithmTypes.forEach(algType -> {
            GetSyncableObjects getSyncableObjects = new GetSyncableObjects();
            getSyncableObjects.setObjectType(algType);
            apiCallDriver.makeGetCall(getSyncableObjects);
            if (getSyncableObjects.getExportResponseMetadata() != null) {
                gottenSyncObjects.addAll(getSyncableObjects.getExportResponseMetadata());
            }
        });

        ArrayList<ExportObject> exportObjects = new ArrayList();
        ArrayList<String> exportObjectFiles = new ArrayList();

        for (ExportObjectMetadata exportObjectMetadata : gottenSyncObjects) {
            PostExportObject postExportObject = new PostExportObject(new
                    ExportObjectMetadata[]{exportObjectMetadata});
            apiCallDriver.makePostCall(postExportObject);

            if (scaled) {
                String fileName = getFileName("sync");
                Utils.writeClassToFile(baseFolder.resolve(fileName + JSON_EXTENSION), postExportObject
                        .getExportObject());
                exportObjectFiles.add(fileName);
            } else {
                exportObjects.add(postExportObject.getExportObject());
            }
        }
        maskingSetup.setExportObjects(exportObjects);
        maskingSetup.setExportObjectFiles(exportObjectFiles);

    }

    /**
     * Backup the file format, which is a global object
     *
     * @throws ApiCallException
     */
    private void backupFileFormats() throws ApiCallException {

        GetFileFormats getFileFormats = new GetFileFormats();

        apiCallDriver.makeGetCall(getFileFormats);
        if (getFileFormats.getFileFormats() == null) {
            return;
        }
        getFileFormats.getFileFormats().forEach(fileFormat -> fileFormat.setFileFieldMetadataFiles(new ArrayList<>()));
        getFileFormats.getFileFormats().forEach(this::handleFileFieldMetadata);
        getFileFormats.getFileFormats().forEach(fileFormat -> fileFormatsIdToName.put(fileFormat.getFileFormatId(),
                fileFormat.getFileFormatName()));
        maskingSetup.setFileFormats(getFileFormats.getFileFormats());
    }


    private void handleFileFieldMetadata(FileFormat fileFormat) {
        GetFileFieldMetadatas getFileFieldMetadatas = new GetFileFieldMetadatas();
        getFileFieldMetadatas.setFile_format_id(fileFormat.getFileFormatId());
        if (isMasked) {
            getFileFieldMetadatas.setIs_masked(isMasked);
        }
        apiCallDriver.makeGetCall(getFileFieldMetadatas);
        if (getFileFieldMetadatas.getFileFieldMetadatas() == null) {
            return;
        }
        // There should only ever be 1 returned because we provided the file format id above
        if (scaled) {
            String fileName = getFileName(fileFormat.getFileFormatName());
            Utils.writeClassToFile(baseFolder.resolve(fileName + JSON_EXTENSION), getFileFieldMetadatas
                    .getFileFieldMetadatas());
            fileFormat.getFileFieldMetadataFiles().add(fileName);
        } else {
            fileFormat.setFileFieldMetadata(getFileFieldMetadatas.getFileFieldMetadatas());
        }
    }

    /**
     * Back up the applications and all of the masking job contents
     *
     * @throws ApiCallException
     */
    private void backupApplications() throws ApiCallException {

        GetApplications getApplications = new GetApplications();
        apiCallDriver.makeGetCall(getApplications);

        getFileConnectors = new GetFileConnectors();
        apiCallDriver.makeGetCall(getFileConnectors);

        getFileRulesets = new GetFileRulesets();
        apiCallDriver.makeGetCall(getFileRulesets);

        GetFileMetadatas getFileMetadatas = new GetFileMetadatas();
        apiCallDriver.makeGetCall(getFileMetadatas);

        if (getApplications.getApplications() != null) {
            ArrayList<Application> applications = new ArrayList<>();
            for (Application application : getApplications.getApplications()) {
                applications.add(handleApp(application));
            }
            maskingSetup.setApplications(applications);
        }
    }

    private void backupUsers() throws ApiCallException {
        GetUsers getUsers = new GetUsers();
        apiCallDriver.makeGetCall(getUsers);

        if (getUsers.getUsers() != null) {
            ArrayList<User> users = new ArrayList<>();
            for (User user : getUsers.getUsers()) {
                users.add(handleUser(user));
            }
            maskingSetup.setUsers(users);
        }
    }

    /**
     * Back up all domains
     *
     * @throws ApiCallException
     */
    private void backupDomains() throws ApiCallException {
        GetDomains getDomains = new GetDomains();
        apiCallDriver.makeGetCall(getDomains);

        if (getDomains.getDomains() != null) {
            ArrayList<Domain> domains = new ArrayList<>();
            for (Domain domain : getDomains.getDomains()) {
                domains.add(domain);
            }
            maskingSetup.setDomains(domains);
        }
    }

    /**
     * Backup all profile expressions and sets
     *
     * @throws ApiCallException
     */
    private void backupProfiles() throws ApiCallException {
        GetProfileSets getProfileSets = new GetProfileSets();
        apiCallDriver.makeGetCall(getProfileSets);

        GetProfileExpressions getProfileExpressions = new GetProfileExpressions();
        apiCallDriver.makeGetCall(getProfileExpressions);


        if (getProfileExpressions.getProfileExpressions() == null) {
            // profile sets require at least one profile expression
            return;
        }

        maskingSetup.setProfileExpressions(getProfileExpressions.getProfileExpressions());
        Map<Integer, String> profileExpressionMap = maskingSetup
                .getProfileExpressions()
                .stream()
                .collect(Collectors.toMap(
                        ProfileExpression::getProfileExpressionId,
                        ProfileExpression::getExpressionName));
        /*
         * For each profile expression in the profile set, map the id to the expression name (using the map above)
         * and then store the list of expression names in the profileExpressionNames list.
         */
        getProfileSets.getProfileSets()
                .stream()
                .forEach(profileSet -> profileSet
                        .setProfileExpressionNames(Arrays
                                .asList(profileSet
                                        .getProfileExpressionIds())
                                .stream()
                                .map(profileExpressionMap::get)
                                .collect(Collectors.toList())));
        getProfileSets.getProfileSets().stream().forEach(profileSet -> profileSet.setProfileExpressionIds(null));
        profileSetsIdToName = getProfileSets
                .getProfileSets()
                .stream()
                .collect(Collectors.toMap(ProfileSet::getProfileSetId, ProfileSet::getProfileSetName));
        maskingSetup.setProfileSets(getProfileSets.getProfileSets());
    }

    /*
     * All of the following functions are used to backup the specific objects they are handling and any child objects
     * they are a parent of
     */
    private Application handleApp(Application application) {

        GetEnvironments getEnvironments = new GetEnvironments();
        try {
            apiCallDriver.makeGetCall(getEnvironments);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        if (getEnvironments.getEnvironments() == null) {
            return application;
        }

        ArrayList<Environment> environments = new ArrayList<>();
        ArrayList<String> environmentFiles = new ArrayList<>();
        for (Environment environment : getEnvironments.getEnvironments()) {
            if (environment.getApplicationId().equals(application.getApplicationId())) {

                if (scaled) {
                    Environment env = handleEnv(environment);
                    String fileName = getFileName(env.getEnvironmentName());
                    Utils.writeClassToFile(baseFolder.resolve(fileName + JSON_EXTENSION), env);
                    environmentFiles.add(fileName);
                } else {
                    environments.add(handleEnv(environment));
                }
            }
        }
        application.setEnvironments(environments);
        application.setEnvironmentFiles(environmentFiles);
        return application;
    }

    private Environment handleEnv(Environment environment) {

        GetDatabaseConnectors getDatabaseConnectors = new GetDatabaseConnectors();
        getDatabaseConnectors.setEnvironmentId(environment.getEnvironmentId());

        try {
            apiCallDriver.makeGetCall(getDatabaseConnectors);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        if (getDatabaseConnectors.getDatabaseConnectors() != null) {
            ArrayList<DatabaseConnector> databaseConnectors = new ArrayList<>();
            for (DatabaseConnector databaseConnector : getDatabaseConnectors.getDatabaseConnectors()) {
                if (databaseConnector.getEnvironmentId().equals(environment.getEnvironmentId())) {
                    databaseConnectors.add(handleDatabaseConnector(databaseConnector));
                }
            }
            environment.setDatabaseConnectors(databaseConnectors);
        }

        if (getFileConnectors.getFileConnectors() != null) {
            ArrayList<FileConnector> fileConnectors = new ArrayList<>();
            for (FileConnector fileConnector : getFileConnectors.getFileConnectors()) {
                if (fileConnector.getEnvironmentId().equals(environment.getEnvironmentId())) {
                    fileConnectors.add(handleFileConnector(fileConnector));
                }
            }
            environment.setFileConnectors(fileConnectors);
        }

        return environment;
    }

    private User handleUser(User user) {

        user.setPassword(USER_PASSWORD);
        if (applicationFlags.getRedactUserInfo()) {
            user.setFirstName(REDACTED);
            user.setLastName(REDACTED);
            user.setEmail(REDACTED);
        }

        return user;
    }

    private DatabaseConnector handleDatabaseConnector(DatabaseConnector databaseConnector) {

        databaseConnector.setPassword(DATABASE_PASSWORD);

        /*
         * There is a bug in the masking application that sometimes returns the instanceName for non MSSQL connectors
         * as an empty string even though that field is not used for oracle connectors. During setup, the masking engine
         * throws an error because this field is not allowed for oracle connectors. This is a workaround until that bug
         * is fixed.
         */
        if (databaseConnector.getDatabaseType() != null && !databaseConnector.getDatabaseType().contains("MSSQL")) {
            databaseConnector.setInstanceName(null);
        }

        /*
         * There is another bug where certain fields can get returned for advanced connectors as well even though only
         * the JDBC connection string should be supplied.
         */
        if (databaseConnector.getJdbc() != null) {
            databaseConnector.setHost(null);
            databaseConnector.setInstanceName(null);
            databaseConnector.setSid(null);
            databaseConnector.setDatabaseName(null);
        }

        /*
         * MySql connectors return a schema name even though none is needed.
         */
        if (databaseConnector.getDatabaseType() != null && databaseConnector.getDatabaseType().contains("MYSQL")) {
            databaseConnector.setSchemaName(null);
        }

        GetDatabaseRulesets getDatabaseRulesets = new GetDatabaseRulesets();
        getDatabaseRulesets.setEnvironment_id(databaseConnector.getEnvironmentId());
        try {
            apiCallDriver.makeGetCall(getDatabaseRulesets);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }
        if (getDatabaseRulesets.getDatabaseRulesets() == null) {
            return databaseConnector;
        }

        ArrayList<DatabaseRuleset> databaseRulesets = new ArrayList<>();
        ArrayList<String> databaseRulesetFiles = new ArrayList<>();
        for (DatabaseRuleset databaseRuleset : getDatabaseRulesets.getDatabaseRulesets()) {
            if (databaseRuleset.getDatabaseConnectorId().equals(databaseConnector.getDatabaseConnectorId())) {
                if (scaled) {
                    DatabaseRuleset dbRuleset = handleDatabaseRuleSet(databaseRuleset);
                    String fileName = getFileName(dbRuleset.getRulesetName());
                    Utils.writeClassToFile(baseFolder.resolve(fileName + JSON_EXTENSION), dbRuleset);
                    databaseRulesetFiles.add(fileName);
                } else {
                    databaseRulesets.add(handleDatabaseRuleSet(databaseRuleset));
                }
            }
        }
        databaseConnector.setDatabaseRulesetFiles(databaseRulesetFiles);
        databaseConnector.setDatabaseRulesets(databaseRulesets);
        return databaseConnector;
    }

    private FileConnector handleFileConnector(FileConnector fileConnector) {
        if (fileConnector.getConnectionInfo().getSshKey() == null) {
            fileConnector.getConnectionInfo().setPassword(SFTP_PASSWORD);
        }
        if (getFileRulesets.getFileRulesets() == null) {
            return fileConnector;

        }

        ArrayList<FileRuleset> fileRulesets = new ArrayList<>();
        for (FileRuleset fileRuleset : getFileRulesets.getFileRulesets()) {
            if (fileRuleset.getFileConnectorId().equals(fileConnector.getFileConnectorId())) {
                fileRulesets.add(handleFileRuleSet(fileRuleset));
            }
        }
        fileConnector.setFileRulesets(fileRulesets);
        return fileConnector;
    }

    private ArrayList<MaskingJob> gatherMaskingJobs(Integer environmentId, Integer parentId) {
        GetMaskingJobs getMaskingJobs = new GetMaskingJobs();
        getMaskingJobs.setEnvironment_id(environmentId);
        try {
            apiCallDriver.makeGetCall(getMaskingJobs);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        if (getMaskingJobs.getMaskingJobs() == null) {
            return null;
        }

        ArrayList<MaskingJob> maskingJobs = new ArrayList<>();
        for (MaskingJob maskingJob : getMaskingJobs.getMaskingJobs()) {
            if (maskingJob.getRulesetId().equals(parentId)) {
                if (maskingJob.getOnTheFlyMasking()) {
                    handleOtfMaskingJob(maskingJob);
                }
                maskingJobs.add(maskingJob);
            }
        }

        return maskingJobs;
    }

    private ArrayList<ProfilingJob> gatherProfileJobs(Integer environmentId, Integer parentId) {
        GetProfilingJobs getProfilingJobs = new GetProfilingJobs();
        getProfilingJobs.setEnvironment_id(environmentId);
        try {
            apiCallDriver.makeGetCall(getProfilingJobs);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        if (getProfilingJobs.getProfilingJobs() == null) {
            return null;
        }

        ArrayList<ProfilingJob> profilingJobs = new ArrayList<>();
        for (ProfilingJob profilingJob : getProfilingJobs.getProfilingJobs()) {
            if (profilingJob.getRulesetId().equals(parentId)) {
                // Store the profile set name based on the set id
                profilingJob.setProfileSetName(profileSetsIdToName.get(profilingJob.getProfileSetId()));
                profilingJob.setProfileSetId(null);
                profilingJobs.add(profilingJob);
            }
        }
        return profilingJobs;
    }

    private FileRuleset handleFileRuleSet(FileRuleset fileRuleset) {


        fileRuleset.setMaskingJobs(gatherMaskingJobs(fileRuleset.getEnvironmentId(), fileRuleset.getFileRulesetId()));
        fileRuleset.setProfilingJobs(gatherProfileJobs(fileRuleset.getEnvironmentId(), fileRuleset.getFileRulesetId()));

        GetFileMetadatas getFileMetadatas = new GetFileMetadatas();
        getFileMetadatas.setRuleset_id(fileRuleset.getFileRulesetId());
        apiCallDriver.makeGetCall(getFileMetadatas);

        if (getFileMetadatas.getFileMetadatas() == null) {
            return fileRuleset;
        }

        for (FileMetadata fileMetadata : getFileMetadatas.getFileMetadatas()) {
            if (fileFormatsIdToName.containsKey(fileMetadata.getFileMetadataId())) {
                fileMetadata.setFileFormatName(fileFormatsIdToName.get(fileMetadata.getFileFormatId()));
            }
        }
        fileRuleset.setFileMetadatas(getFileMetadatas.getFileMetadatas());
        return fileRuleset;
    }

    private DatabaseRuleset handleDatabaseRuleSet(DatabaseRuleset databaseRuleset) {

        GetTableMetadatas getTableMetadatas = new GetTableMetadatas();
        getTableMetadatas.setRuleset_id(databaseRuleset.getDatabaseRulesetId());
        try {
            apiCallDriver.makeGetCall(getTableMetadatas);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        if (getTableMetadatas.getTableMetadatas() != null) {
            ArrayList<TableMetadata> tableMetadatas = new ArrayList<>();
            ArrayList<String> tableMetadataFiles = new ArrayList<>();
            for (TableMetadata tableMetadata : getTableMetadatas.getTableMetadatas()) {
                if (tableMetadata.getRulesetId().equals(databaseRuleset.getDatabaseRulesetId())) {
                    tableMetadata = handleTableMetadata(tableMetadata);
                    if (scaled) {
                        String fileName = getFileName(tableMetadata.getTableName());
                        Utils.writeClassToFile(baseFolder.resolve(fileName + JSON_EXTENSION), tableMetadata);
                        tableMetadataFiles.add(fileName);
                    } else {
                        tableMetadatas.add(tableMetadata);
                    }
                }
            }
            databaseRuleset.setTableMetadataFiles(tableMetadataFiles);
            databaseRuleset.setTableMetadatas(tableMetadatas);
        }

        databaseRuleset.setMaskingJobs(gatherMaskingJobs(databaseRuleset.getEnvironmentId(), databaseRuleset
                .getDatabaseRulesetId()));
        databaseRuleset.setProfilingJobs(gatherProfileJobs(databaseRuleset.getEnvironmentId(), databaseRuleset
                .getDatabaseRulesetId()));

        return databaseRuleset;
    }

    private void handleOtfMaskingJob(MaskingJob maskingJob) {
        Integer connectorId = maskingJob.getOnTheFlyMaskingSource().getConnectorId();

        GetDatabaseConnectors getDatabaseConnectors = new GetDatabaseConnectors();
        try {
            apiCallDriver.makeGetCall(getDatabaseConnectors);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        String connName;
        Integer envId;
        DatabaseConnector dbConn = getDatabaseConnectors
                .getDatabaseConnectors()
                .stream()
                .filter(conn -> conn.getDatabaseConnectorId().equals(connectorId))
                .findFirst()
                .orElse(null);

        if (dbConn == null) {
            GetFileConnectors getFileConnectors = new GetFileConnectors();
            apiCallDriver.makeGetCall(getFileConnectors);
            FileConnector filConn = getFileConnectors
                    .getFileConnectors()
                    .stream()
                    .filter(conn -> conn.getFileConnectorId().equals(connectorId))
                    .findFirst()
                    .orElseThrow(RuntimeException::new);

            connName = filConn.getConnectorName();
            envId = filConn.getEnvironmentId();
        } else {
            connName = dbConn.getConnectorName();
            envId = dbConn.getEnvironmentId();
        }
        maskingJob.getOnTheFlyMaskingSource().setConnectorName(connName);


        GetEnvironments getEnvironments = new GetEnvironments();
        try {
            apiCallDriver.makeGetCall(getEnvironments);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        Environment sourceEnvironment = getEnvironments
                .getEnvironments()
                .stream()
                .filter(env -> env.getEnvironmentId().equals(envId))
                .findFirst()
                .orElseThrow(RuntimeException::new);

        maskingJob.getOnTheFlyMaskingSource().setEnvironmentName(sourceEnvironment.getEnvironmentName());

    }

    private TableMetadata handleTableMetadata(TableMetadata tableMetadata) {

        GetColumnMetadatas getColumnMetadatas = new GetColumnMetadatas();
        getColumnMetadatas.setTable_metadata_id(tableMetadata.getTableMetadataId());
        if (isMasked) {
            getColumnMetadatas.setIs_masked(isMasked);
        }

        try {
            apiCallDriver.makeGetCall(getColumnMetadatas);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }
        if (getColumnMetadatas.getColumnMetadatas() == null) {
            return tableMetadata;
        }

        ArrayList<ColumnMetadata> columnMetadatas = new ArrayList<>();
        for (ColumnMetadata columnMetadata : getColumnMetadatas.getColumnMetadatas()) {
            if (columnMetadata.getTableMetadataId().equals(tableMetadata.getTableMetadataId())) {
                /*
                 * There is a bug in the masking application that returns empty date formats in certain situations where
                 * the date format should be null. We need to manually null the date format.
                 */
                if (columnMetadata.getDateFormat() != null && columnMetadata.getDateFormat().equals("")) {
                    columnMetadata.setDateFormat(null);
                }
                columnMetadatas.add(columnMetadata);
            }
        }
        tableMetadata.setColumnMetadatas(columnMetadatas);
        return tableMetadata;
    }

}
