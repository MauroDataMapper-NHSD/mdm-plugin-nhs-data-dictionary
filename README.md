# mdm-plugin-nhs-data-dictionary

Repository holding the Mauro plugin to manage operations on the NHS Data Dictionary when contained and modelled in Mauro. This handles:

- Ingest of the original branch content from the obsolete Borland Together authoring system.
- Generation of DITA outputs for the creation of the Data Dictionary website and Change Papers.
- Generation of HTML previews for displaying what the final content will look like in the website and Change Papers.
- Standard operations related to publication, such as running statistics and integrity checks

| Branch  | Build Status |
| ------- | ------- | 
| main    | [![Build Status](https://github.com/MauroDataMapper-NHSD/mdm-plugin-nhs-data-dictionary/actions/workflows/gradle.yml/badge.svg?branch=develop)](https://github.com/MauroDataMapper-NHSD/mdm-plugin-nhs-data-dictionary/actions/workflows/gradle.yml) |
| develop | [![Build Status](https://github.com/MauroDataMapper-NHSD/mdm-plugin-nhs-data-dictionary/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/MauroDataMapper-NHSD/mdm-plugin-nhs-data-dictionary/actions/workflows/gradle.yml) |

This plugin is one part of the whole Mauro solution to support the NHS Data Dictionary authoring and publication. The full solution comprises of:

- [mdm-core](https://github.com/MauroDataMapper/mdm-core) - the core Mauro backend API
- [mdm-plugin-nhs-data-dictionary](https://github.com/MauroDataMapper-NHSD/mdm-plugin-nhs-data-dictionary) - this Mauro plugin repository
- [mdm-plugin-authentication-openid-connect](https://github.com/MauroDataMapper-Plugins/mdm-plugin-authentication-openid-connect) - a plugin to support authentication via 3rd party systems, such as Microsoft
- [mdm-application-build](https://github.com/MauroDataMapper/mdm-application-build) - hosting solution to run Mauro core with all plugins under a web server
- [mdm-ui](https://github.com/MauroDataMapper/mdm-ui) - the standard user interface for managing the Mauro catalogue - in this case, the Data Dictionary content and branches.
- [nhsd-datadictionary-orchestration](https://github.com/MauroDataMapper-NHSD/nhsd-datadictionary-orchestration) - a custom user interface for NHS England to manage preview and publication operations that this plugin provides.
- [nhsd-datadictionary-docker](https://github.com/MauroDataMapper-NHSD/nhsd-datadictionary-docker) - the hosting and deployment setup to run the NHS Data Dictionary solution via Docker containers.

# Documentation

Apart from this README, which provides the basic setup required for development, further documentation can be found under [documentation](/docs) folder:

- [HTTP Endpoints](/docs/endpoints.md)
- [Previews](/docs/previews.md)
- [Publication](/docs/publication.md)

## Requirements

* Java 17 (Temurin)
* Grails 5.1.2+
* Gradle 7.3.3+

All of the above can be installed and easily maintained by using [SDKMAN!](https://sdkman.io/install).

Knowledge of Grails and how Mauro plugins are developed will be required to develop these tools. These may be useful for reference:

- [Grails documentation](https://docs.grails.org/5.1.2/guide/single.html)
- [Mauro documentation](https://maurodatamapper.github.io/)

# Setup for development

The following instructions explain how to setup your development environment to work with this plugin.

## Configure mdm-application-build

The `mdm-plugin-nhs-data-dictionary` Grails plugin needs to be loaded into a full Mauro backend instance. Use [mdm-application-build](https://github.com/MauroDataMapper/mdm-application-build) for this as only changes to `mdm-plugin-nhs-data-dictionary` are required - the core Mauro libraries should not need changing.

Clone the `mdm-application-build` repo, then modify the `build.gradle` file to include the `mdm-plugin-nhs-data-dictionary` plugin:

```groovy
// Update the grails > plugins list of dependencies...
grails {
    plugins {
        // Other plugin references here...

        runtimeOnly 'uk.ac.ox.softeng.maurodatamapper.plugins:mdm-plugin-nhs-data-dictionary:2.0.0-SNAPSHOT'
    }
}
```

## Publish to Maven local

`mdm-application-build` is configured to fetch library dependencies from a package dependency source using Maven. Usually published dependencies will
come from Artefactory, but for development it will also inspect your local Maven folder - found in `~/.m2/repository`.

In this repo, run `gradle :mdm-plugin-nhs-data-dictionary:publishToMavenLocal` to publish latest changes to your local Maven folder.

## Run mdm-application-build

In the `mdm-application-build` repo, you can now run `gradle bootRun` to run a full Mauro instance on your machine and host
from http://localhost:8080. If the `build.gradle` file was configured correctly, the log file should say that the `mdm-plugin-nhs-data-dictionary` plugin
was loaded successfully into the Grails application.

## Run user interfaces

You will want to inspect the results in Mauro using the user interfaces, which will be easier than manual API requests.

Clone the [mdm-ui](https://github.com/MauroDataMapper/mdm-ui) repo, then run:

```bash
npm install
npm start
```

This will host the Mauro frontend locally on http://localhost:4200.

Finally, clone the [nhsd-data-dictionary-orchestration](https://github.com/MauroDataMapper-NHSD/nhsd-datadictionary-orchestration) repo, then run:

```bash
npm install
npm start
```

This will host the Mauro frontend locally on http://localhost:4201.
