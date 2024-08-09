# Publication

This document will explain how to produce the final DITA outputs required to build the NHS Data Dictionary website and related change papers.

## Generate DITA Outputs

Publication starts by signing in to the Orchestrator, found at:

- https://mauro.dataproducts.nhs.uk/orchestration/ - LIVE
- https://mauro.dev.dataproducts.nhs.uk/orchestration/ - TEST
- https://mauro.uat.dataproducts.nhs.uk/orchestration/ - TRAINING

Then click on the "Branches" navigation header link, select a branch to publish, then review the publish options available in the "Publish" tab:

![](/docs/images/publish-options.png)

There are three options:

1. Generate a Change Paper with basic definitions.
2. Generate a Change Paper, including full Data Set definitions.
3. Generate a full NHS Data Dictionary website.

Click any of the "Generate" buttons to start DITA generation. This will take some time depending on the size of the Data Dictionary branch.

The output of each action is always a zip file containing all the DITA content files required to produce the final output. This is a standard file download from the browser, you will be asked where to save the zip and what it is called.

**Note:** the website DITA zip file contains a lot of content. We have noticed that on Windows machines, the zip file cannot be opened using the built-in Windows Explorer zip functionality. However, the zip file can be opened in other utilities, such as [7-Zip](https://www.7-zip.org/).

## Transforming DITA Outputs

To transform the DITA files into their final outputs, the following steps will be carried out in [OxygenXML Editor](https://www.oxygenxml.com/).

You will also need to clone the NHS Digital repo for the DITA stylesheets and templates: https://github.com/NHSDigital/DITAStyleSheets.

Extract all the files from the zip file to a local folder. Under the `dita` folder, there will be a `nhs_data_dictionary.ditamap` file (for both website and change papers) - this is the map file referencing all other `.dita` files and topics/links. Open this file in OxygenXML Editor (selecting "Open in DITA Maps Manager" when prompted).

You will then be able to view all the files for the output:

![](/docs/images/oxygenxml-editor.png)

To configure and start a transformation to the final format, click on the "Configure Transformation Scenarios" option:

![](/docs/images/oxygenxml-transform-start.png)

Then follow these steps depending on the target output.

### Website

Double click "DITA Map WebHelp Responsive". When asked if you want to duplicate and edit a copy of this scenario, say "Yes".

![](/docs/images/oxygenxml-transform-web.png)

Edit the scenario by: 

- Giving a memorable name e.g. "DITA Map Web - NHS Data Dictionary (Preview)" 
- Under the "Templates" tab, click "Choose custom publishing template". Select one of the `.opt` files file from your local DITAStyleSheets repo e.g. "NHS Data Dictionary (Preview).opt"

![](/docs/images/oxygenxml-transform-web-template.png)

- Click "OK" to save 

Make sure your scenario is ticked, then click "Apply associated". 

![](/docs/images/oxygenxml-transform-web-select.png)

**Note:** transforming the website DITA into HTML will take a long time (around 30-45 minutes) due to the amount of content to work on.

The `index.html` file of the final static website will automatically open in your browser when finished. 

### Change Paper

Double click "DITA Map PDF - based on HTML5 & CSS". When asked if you want to duplicate and edit a copy of this scenario, say "Yes".

![](/docs/images/oxygenxml-transform-pdf.png)

Edit the scenario by: 

- Giving a memorable name e.g. "DITA Map PDF - NHS Data Dictionary - Change Paper PDF with styling" 
- Under the "Templates" tab, click "Choose custom publishing template". Select the "NHS Data Dictionary (CR pdf).opt" file from your local DITAStyleSheets repo 

![](/docs/images/oxygenxml-transform-pdf-template.png)

- Under the "Parameters" tab, select the CSS file to use. Choose `NHSD-print.less` from your local DITAStyleSheets repo 

![](/docs/images/oxygenxml-transform-pdf-params.png)

- Click "OK" to save 

Make sure your scenario is ticked, then click "Apply associated". 

![](/docs/images/oxygenxml-transform-pdf-select.png)

The output file will automatically be saved local to the dita folder under the `out` folder and will automatically open in your PDF viewer app. 