# ftphelper

A Java library that provides a single unified static API for FTP and SFTP operations. Write one line of code — it works the same regardless of protocol.

## Dependencies

| Library | Version |
|---|---|
| Java | 11 |
| [JSch](http://www.jcraft.com/jsch/) (SFTP) | 0.1.55 |
| [Apache Commons Net](https://commons.apache.org/proper/commons-net/) (FTP) | 3.8.0 |
| Maven | 3.x |

## Build

```bash
mvn clean package
```

The jar is output to `target/ftphelper-1.0.0.jar`.

## Quick start

```java
// Create connection details — same object for both protocols
ConnectionDetails ftpConn  = new ConnectionDetails("ftp.example.com",  "user", "pass", Protocol.FTP);
ConnectionDetails sftpConn = new ConnectionDetails("sftp.example.com", "user", "pass", Protocol.SFTP);

// Default ports are used automatically: FTP=21, SFTP=22
// Specify an explicit port with the 4-argument constructor:
ConnectionDetails conn = new ConnectionDetails("sftp.example.com", 2222, "user", "pass", Protocol.SFTP);
```

## API reference

All methods are static on `FtpHelper` and throw `IOException` on failure. Each call opens and closes its own connection.

### writeFile

Upload data to the server.

```java
FtpHelper.writeFile(conn, "/remote/dir/report.csv", inputStream);
```

### readFile

Download a file. The content is fully buffered in memory before the connection closes, so the returned `InputStream` is safe to use after the call returns.

```java
InputStream data = FtpHelper.readFile(conn, "/remote/dir/report.csv");
```

### deleteFile

```java
FtpHelper.deleteFile(conn, "/remote/dir/old-report.csv");
```

### deleteFolder

```java
// Delete an empty folder
FtpHelper.deleteFolder(conn, "/remote/archive", false);

// Delete a folder and all its contents recursively
FtpHelper.deleteFolder(conn, "/remote/archive", true);
```

### listFiles

Returns `List<FileInfo>` for all files in a directory, or those within a date range.

```java
List<FileInfo> all = FtpHelper.listFiles(conn, "/remote/reports");

List<FileInfo> recent = FtpHelper.listFiles(conn, "/remote/reports",
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 12, 31));
```

`FileInfo` fields: `getName()`, `getPath()`, `getSize()` (bytes), `getLastModified()` (`LocalDateTime`, UTC).

### listFolders

Returns `List<FolderInfo>` for all sub-folders in a directory, or those within a date range.

```java
List<FolderInfo> all = FtpHelper.listFolders(conn, "/remote");

List<FolderInfo> recent = FtpHelper.listFolders(conn, "/remote",
        LocalDate.of(2024, 6, 1),
        LocalDate.now());
```

`FolderInfo` fields: `getName()`, `getPath()`, `getLastModified()` (`LocalDateTime`, UTC).

### getFileInfo

```java
FileInfo info = FtpHelper.getFileInfo(conn, "/remote/reports/q1.csv");
System.out.println(info.getSize());         // bytes
System.out.println(info.getLastModified()); // LocalDateTime UTC
```

### getFolderInfo

```java
FolderInfo info = FtpHelper.getFolderInfo(conn, "/remote/reports");
System.out.println(info.getLastModified());
```

### watchFolder

Polls a remote folder at a fixed interval and fires a callback for every addition, deletion, or modification. Returns a `FolderWatcher` that must be closed to stop background polling.

```java
try (FolderWatcher watcher = FtpHelper.watchFolder(
        conn,
        "/remote/inbox",
        10, TimeUnit.SECONDS,
        event -> System.out.println(event.getType() + " " + event.getFile().getName())
)) {
    Thread.sleep(300_000); // watch for 5 minutes
}
```

`FolderChangeEvent` fields: `getType()` (`ADDED`, `DELETED`, or `MODIFIED`), `getFile()` (`FileInfo` — for `DELETED` events this reflects the last known state).

The first poll silently establishes a baseline; events begin firing from the second poll onward. Transient connection errors are swallowed and retried on the next tick.

## Project structure

```
ftphelper/
├── pom.xml
└── src/main/java/com/ftphelper/
    ├── FtpHelper.java          # Public static API
    ├── ConnectionDetails.java  # Connection parameters
    ├── Protocol.java           # Enum: FTP | SFTP
    ├── FileInfo.java           # File metadata
    ├── FolderInfo.java         # Folder metadata
    ├── FolderChangeType.java   # Enum: ADDED | DELETED | MODIFIED
    ├── FolderChangeEvent.java  # Change event payload
    ├── FolderWatcher.java      # Background polling watcher
    ├── FtpHelperException.java # Unchecked wrapper exception
    ├── RemoteOperations.java   # Internal interface (package-private)
    ├── FtpOperations.java      # FTP implementation (package-private)
    └── SftpOperations.java     # SFTP implementation (package-private)
```
