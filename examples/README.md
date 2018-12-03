# Overview

This folder contains example masking application configurations that can be used to repeatedly
initialize the same masking engine to the same state, or multiple masking engines to the same state.

While the below examples are starters, the great thing about this tool is that it has a 1:1
mapping between the fields that can be specified and the api v5 masking endpoints. Given this,
any fields that you want to use that are not mentioned in the examples below can still be used. Just
look at the json files that contain the swagger schema to figure out model setup and naming conventions.

# Examples
* [Template](template.yaml)
* [Oracle database](oracle.yaml)
* [Only application and env](onlyAppEnv.yaml)
* [MSSQL](mssql.yaml)
* [Delimited file](delmFile.yaml)

### Still to come in examples
 * Global objects (profile sets/profile expressions/domains/syncable-objects...)

### Note
 Yaml files can be really picky about spacing/indentation, make sure not to mess that up.
