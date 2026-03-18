# ftphelper

##  Design

### Requirement

A simple java/mavan project to create utility to simplify ftp and sftp operations into single static methods wrapping underlying implementations with same methods being used whether sftp or ftp is used.

All methods have simple connection detail object as parameter which defines,

*   host
*   port
*   username
*   password
*   ftp/sftp

### Location and required libraries

*   Java 11 - home=C:\Program Files\Java\jdk-11.0.2
*   ssh - jsch version=2.27.9
*   ftp - apache commons net version=3.8.0
*   Maven - home=F:\apache-maven-3.9.3

### Implementation

*   writeFile
*   readFile
*   deleteFile
*   deleteFolder (option to include contents)
*   listFiles (all)
*   listFiles (in date range)
*   listFolders (all)
*   listFolders (in date range)
*   getFileInfo
*   getFolderInfo




