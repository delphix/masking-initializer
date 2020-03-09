# Description

This repository contains a command line tool that can be used to back up and
restore a masking engine using the APIv5 endpoints. It allows backup/restoration
of the following objects.

##### Masking Job related objects
 * Applications
 * Environments
 * Connectors (Database and File)
 * Rulesets (Database and File)
 * Table metadata
 * File metadata
 * Column metadata
 * Masking jobs
 * Profiling jobs
 * Mounts

##### Global Objects
 * Profile Sets
 * Profile Expressions
 * Domains
 * Syncable Objects
 * File formats
 * File field metadata
 * Users
 * JDBC Drivers

# Version

The current `2.X` version is `2.3.3`.

The current `1.X` version is `1.1.0`.

Please see the [Change log](CHANGELOG.md) for
information on each version.

The 2.X.X versions work with the 6.0.X.X versions of the masking application.
The 1.X.X versions work with the 5.3.X.X versions of the masking application.
The 0.X.X versions work with the 5.2.X.X versions of the masking application.
There is no guarantee of forwards or backwards compatibility.

# Install

There are a couple of ways to obtain a copy of this tool.

1. You can now download the binaries under the "release" menu option.
2. Clone this repository, checkout the version you want (branch name `Vx.y.z` and either run `./gradlew installDist` to generate the files
in `/build/install/dlpx-masking-initializer` or `./gradlew distZip` to generate the same zip file
that exists in google drive.

One you have built and/or unzipped this file, the command line executable is in `./dlpx-masking-initializer/bin/`
and can be used as described below! **Important**: You must execute the executable from within `./dlpx-masking-initializer/bin/`
otherwise the tool may not work properly.

# Usage

The tools works by storing masking engine objects in a `yaml` file (similar to JSON) and
restoring a masking engine from that same file.

**It is extremely important to note that the IDs of these objects are not guaranteed
to be the same between engines. IDs are determined by the engine and are dependent
on other objects that may or may not already exist in that engine.**

Please see example `yaml` files in the [example folder](examples/).

# Flags

The following flags are available to help run this tool.

 * `-H` specifies the host of the masking engine. Can be `localhost`, `12.23.32.12`, `youCompanyMasking.com`. Can also be specified by environment variable `MASKING_HOST`.
 * `-p` specifies the port that the masking engine is running on. Can also be specified by environment variable `MASKING_PORT`.
 * `-u` specifies the username of an admin user that can be used to login to the masking engine. Can also be specified by environment variable `MASKING_USER`.
 * `-P` specifies the password of the admin user. Can also be specified by environment variable `MASKING_PASSWORD`.
 * `-t` specifies an authorization token. This is be specified instead of a username and password
 * `-f` specifies the file or folder to either read from (for restoration) or write to (for backup)
 * `-b` runs in backup mode
 * `-s` runs in setup (restoration) mode
 * `-o` when running in backup mode, this flag must be specified if the file supplied in
 `-f` already exists and grants the tool permission to overwrite its contents.
 * `-g` when running in backup mode, this flag specifies that global objects should be backed up as well.
 Currently these objects include profiler sets/expressions and domains.
 * `-e` when running in backup mode, this flag specifies that engine sync objects should be backed up as well.
 * `-d` specifies the password file to use. **NOT FULLY IMPLEMENTED**
 * `-r` when running in setup mode, if creating an object returns a conflict, `409`, then instead of skipping
 the creation of that object, the call gets turned into a `PUT` operation and overwrites the existing object.
 * `-m` when backing up, only backup those columns that are actually being masked. This will reduce this
 size of the backup file but can lead to the columns that are being masked that were not masked in the backup
 remaining masked even after restoration. This flag is good if you are starting from a clean engine for the restoration.
 * `-c` Runs the tool in scale mode. This mode is great if their are 1000s of objects to back up. Instead of backing
 everything up to a single file, it creates the backup in multiple files, reducing the amount of memory that is needed
 to run the tool. If you are using scale mode then the option passed into `-f` should be a folder, not a file.
 * `-l` Sets the log level. By default this value is set to `INFO`, but can also be set to
  `TRACE`, `DEBUG`, `WARN`, `ERROR`.
 * `-i` When used, the flag causes any errors at the tablemetadata or columnmetadata
 level to be skipped. This means that if a table no longer exists in a database then instead
 of the whole setup failing, that table will be skipped. Similar to columns missing from tables.
 This flag is only used in setup mode.
 * `-R` when running in backup mode, redacts user information (first name, last name, email address)
 and replaces with `REDACTED`.
 * `-S` when using this tool with a masking application over SSL, supplying this flag is necessary. Note that no certificate
 checks are done. All certificates are trusted.

 The `-f` flag is always required and one and only one of `-s` or `-b` must be provided.

## Environment Variables

The following environment variable can be set instead of using the above flags.
See the [flags](#flags) for how each variable is used.

* `MASKING_USER` replaces `-u`
* `MASKING_PASSWORD` replaces `-P`
* `MASKING_HOST` replaces `-H`
* `MASKING_PORT` replaces `-p`

# Backup

In order to backup and engine, the user must provide 5 arguments to this tool.

1. The file in which to store the backup. `-f`
2. The hostname of the masking engine to back up (e.g. myMaskingEngine). `-H`
3. The port the masking engine is located at. `-p`
4. Username of an admin account for that masking engine. `-u`
5. Password for that admin account. `-P`

For example, if I wanted to store the backup file at `/Users/me/backup.yaml` for
a masking engine located at `https://companyXMasking.com:8282/dmsuite` with admin
user `admin_user` and password `admin_password` the following command would achieve this.

```
./dlpx-masking-initializer -f /Users/me/backup.yaml -H companyXMasking.com -p 8282 -u admin_user -P admin_password -b
```

After this operation completes, the file `/Users/me/backup.yaml` should contain all masking state
from the host engine.

###### Overwrite flag

If the file where the backup is being written to already exists, then a `-o` flag must
be provided in order to overwrite that file, otherwise the backup will not occur.

# Restoring

In order to restore the masking engine from a backup file, all that is needed is the backup
file. For example, if my backup file is at `/Users/me/backup.yaml` then running the
following command will upload all objects in the backup file to the masking engine.

```
./dlpx-masking-initializer -f /Users/me/backup.yaml -s
```

**IMPORTANT** Please see [Password Limitation](#password-limitations) before attempting to
restore from a backup file.

###### Specifying a different masking engine

By default, the above command will attempt to restore the masking engine to which it was backed
up from. A different engine can be targeted for restoration by supplying the host, port, username, and password
flags in the command line.

```
./dlpx-masking-initializer -f /Users/me/backup.yaml -H aMaskingEngine.co -p 8282 -u another_admin_user -P another_admin_password -s
```

# Suggested Usage For Engineers

This can be a very powerful tool for developers and QA. If an engineer has a database/sftp server
that they repeatedly use then this tool can be used to quickly configure a masking job
to set up the necessary objects to run that job. The issue is that creating the `yaml`
file can be confusing and be easily messed up. For this reason it is suggested that
an engineer first set up a masking job manually, back up that masking engine and then
use the backup file in the future to restore the engine to that state. This is preferable
to manually writing the `yaml` file.

An example workflow is explained below.

1. Create a masking job pointing to a commonly used database or SFTP server.
2. Run this tool in backup mode, as spelled out in the [Backup](#backup) section.
3. Manually open up the backup file and remove/edit any backup state.
Working with an existing `yaml` file is pretty simple as the structure is already laid
and should make sense to anyone familiar with the masking app. **IMPORTANT**: Even if you
wish to use the exact some configuration you *must* still add in the passwords to the
database and sftp connectors as this information is not provided by the API. Please see [Password Limitation](#password-limitations).
4. Run this tool in restore mode, as spelled out in the [Restoring](#restoring) section, anytime you want
to restore a masking application to the configuration specified in the `yaml` file.

# Password Limitations

One limitation of the API is that it does not return database, user, or SFTP server passwords.
This is done for security purposes. This means the backup file does not contain the
database, user, or SFTP server passwords. In order to completely restore from a backup file, you
must do the following.

Manually open up the backup file and fill in all password fields. They are tagged with
`DATABASE_PASSWORD`, `USER_PASSWORD` and `SFTP_PASSWORD` so that they can be easily identified.

# File Masking

Unfortunately the masking engine does not provide a way to retrieve file formats that have been uploaded,
so this tool has no way to retrieve them at backup. However, the masking engine does provide a way to
back up file conenctors/rulesets/metadata. If a file masking job is backed up, then if the user simply provides
the required file format at step up time, the job can be configured properly. For example:

1. User sets up a file masking job using the file format named `my_file_format.txt`
2. User runs this back up script on the masking engine.
3. User wants to restore that masking job to another engine. They can achieve this as long as
 `my_file_format.txt`  is next to the backup file, or in the backup folder.

# JDBC Drivers

Unfortunately the masking engine does not provide a way to retrieve JDBC driver files that have been uploaded,
so this tool has no way to retrieve them at backup. However, masking engine does backup of JDBC Drivers MDS information. If a JDBC Driver is backed up, then if the user simply provides
the required driver zip file at step up time, the JDBC Driver can be configured properly. For example:

1. User sets up a JDBC Driver using driver zip file named `JDBC_DRIVER_<DRIVER_NAME>.zip`.
It is important that driver zip file name should be use `JDBC_DRIVER_<DRIVER_NAME>`, eg. If `driverName` is `HANA`, then driver zip file should be named as `JDBC_DRIVER_HANA.zip`.
2. User runs this back up script on the masking engine.
3. User wants to restore that masking job to another engine. They can achieve this as long as
 `JDBC_DRIVER_<DRIVER_NAME>.zip` is next to the backup file, or in the backup folder.

# Contribute

1.  Fork the project.
2.  Make your bug fix or new feature.
3.  Add tests for your code.
4.  Send a pull request.

Contributions must be signed as `User Name <user@email.com>`. Make sure to [set up Git with user name and email address](https://git-scm.com/book/en/v2/Getting-Started-First-Time-Git-Setup). Bug fixes should branch from the current stable branch. New features should be based on the `master` branch.

### Code of Conduct

This project operates under the [Delphix Code of Conduct](https://delphix.github.io/code-of-conduct.html). By participating in this project you agree to abide by its terms.

### Contributor Agreement

All contributors are required to sign the Delphix Contributor agreement prior to contributing code to an open source repository. This process is handled automatically by [cla-assistant](https://cla-assistant.io/). Simply open a pull request and a bot will automatically check to see if you have signed the latest agreement. If not, you will be prompted to do so as part of the pull request process.

# Reporting Issues

Issues should be reported in the GitHub repo's issue tab. Include a link to it.

# Statement of Support

This software is provided as-is, without warranty of any kind or commercial support through Delphix. See the associated license for additional details. Questions, issues, feature requests, and contributions should be directed to the community as outlined in the [Delphix Community Guidelines](https://delphix.github.io/community-guidelines.html).

# License

This is code is licensed under the Apache License 2.0. Full license is available [here](./LICENSE).
