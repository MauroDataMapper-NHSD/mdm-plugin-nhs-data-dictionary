# HTTP Endpoints

This document explains the HTTP endpoints that this Mauro plugin exposes to the Mauro system.

Unless otherwise stated, all HTTP responses should return a `200 OK` status code.

All endpoints listed here presume that there is a base domain preceding them e.g. if running under the NHS England test environment https://mauro.dev.dataproducts.nhs.uk/, then the branches endpoint would be `GET https://mauro.dev.dataproducts.nhs.uk/api/nhsdd/branches`.

Although not required, this document assumes that HTTP requests are sent using [Postman](https://www.postman.com/), but any similar tool could also be used.

## Authentication

To perform any of the HTTP requests below, you will need to authenticate in Mauro beforehand. To send authentication requests, please review the [Authentication](https://documenter.getpostman.com/view/9840589/UVC8BkkA#authentication) documentation. Then send a [POST Login](https://documenter.getpostman.com/view/9840589/UVC8BkkA#8cedd7d2-e565-4137-9882-f5a5ccef040f) request to receive an authentication cookie.

## Ingest

```http
POST /api/nhsdd/ingest
```

Use this endpoint to ingest an XML file extracted from Borland Together to recreate a Data Dictionary branch in it's entirety. This should be used during migration from Borland Together to Mauro.

**Note:** The XML files will be very large, so ensure that the web server can accept requests sizes in the region of 200MB.

The body of the request should be set as `form-data`. There are two request bodies to send depending on what you are ingesting:

### Ingest a released branch

Send `form-data` with these names/values:

```
ingestFile         – XML file to upload
finalise           – true
releaseDate        – e.g. "May 2024"
folderVersionNo    – e.g. "2024.5.0"
prevVersion        – A UUID to the previous version e.g. if ingesting
                    - "May 2024", use the UUID pointing to the previously
                    - ingested "March 2024" versioned folder. If this is
                    - the first one being injected, ignore this parameter
async              - true
```

The `releaseDate` and `folderVersionNo` are required. The `releaseDate` maps to the version tag of the branch, and the `folderVersionNo` is the formal version number of the branch. Convention so far has been to name them as:

- `folderVersionNo` - `<year>.<month>.0` e.g. August 2024 would be `2024.8.0`
- `releaseDate` - the month and year of the release e.g. "August 2024"

By setting `async=true`, the response back should be `202 Accepted`. The ingest will then happen in the background, taking around 2 - 5 minutes.

### Ingest an in-flight branch

Send `form-data` with these names/values:

```
ingestFile  – XML file to upload
branchName  – The name to give the branch. They usually name them after
                their change request numbers e.g. "CR1915 - [Name]"
prevVersion – A UUID to the previous finalised versioned folder that is
                the base for this branch
async       – true
```

By setting `async=true`, the response back should be `202 Accepted`. The ingest will then happen in the background, taking around 2 - 5 minutes.

### Asynchronous Jobs

If the `async=true` parameter was set in the request body, the HTTP response will always be `202 Accepted` to return an immediate response. The work to ingest the file is then carried out in the background on the Mauro server (since the ingest can take several minutes). To view the progress of the async job, the HTTP response will also return a job ID which can be used to request the status of this job, using these [Mauro Async Jobs APIs](https://documenter.getpostman.com/view/9840589/UVC8BkkA#351305e8-341d-4ca7-a401-02e50fbdbfc7).

## Branches

```http
GET /api/nhsdd/branches
```

Returns a list of all available Data Dictionary branches to use. Each branch returned will provide an `id` value, which is then used in the endpoints listed below (where it says `{branchId}`)

## Statistics

```http
GET /api/nhsdd/branches/{branchId}/statistics
```

Returns the set of statistics calculated for a Data Dictionary branch, such as how many items are retired or preparatory.

## Integrity Checks

```http
GET /api/nhsdd/branches/{branchId}/integrityChecks
```

Returns a list of categories for integrity checks (validity checks) across a Data Dictionary branch. Under each category will be a list of Data Dictionary items to highlight which items have issues under that category.

## Publish

```http
GET /api/nhsdd/branches/{branchId}/publish/website
```

Generates a zip file containing the compiled DITA files for a Data Dictionary branch. This will be used to transform into the live published version of the NHS Data Dictionary website.

```http
GET /api/nhsdd/branches/{branchId}/publish/changePaper

GET /api/nhsdd/branches/{branchId}/publish/changePaper?dataSets=true
```

Generates a zip file containing the compiled DITA files for a Data Dictionary branch change paper. This will be used to transform into a PDF document representing the branded NHS England change paper describing the changes made in a branch.

The `dataSets` query parameter is optional. If provided, the full data set definitions and tables will be included in the change paper DITA.

## Preview

```http
GET /api/nhsdd/branches/{branchId}/preview/changePaper

GET /api/nhsdd/branches/{branchId}/preview/changePaper?includeDataSets=true
```

Generates the approximate HTML for a change paper of a branch to act as a preview before DITA generation. The optional `includeDataSets` query parameter will include full data set definitions as well.

```http
GET /api/nhsdd/branches/{branchId}/preview/{pageType}
```

Generates the approximate HTML for a website page preview on a branch to act as a preview before DITA generation. The `{pageType}` fragment should be one of the following:

- `allItemsIndex`
- `classes`
- `attributes`
- `elements`
- `dataSets`
- `dataSetFolders`
- `dataSetConstraints`
- `businessDefinitions`
- `supportingInformation`

```http
GET /api/nhsdd/branches/{branchId}/preview/{pageType}/whereUsed
```

Part of the HTML preview, this endpoint will calculate a list of links to identify where a data dictionary item is used elsewhere in the dictionary. This returns the HTML content for an item page as a table.