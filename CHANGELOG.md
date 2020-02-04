# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

ISSUE #3 Add -v flag to allow for tracking of versions
ISSUE #15 base file formatted differently from sub files on scale
ISSUE #17 Allow tables to be grouped into one file
ISSUE #20 Remove all Delphix related code names from the codebase

## [2.3.1]
Fix for issue #51 - Compare engine version functionality is not working if the version contains additional string information.

## [2.3.0]

### Fixed
Fix for issue #49 - Make changes in JDBC Drivers POST/PUT API call to use file upload endpoint to restore JDBC driver

## [2.2.0]

### Added
Fix for issue #47 - Add JDBC Drivers object to backup/restore

## [2.1.0]
Add support for syncing mounts

## [2.0.0]
Sync with masking schema for masking version 6.0

### Fixed
Fix for issue #39 - Masking initializer is no longer in sync with masking engine schema

## [1.1.0] - 2018-12-14

### Added
Fix for issue #32 - Add SSL support

## [1.0.1] - 2018-10-19

### Fixed
Fix for issue #29 - Masking job triggers and constraint check boxes are not getting backed up

## [1.0.0] - 2018-10-18

### Fixed
Fix for issue #24 - Running in backup mode for 5.3 engine with -e option causes exception

### Added
Fix for issue #14 - Add user object to backup/restore.
Fix for issue #27 - Add ability to back executions

## [0.7.0] - 2018-08-17

### Fixed
Fix for issue #17 - Allow auth tokens on the command line
Fix for issue #20 - Add UTF-8 character support for PUT requests
Fix for issue #21 - Add better logging around which file is being read

## [0.6.0] - 2018-06-19

### Added
Added #8 - [FEATURE] Add ability to specify flags through environment variables

### Fixed
Fix for issue #2 - Profiler Set Purpose (description) missing on restore
Fix for issue #3 - Algorithms being overwritten on restore when using -s | DEBUG option
Fix for issue #4 - Host and instance name conflict for advanced jdbc connectors
Fix for issue #5 - Error loading specific connector from missing profile set ID#
Fix for issue #10 - UTF-8 characters are not properly encoded on setup

## [0.5.0]

### Added
ISSUE #4 Add flag to continue on if error occurs

### Fixed
ISSUE #11 On the fly does not work if the wrong environment is created first
ISSUE #15 base file formatted differently from sub files on scale
ISSUE #20 Remove all Delphix related code names from the codebase
ISSUE #23 File formats can not be backup/restored properly
ISSUE #24 Masking Application date format bug causes certain restorations to fail
ISSUE #25 Convert to json files only
ISSUE #26 Work around for DLPX-56628

## [0.4.0]

### Added
ISSUE #22 Version each backup file so that we can deprecate features in the future

### Fixed
ISSUE #19 profile expressions that are not a part of profile sets are never backed up
ISSUE #21 In backup the host is being set as the username instead of as the host

## [0.3.2]

### Fixed
ISSUE #18 Import global objects first
ISSUE #16 double .json for file names

## [0.3.1]

### Fixed
ISSUE #13 Catch/log exception at highest level

## [0.3.0]

### Added
ISSUE #12 Add better logging

### Fixed
ISSUE #10 Error in command line validation does not stop backup from running

## [0.2.0] - 2017-11-23

### Fixed
ISSUE #6 Column metadata is out of sync

ISSUE #7 Engines with more then 100 columns in the inventory do not have all of their columns backed up

### Added
ISSUE #8 Flag to only backup column metadata where `isMasked` is true

ISSUE #9 Add ability to scale engine sync backups

## [0.1.1] - 2017-11-14

### Fixed
ISSUE #5 PUT call not getting latest id

## [0.1.0] - 2017-11-12

### Added
ISSUE #1 Overwrite existing values

ISSUE #2 Separate file infrastructure and separate out environments/rulesets/tablemetadata

## [0.0.5] - 2017-11-12

This is the initial version of the change log. From here on all changed will be
logged here.

