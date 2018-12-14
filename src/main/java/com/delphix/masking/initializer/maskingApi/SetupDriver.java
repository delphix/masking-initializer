package com.delphix.masking.initializer.maskingApi;

import static com.delphix.masking.initializer.Constants.BASE_FILE;
import static com.delphix.masking.initializer.Constants.JSON_EXTENSION;
import static com.delphix.masking.initializer.Constants.MASKING;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.delphix.masking.initializer.ApplicationFlags;
import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.exception.MissingDataException;
import com.delphix.masking.initializer.maskingApi.endpointCaller.ApiCallDriver;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetColumnMetadatas;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetDatabaseConnectors;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetEnvironments;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileConnectors;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileFieldMetadatas;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetFileFormats;
import com.delphix.masking.initializer.maskingApi.endpointCaller.GetProfileSets;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostApplication;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostDatabaseConnector;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostDatabaseRuleset;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostDomain;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostEnvironment;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostFileConnector;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostFileFormat;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostFileMetadata;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostFileRuleset;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostImportObject;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostMaskingJob;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostProfileExpression;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostProfileSet;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostProfilingJob;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostTableMetadata;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PostUser;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PutColumnMetadata;
import com.delphix.masking.initializer.maskingApi.endpointCaller.PutFileFieldMetadata;
import com.delphix.masking.initializer.pojo.Application;
import com.delphix.masking.initializer.pojo.ColumnMetadata;
import com.delphix.masking.initializer.pojo.DatabaseConnector;
import com.delphix.masking.initializer.pojo.DatabaseRuleset;
import com.delphix.masking.initializer.pojo.Domain;
import com.delphix.masking.initializer.pojo.Environment;
import com.delphix.masking.initializer.pojo.ExportObject;
import com.delphix.masking.initializer.pojo.FileConnector;
import com.delphix.masking.initializer.pojo.FileFieldMetadata;
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

public class SetupDriver {

    Logger logger = LogManager.getLogger(SetupDriver.class);

    private ApiCallDriver apiCallDriver;
    private ApplicationFlags applicationFlags;
    private MaskingSetup maskingSetup;
    private Path baseFolder;
    private boolean replace = false;
    private boolean onlyConnectors = true;
    private boolean ignoreErrors = false;

    private Map<String, Integer> fileFormats = new HashMap<>();

    /**
     * Initializes {@link #maskingSetup} with only the contents of the file
     *
     * @param setupFile Path to the setup file
     */
    public SetupDriver(String setupFile) {
        try {
            maskingSetup = Utils.getClassFromFile(Paths.get(setupFile), MaskingSetup.class);
            baseFolder = Paths.get(setupFile).getParent();
            init();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Initialize the masking setup object with the provided flags
     *
     * @param applicationFlags contains all info regarding engine setup and flag configs
     */
    public SetupDriver(ApplicationFlags applicationFlags) {
        try {
            Path backupFile = applicationFlags.getFile();
            if (backupFile.toFile().isDirectory()) {
                baseFolder = backupFile;
                backupFile = backupFile.resolve(BASE_FILE);
            } else {
                baseFolder = backupFile.getParent();
            }
            maskingSetup = Utils.getClassFromFile(backupFile, MaskingSetup.class);
            // Override any of the passed in options that are not null
            if (applicationFlags.getHost() != null) {
                maskingSetup.setHost(applicationFlags.getHost());
            }
            if (applicationFlags.getPort() != null) {
                maskingSetup.setPort(applicationFlags.getPort());
            }
            if (applicationFlags.getApiPath() != null) {
                maskingSetup.setApiPath(applicationFlags.getApiPath());
            }

            if (applicationFlags.getAuthToken() != null) {
                maskingSetup.setAuthToken(applicationFlags.getAuthToken());
            } else {
                if (applicationFlags.getUsername() != null) {
                    maskingSetup.setUsername(applicationFlags.getUsername());
                }
                if (applicationFlags.getPassword() != null) {
                    maskingSetup.setPassword(applicationFlags.getPassword());
                }
            }
            this.replace = applicationFlags.getReplace();
            this.ignoreErrors = applicationFlags.isIgnoreErrors();
            this.applicationFlags = applicationFlags;
            init();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Initializes the api call driver and double checks that the api path is set. If it is not is writes the default
     * value to it. This function should be called as the last piece in every constructor.
     */
    private void init() {
        try {

            if (maskingSetup.getApiPath() == null) {
                maskingSetup.setApiPath(MASKING);
            }

            if (maskingSetup.getScaled() == null) {
                maskingSetup.setScaled(false);
            }
            if (maskingSetup.getAuthToken() == null) {
                apiCallDriver = new ApiCallDriver(
                        maskingSetup.getHost(),
                        maskingSetup.getUsername(),
                        maskingSetup.getPassword(),
                        maskingSetup.getPort(),
                        maskingSetup.getApiPath(),
                        replace,
                        applicationFlags.getIsSslEnabled()
                        );
            } else {
                apiCallDriver = new ApiCallDriver(
                        maskingSetup.getHost(),
                        maskingSetup.getAuthToken(),
                        maskingSetup.getPort(),
                        maskingSetup.getApiPath(),
                        replace,
                        applicationFlags.getIsSslEnabled());
            }

        } catch (ApiCallException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Actually makes the necessary calls to the masking engine in order to setup the engine based on the contents of
     * {@link #maskingSetup} that was initialized in the constructor
     */
    public void run() {
        try {

            // Checks if there are objects in each section that need to be uploaded

            if (maskingSetup.getExportObjects() != null) {
                handleExportObject(maskingSetup.getExportObjects());
            }

            if (maskingSetup.getExportObjectFiles() != null) {
                handleExportObjectFiles(maskingSetup.getExportObjectFiles());
            }

            if (maskingSetup.getDomains() != null) {
                handleDomains(maskingSetup.getDomains());
            }

            if (maskingSetup.getUsers() != null) {
                handleUsers(maskingSetup.getUsers());
            }

            Map<String, Integer> profileExpressionMap = null;
            if (maskingSetup.getProfileExpressions() != null) {
                profileExpressionMap = handleProfilerExpressions(maskingSetup.getProfileExpressions());
            }

            if (maskingSetup.getProfileSets() != null) {
                handleProfilerSets(maskingSetup.getProfileSets(), profileExpressionMap);
            }

            if (maskingSetup.getFileFormats() != null) {
                handleFileFormats(maskingSetup.getFileFormats());
            }

            if (maskingSetup.getApplications() != null) {
                handleApps(maskingSetup.getApplications());
                onlyConnectors = false;
                handleApps(maskingSetup.getApplications());
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ApiCallException e) {
            logger.error(e.getMessage());
        }
    }

    /*
     * The following functions are responsible for making the necessary API calls for the masking object that they are
     * handling. After making the appropriate call, they check if there are any children objects that need to be setup
     * and calls the necessary function to set them up as well.
     */

    private void handleFileFormats(List<FileFormat> fileFormats) throws IOException {
        for (FileFormat fileFormat : fileFormats) {
            PostFileFormat postFileFormat = new PostFileFormat(fileFormat, baseFolder);
            apiCallDriver.makePostCall(postFileFormat);
            Integer id = Integer.valueOf(postFileFormat.getId());
            this.fileFormats.put(fileFormat.getFileFormatName(), id);

            if (fileFormat.getFileFieldMetadata() != null) {
                handleFileFieldMetadata(fileFormat.getFileFieldMetadata(), id);
            }

            if (fileFormat.getFileFieldMetadataFiles() != null) {
                for (String fileFieldMetadataFile : fileFormat.getFileFieldMetadataFiles()) {
                    FileFieldMetadata[] fileFieldMetadatas = Utils.getClassFromFile(baseFolder.resolve
                            (fileFieldMetadataFile + JSON_EXTENSION), FileFieldMetadata[].class);
                    handleFileFieldMetadata(Arrays.asList(fileFieldMetadatas), id);
                }
            }
        }

        // Add any pre-existing file formats to the map so file metadata can be uploaded properly
        GetFileFormats getFileFormats = new GetFileFormats();
        apiCallDriver.makeGetCall(getFileFormats);
        for (FileFormat fileFormat : getFileFormats.getFileFormats()) {
            if (!this.fileFormats.containsKey(fileFormat.getFileFormatName())) {
                this.fileFormats.put(fileFormat.getFileFormatName(), fileFormat.getFileFormatId());
            }
        }
    }

    private void handleFileFieldMetadata(List<FileFieldMetadata> fileFieldMetadatas, Integer fileFormatId) {

        GetFileFieldMetadatas getFileFieldMetadatas = new GetFileFieldMetadatas();
        getFileFieldMetadatas.setFile_format_id(fileFormatId);

        apiCallDriver.makeGetCall(getFileFieldMetadatas);

        if (getFileFieldMetadatas.getFileFieldMetadatas() == null) {
            return;
        }

        for (FileFieldMetadata fileFieldMetadata : fileFieldMetadatas) {

            Integer id = getFileFieldMetadatas
                    .getFileFieldMetadatas()
                    .stream()
                    .filter(f -> f.getFileFormatId().equals(fileFormatId) && f.getFieldName().equals
                            (fileFieldMetadata.getFieldName()))
                    .findFirst()
                    .orElseThrow(RuntimeException::new)
                    .getFileFieldMetadataId();

            fileFieldMetadata.setFileFieldMetadataId(id);

            PutFileFieldMetadata putFileFieldMetadata = new PutFileFieldMetadata(fileFieldMetadata);
            apiCallDriver.makePutCall(putFileFieldMetadata);
        }


    }

    private void handleExportObject(List<ExportObject> exportObjects) throws ApiCallException {
        for (ExportObject exportObject : exportObjects) {
            PostImportObject postImportObject = new PostImportObject(exportObject, replace);
            apiCallDriver.makePostCall(postImportObject);
        }
    }

    private void handleExportObjectFiles(List<String> exportObjectFiles) throws ApiCallException, IOException {
        for (String filename : exportObjectFiles) {
            Path exportObjectFilePath = baseFolder.resolve(filename + JSON_EXTENSION);
            logger.info("Reading from file: " + exportObjectFilePath);
            ExportObject exportObject = Utils.getClassFromFile(exportObjectFilePath,
                    ExportObject.class);
            PostImportObject postImportObject = new PostImportObject(exportObject, replace);
            apiCallDriver.makePostCall(postImportObject);
        }
    }

    private void handleDomains(List<Domain> domains) throws ApiCallException {
        for (Domain domain : domains) {
            apiCallDriver.makePostCall(new PostDomain(domain));
        }
    }

    private void handleUsers(List<User> users) throws ApiCallException {
        for (User user : users) {
            apiCallDriver.makePostCall(new PostUser(user));
        }
    }

    private Map<String, Integer> handleProfilerExpressions(List<ProfileExpression> profileExpressions) throws
            ApiCallException {
        Map<String, Integer> profileExpressionMap = new HashMap<>();
        for (ProfileExpression profileExpression : profileExpressions) {
            PostProfileExpression postProfileExpression = new PostProfileExpression(profileExpression);
            apiCallDriver.makePostCall(postProfileExpression);
            profileExpressionMap.put(profileExpression.getExpressionName(), Integer.valueOf(postProfileExpression
                    .getId()));
        }
        return profileExpressionMap;
    }

    private void handleProfilerSets(List<ProfileSet> profileSets, Map<String, Integer> profileExpressionIdMap) throws
            ApiCallException {
        for (ProfileSet profileSet : profileSets) {
            List<Integer> profileExpressionIds = new ArrayList<>();
            /*
             * BACKWARDS_COMPATIBLE(0.4.0)
             * Starting in V0.4.0, profile expression are no longer stored under the profile set. Instead, they are
             * stored as their own global object in the maskingSetup object and the profile set merely stores the name
             * of them.
             *
             * The below loop remains in order to allow for pre V0.4.0 backups to still be setup with newer versions
             * of the script.
             */
            if (profileSet.getProfileExpressions() != null) {
                for (ProfileExpression profileExpression : profileSet.getProfileExpressions()) {
                    PostProfileExpression postProfileExpression = new PostProfileExpression(profileExpression);
                    apiCallDriver.makePostCall(postProfileExpression);
                    profileExpressionIds.add(Integer.valueOf(postProfileExpression.getId()));
                }
            }

            if (profileExpressionIdMap != null) {
                for (String profileExpressionName : profileSet.getProfileExpressionNames()) {
                    if (!profileExpressionIdMap.containsKey(profileExpressionName)) {
                        throw new RuntimeException(
                                String.format(
                                        "Profile Expression with name [%s] was not exported but is needed for profile" +
                                                " set with name [%s].",
                                        profileExpressionName,
                                        profileSet.getProfileSetName()));
                    }
                    profileExpressionIds.add(profileExpressionIdMap.get(profileExpressionName));
                }
            }
            Integer[] ids = new Integer[profileExpressionIds.size()];
            ids = profileExpressionIds.toArray(ids);
            profileSet.setProfileExpressionIds(ids);
            PostProfileSet PostProfileSet = new PostProfileSet(profileSet);
            apiCallDriver.makePostCall(PostProfileSet);
        }
    }

    private void handleApps(List<Application> applications) throws IOException, ApiCallException {
        for (Application application : applications) {
            apiCallDriver.makePostCall(new PostApplication(application));

            if (application.getEnvironmentFiles() != null) {
                for (String environmentFile : application.getEnvironmentFiles()) {
                    Path envFilePath = baseFolder.resolve(environmentFile +
                            JSON_EXTENSION);
                    logger.info("Reading from: " + envFilePath);
                    Environment environment = Utils.getClassFromFile(envFilePath, Environment.class);
                    handleEnv(environment, application.getApplicationName());
                }
            }

            if (application.getEnvironments() != null) {
                handleEnvs(application.getEnvironments(), application.getApplicationName());
            }
        }
    }

    private void handleEnv(Environment environment, String appName) throws IOException, ApiCallException {
        environment.setApplication(appName);
        PostEnvironment PostEnvironment = new PostEnvironment(environment);
        apiCallDriver.makePostCall(PostEnvironment);
        Integer envId = Integer.valueOf(PostEnvironment.getId());
        ;

        if (environment.getDatabaseConnectors() != null) {
            handleDatabaseConnectors(environment.getDatabaseConnectors(), envId);
        }

        if (environment.getFileConnectors() != null) {
            handleFileConnector(environment.getFileConnectors(), envId);
        }
    }

    private void handleEnvs(List<Environment> environments, String appName) throws IOException, ApiCallException {
        for (Environment environment : environments) {
            handleEnv(environment, appName);
        }
    }

    private void handleDatabaseConnectors(List<DatabaseConnector> databaseConnectors, Integer envId) throws
            IOException, ApiCallException {
        for (DatabaseConnector databaseConnector : databaseConnectors) {
            PostDatabaseConnector postDatabaseConnector = new PostDatabaseConnector(databaseConnector, envId);
            apiCallDriver.makePostCall(postDatabaseConnector);
            if (onlyConnectors == true) {
                continue;
            }
            if (databaseConnector.getDatabaseRulesetFiles() != null) {
                for (String dbRuleSetFile : databaseConnector.getDatabaseRulesetFiles()) {
                    DatabaseRuleset dbRuleset = Utils.getClassFromFile(baseFolder.resolve(dbRuleSetFile +
                            JSON_EXTENSION), DatabaseRuleset.class);
                    handleDatabaseRuleset(dbRuleset, Integer.valueOf(postDatabaseConnector.getId()));
                }
            }
            if (databaseConnector.getDatabaseRulesets() != null) {
                handleDatabaseRulesets(databaseConnector.getDatabaseRulesets(), Integer.valueOf(postDatabaseConnector
                        .getId()));
            }
        }
    }

    private void handleFileConnector(List<FileConnector> fileConnectors, Integer envId) throws ApiCallException {
        for (FileConnector fileConnector : fileConnectors) {
            PostFileConnector postFileConnector = new PostFileConnector(fileConnector, envId);
            apiCallDriver.makePostCall(postFileConnector);
            if (onlyConnectors == true) {
                continue;
            }
            if (fileConnector.getFileRulesets() != null) {
                handleFileRuleset(fileConnector.getFileRulesets(), Integer.valueOf(postFileConnector.getId()));
            }
        }
    }

    private void handleFileRuleset(List<FileRuleset> fileRulesets, Integer connectorId) throws ApiCallException {
        for (FileRuleset fileRuleset : fileRulesets) {
            fileRuleset.setFileConnectorId(connectorId);
            PostFileRuleset postFileRuleset = new PostFileRuleset(fileRuleset);
            apiCallDriver.makePostCall(postFileRuleset);
            if (fileRuleset.getFileMetadatas() != null) {
                handleFileMetadata(fileRuleset.getFileMetadatas(), Integer.valueOf(postFileRuleset.getId()));
            }

            if (fileRuleset.getMaskingJobs() != null) {
                handleMaskingJob(fileRuleset.getMaskingJobs(), Integer.valueOf(postFileRuleset.getId()));
            }

            if (fileRuleset.getProfilingJobs() != null) {
                handleProfilingJob(fileRuleset.getProfilingJobs(), Integer.valueOf(postFileRuleset.getId()));
            }
        }
    }

    private void handleFileMetadata(List<FileMetadata> fileMetadatas, Integer rulesetId) throws ApiCallException {
        for (FileMetadata fileMetadatass : fileMetadatas) {
            if (!fileFormats.containsKey(fileMetadatass.getFileFormatName())) {
                // TODO do GET call to test if this is true first (only has uploaded ones currently
                throw new MissingDataException(String.format("File format %s was not uploaded", fileMetadatass
                        .getFileFormatName()));
            }
            fileMetadatass.setFileFormatId(fileFormats.get(fileMetadatass.getFileFormatName()));
            PostFileMetadata postFileMetadata = new PostFileMetadata(fileMetadatass, rulesetId);
            apiCallDriver.makePostCall(postFileMetadata);
        }
    }

    private void handleDatabaseRuleset(DatabaseRuleset databaseRuleset, Integer connectorId) throws IOException,
            ApiCallException {
        PostDatabaseRuleset PostDatabaseRuleset = new PostDatabaseRuleset(databaseRuleset, connectorId);
        apiCallDriver.makePostCall(PostDatabaseRuleset);
        if (databaseRuleset.getTableMetadatas() != null) {
            handleTableMetadatas(databaseRuleset.getTableMetadatas(), Integer.valueOf(PostDatabaseRuleset.getId()));
        }
        if (databaseRuleset.getTableMetadataFiles() != null) {
            for (String tableMetadataFile : databaseRuleset.getTableMetadataFiles()) {
                TableMetadata tableMetadata = Utils.getClassFromFile(baseFolder.resolve(tableMetadataFile +
                        JSON_EXTENSION), TableMetadata.class);
                handleTableMetadata(tableMetadata, Integer.valueOf(PostDatabaseRuleset.getId()));
            }
        }
        if (databaseRuleset.getMaskingJobs() != null) {
            handleMaskingJob(databaseRuleset.getMaskingJobs(), Integer.valueOf(PostDatabaseRuleset.getId()));
        }
        if (databaseRuleset.getProfilingJobs() != null) {
            handleProfilingJob(databaseRuleset.getProfilingJobs(), Integer.valueOf(PostDatabaseRuleset.getId()));
        }
    }

    private void handleDatabaseRulesets(List<DatabaseRuleset> databaseRulesets, Integer connectorId) throws
            IOException, ApiCallException {
        for (DatabaseRuleset databaseRuleset : databaseRulesets) {
            handleDatabaseRuleset(databaseRuleset, connectorId);
        }
    }

    private void handleProfilingJob(List<ProfilingJob> profilingJobs, Integer ruleSetId) throws ApiCallException {
        /*
         * The profile set name is stored in the backup so we have to get all profile sets on the engine so we can map
         * the name of the profile set to its ID so that we can properly create the profile job.
         */
        GetProfileSets getProfileSets = new GetProfileSets();
        apiCallDriver.makeGetCall(getProfileSets);
        Map<String, Integer> profileSetsNameToId = getProfileSets
                .getProfileSets()
                .stream()
                .collect(Collectors.toMap(ProfileSet::getProfileSetName, ProfileSet::getProfileSetId));
        for (ProfilingJob profilingJob : profilingJobs) {

            // Map the profile set name to the correct ID and fail if we cannot find the right profile set
            String profileSetName = profilingJob.getProfileSetName();
            if (!profileSetsNameToId.containsKey(profileSetName)) {
                logger.error("Expecting profile job with name {} to exist, but none found", profileSetName);
                throw new RuntimeException(String.format("Missing profile set."));
            }

            Integer profilerSetId = profileSetsNameToId.get(profileSetName);
            logger.debug("Mapping profile set name {} to ID {}", profileSetName, profilerSetId);
            profilingJob.setProfileSetId(profilerSetId);

            PostProfilingJob postProfilingJob = new PostProfilingJob(profilingJob, ruleSetId);
            apiCallDriver.makePostCall(postProfilingJob);
        }
    }

    private void handleMaskingJob(List<MaskingJob> maskingJobs, Integer ruleSetId) throws ApiCallException {
        for (MaskingJob maskingJob : maskingJobs) {
            if (maskingJob.getOnTheFlyMasking()) {
                handleOtfMaskingJob(maskingJob);
            }
            PostMaskingJob PostMaskingJob = new PostMaskingJob(maskingJob, ruleSetId);
            apiCallDriver.makePostCall(PostMaskingJob);
        }
    }

    private void handleOtfMaskingJob(MaskingJob maskingJob) {
        GetEnvironments getEnvironments = new GetEnvironments();
        try {
            apiCallDriver.makeGetCall(getEnvironments);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        Integer envId = getEnvironments
                .getEnvironments()
                .stream()
                .filter(env -> env.getEnvironmentName().equals(maskingJob.getOnTheFlyMaskingSource()
                        .getEnvironmentName()))
                .findFirst()
                .orElseThrow(RuntimeException::new)
                .getEnvironmentId();

        GetDatabaseConnectors getDatabaseConnectors = new GetDatabaseConnectors();
        getDatabaseConnectors.setEnvironmentId(envId);

        try {
            apiCallDriver.makeGetCall(getDatabaseConnectors);
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }
        Integer connId;
        DatabaseConnector dbConn = getDatabaseConnectors
                .getDatabaseConnectors()
                .stream()
                .filter(conn -> conn.getConnectorName().equals(maskingJob.getOnTheFlyMaskingSource().getConnectorName
                        ()))
                .findFirst()
                .orElse(null);

        if (dbConn == null) {
            GetFileConnectors getFileConnectors = new GetFileConnectors();
            getFileConnectors.setEnvironment_id(envId);
            apiCallDriver.makeGetCall(getFileConnectors);
            FileConnector filConn = getFileConnectors
                    .getFileConnectors()
                    .stream()
                    .filter(conn -> conn.getConnectorName().equals(maskingJob.getOnTheFlyMaskingSource()
                            .getConnectorName()))
                    .findFirst()
                    .orElseThrow(RuntimeException::new);
            connId = filConn.getFileConnectorId();

        } else {
            connId = dbConn.getDatabaseConnectorId();
        }

        maskingJob.getOnTheFlyMaskingSource().setConnectorId(connId);

    }

    private void handleTableMetadata(TableMetadata tableMetadata, Integer ruleSetId) throws ApiCallException {
        PostTableMetadata PostTableMetadata = new PostTableMetadata(tableMetadata, ruleSetId);
        try {
            apiCallDriver.makePostCall(PostTableMetadata);
            if (tableMetadata.getColumnMetadatas() != null) {
                handleColumnMetadata(tableMetadata.getColumnMetadatas(), Integer.valueOf(PostTableMetadata.getId()));
            }
        } catch (ApiCallException e) {
            if (ignoreErrors) {
                logger.error("Error while creating table metadata: " + e.getMessage());
                logger.info("Continuing on because ignore errors is specified.");
            } else {
                throw e;
            }
        }
    }

    private void handleTableMetadatas(List<TableMetadata> tableMetadatas, Integer ruleSetId) throws ApiCallException {
        for (TableMetadata tableMetadata : tableMetadatas) {
            handleTableMetadata(tableMetadata, ruleSetId);
        }
    }

    private void handleColumnMetadata(List<ColumnMetadata> columnMetadatas, Integer tableMetadataId) throws
            ApiCallException {
        GetColumnMetadatas getColumnMetadatas = new GetColumnMetadatas();
        getColumnMetadatas.setTable_metadata_id(tableMetadataId);
        apiCallDriver.makeGetCall(getColumnMetadatas);
        for (ColumnMetadata columnMetadata : columnMetadatas) {
            for (ColumnMetadata columnMetadata1 : getColumnMetadatas.getColumnMetadatas()) {
                if (columnMetadata.getColumnName().equalsIgnoreCase(columnMetadata1.getColumnName())
                        && tableMetadataId.equals(columnMetadata1.getTableMetadataId())) {
                    columnMetadata.setColumnMetadataId(columnMetadata1.getColumnMetadataId());
                    break;
                }
            }
            if (columnMetadata.getColumnMetadataId() == null) {
                String error = String.format("Unable to find column with name %s in table with id " +
                        "%s", columnMetadata.getColumnName(), tableMetadataId);
                if (ignoreErrors) {
                    logger.error(error);
                    logger.info("Continuing on because ignore errors is specified.");
                    continue;
                } else {
                    throw new MissingDataException(error);
                }

            }
            PutColumnMetadata putColumnMetadata = new PutColumnMetadata(columnMetadata);
            apiCallDriver.makePutCall(putColumnMetadata);
        }

    }
}

