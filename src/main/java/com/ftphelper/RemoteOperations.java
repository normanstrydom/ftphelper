package com.ftphelper;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Internal abstraction over FTP and SFTP clients.
 * Package-private — use FtpHelper for the public API.
 */
interface RemoteOperations extends Closeable {

    void writeFile(String remotePath, InputStream data) throws IOException;

    InputStream readFile(String remotePath) throws IOException;

    void deleteFile(String remotePath) throws IOException;

    void renameFile(String remotePath, String newPath) throws IOException;

    void moveFile(String remotePath, String destinationPath) throws IOException;

    void deleteFolder(String remotePath, boolean includeContents) throws IOException;

    void renameFolder(String remotePath, String newPath) throws IOException;

    void moveFolder(String remotePath, String destinationPath) throws IOException;

    List<FileInfo> listFiles(String remotePath) throws IOException;

    List<FolderInfo> listFolders(String remotePath) throws IOException;

    FileInfo getFileInfo(String remotePath) throws IOException;

    FolderInfo getFolderInfo(String remotePath) throws IOException;
}
