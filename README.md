# BibX
### A tool for bibliometric data exploration

### Introduction

BibX is a Java-based tool that stores article records retrieved from Thomson Reuters [Web of Science] (http://webofknowledge.com) to a relational database, and based on those records generates a pdf-report with a multitude of bibliometric indicators. BibX utilizes R to draw several charts and network graphs for visual data representation, and aids in discovering discrepancies in the Web of Science data.

In addition, as very few keyword searches used to retrieve the article records are either exclusive or exhaustive, methods are provided to help in iterative article selection (and data correction) by enabling moving the data in and out of the database between analyses.

BibX may thus be helpful for anyone doing bibliometric analyses by easing the data manipulation process and by performing many automated analyses based on the data.


### Requirements

Below is a list of technologies required to run BibX. Some of them are optional to enable the full range of functionality. Version numbers used in development are listed for reference, but earlier versions may also work.

_Program_
- Java (1.8.72)

_Database_
- MySQL Community Server (5.7.10, may work on other SQL databases also)
- MySQL Connector/J (5.1.38)

_Data_out_
- Optional for doc output: Apache POI (required files from 3.14 included in `lib`)
- Optional for pdf output: TeX to PDF typesetter (pdflatex from MiKTeX 2.9)

_Report_
- TeX to PDF typesetter (pdflatex from MiKTeX 2.9)
- R for charts and network graphs (3.2.3)

Development done on Windows 10, probably will work on other platforms also.


### Example

Please take a look at the `example`-folder for what to expect. In the root of the folder there is a data file with 327 full article records retrieved from [Web of Science] (http://webofknowledge.com) with a simple title search on "absorptive capacity". This data file has been imported to a database, and then exported to `bibx_data_out`-folder for closer inspection. An analysis report on this initial data has also been generated to `bibx_report`-folder: see the `bibx_report.pdf`.


### Instructions

##### Short version

Add "&lt;your_path&gt;/BibX/lib/poi-3.14/*" to [classpath] (https://en.wikipedia.org/wiki/Classpath_%28Java%29) environment variable.

Compile the source code by executing:

    javac *.java
    
(Ready-compiled classes are also included in the `classes`-folder.)

Set up your database by running the `init.sql`-script found in the `sql`-folder.

Ensure you have the required programs (listed above) set up.

Fetch some data from [Web of Science] (http://webofknowledge.com) by exporting search results as full record text.

Then run BibX:

    java bibx_data_in <path/to/data_file.txt>
    java bibx_data_out
    java bibx_report

##### Longer notes

_Classpath_

Compiling `bibx_data_out.java` requires inclusion of the Apache POI jar files from the `lib`-folder to either the command line classpath or the environment variable. These jars are also required when executing `bibx_data_out` with output_file_type configured to "doc" or "all" in `bibx_data_out_properties.txt` found in the `properties`-folder.

_Database_

Set up a database on your MySQL-server by running the `init.sql`-script from the `sql`-folder. It executes three different scripts that creates the database, a user and the tablespace. If you have already created a database and/or a user, you will want to execute these scripts individually.
Edit the scripts if you wish to rename your database or configure the user in more detail. Remember to then go to the `properties`-folder and adjust the properties-files to match your edited settings. The default setup should not need any configuration.

_Path_

It is convenient to add the following program folders to the path environment variable (although they can be configured with absolute paths in the properties):
- Pdflatex: e.g. &lt;your_path&gt;/MiKTeX 2.9/miktex/bin/
- Rscript: e.g. &lt;your_path&gt;/R/R-3.2.3/bin/
- (Of course also Java: e.g. &lt;your_path&gt;/Java/jdk1.8.0_72/bin/)

_Data_in_

BibX inputs article records from [Web of Science] (http://webofknowledge.com). To retrieve data for your analyses:
- Go to http://webofknowledge.com
- Do a search (configuring all your preferred search parameters)
- Sort the records from oldest to newest (more convenient if you want to add more records to your data later)
- Then choose "Save to Other File Formats" for a pop-up:
  - Number of Records: Records 1 to your max
  - (Note that if you have over 500 articles, you need to download them 500 at a time to separate files and combine them manually.)
  - Record Content: Full Record and Cited References
  - File Format: Plain Text
- This will be your data file
- Import it to the database using `java bibx_data_in <path/to/data_file.txt>`

_Data_out_

Outputs database records for convenient reviewing. You can configure the output formats in `bibx_data_out_properties.txt`. The pdf-format supports highlighting patterns (such as the keywords used for retrieving the data) that can be configured in the same file.
Based on your review of the output you may (most probably) want to remove some unnecessary articles from your data set. Clear the database using `clear_tables.sql` from the `sql`-folder, manually remove the unnecessary records from your data file and import the remaining data again using `java bibx_data_in <path/to/data_file.txt>`.

_Report_

Generates the main analysis report based on the article data in the database. Several configuration variables found in `bibx_report_properties.txt` control the output.
If you wish to reduce the length of the generated report and concentrate solely on the bibliometric indicators, you may wish the turn the appendix off. On the other hand, if you first want to check your data for integrity, you may wish to generate only the appendix. Finding any inconsistencies in the data (such as often found in author names) you can, as noted previously, clear the database, manually edit your data file and then import the corrected data again.
The network_edgesize_min -variables affect the visibility of ties and nodes in the network graphs. For large data sets raise these numbers to make the graphs less cluttered. For smaller data sets configure the numbers close to 1 (or 1) to make most (or all) ties visible.

### Finally

Performing bibliometric analyses and utilizing Web of Science data is generally tricky and most likely an iterative process. Hopefully BibX can aid you in this work and help you find interesting and meaningful research results!

##### Citation

You can use the following citation and DOI to refer to BibX:

Laine, M.O.J. (2016). BibX: A tool for bibliometric data exploration. DOI: 10.5281/zenodo.51389. Available at: http://github.com/MkkLn/BibX.

[![DOI](https://zenodo.org/badge/22028/MkkLn/BibX.svg)](https://zenodo.org/badge/latestdoi/22028/MkkLn/BibX)
