package com.ftphelper;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * FTP implementation using Apache Commons Net. Package-private.
 */
class FtpOperations implements RemoteOperations {

    private static final Logger log = Logger.getLogger(FtpOperations.class.getName());

    private final FTPClient client;
    private final String host;

    FtpOperations(ConnectionDetails conn) throws IOException {
        this.host = conn.getHost() + ":" + conn.getPort();
        log.fine(() -> "FTP connecting to " + host);
        client = new FTPClient();
        client.connect(conn.getHost(), conn.getPort());
        int reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            throw new IOException("FTP server refused connection to " + conn.getHost());
        }
        if (!client.login(conn.getUsername(), conn.getPassword())) {
            client.disconnect();
            throw new IOException("FTP login failed for user: " + conn.getUsername());
        }
        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);
        log.fine(() -> "FTP connected and logged in as " + conn.getUsername() + " @ " + host);
    }

    @Override
    public void writeFile(String remotePath, InputStream data) throws IOException {
        log.fine(() -> "writeFile " + remotePath);
        if (!client.storeFile(remotePath, data)) {
            throw new IOException("Failed to write file: " + remotePath + " — " + client.getReplyString().trim());
        }
        log.fine(() -> "writeFile complete: " + remotePath);
    }

    @Override
    public InputStream readFile(String remotePath) throws IOException {
        log.fine(() -> "readFile " + remotePath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!client.retrieveFile(remotePath, baos)) {
            throw new IOException("Failed to read file: " + remotePath + " — " + client.getReplyString().trim());
        }
        int size = baos.size();
        log.finer(() -> "readFile complete: " + remotePath + " (" + size + " bytes)");
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public void deleteFile(String remotePath) throws IOException {
        log.fine(() -> "deleteFile " + remotePath);
        if (!client.deleteFile(remotePath)) {
            throw new IOException("Failed to delete file: " + remotePath + " — " + client.getReplyString().trim());
        }
        log.fine(() -> "deleteFile complete: " + remotePath);
    }

    @Override
    public void renameFile(String remotePath, String newPath) throws IOException {
        log.fine(() -> "renameFile " + remotePath + " -> " + newPath);
        if (!client.rename(remotePath, newPath)) {
            throw new IOException("Failed to rename file: " + remotePath + " -> " + newPath + " — " + client.getReplyString().trim());
        }
        log.fine(() -> "renameFile complete: " + remotePath + " -> " + newPath);
    }

    @Override
    public void deleteFolder(String remotePath, boolean includeContents) throws IOException {
        log.fine(() -> "deleteFolder " + remotePath + " includeContents=" + includeContents);
        if (includeContents) {
            deleteFolderRecursive(remotePath);
        } else {
            if (!client.removeDirectory(remotePath)) {
                throw new IOException("Failed to delete folder: " + remotePath + " — " + client.getReplyString().trim());
            }
        }
        log.fine(() -> "deleteFolder complete: " + remotePath);
    }

    @Override
    public void moveFile(String remotePath, String destinationPath) throws IOException {
        log.fine(() -> "moveFile " + remotePath + " -> " + destinationPath);
        if (!client.rename(remotePath, destinationPath)) {
            throw new IOException("Failed to move file: " + remotePath + " -> " + destinationPath + " — " + client.getReplyString().trim());
        }
        log.fine(() -> "moveFile complete: " + remotePath + " -> " + destinationPath);
    }

    @Override
    public void renameFolder(String remotePath, String newPath) throws IOException {
        log.fine(() -> "renameFolder " + remotePath + " -> " + newPath);
        if (!client.rename(remotePath, newPath)) {
            throw new IOException("Failed to rename folder: " + remotePath + " -> " + newPath + " — " + client.getReplyString().trim());
        }
        log.fine(() -> "renameFolder complete: " + remotePath + " -> " + newPath);
    }

    @Override
    public void moveFolder(String remotePath, String destinationPath) throws IOException {
        log.fine(() -> "moveFolder " + remotePath + " -> " + destinationPath);
        if (!client.rename(remotePath, destinationPath)) {
            throw new IOException("Failed to move folder: " + remotePath + " -> " + destinationPath + " — " + client.getReplyString().trim());
        }
        log.fine(() -> "moveFolder complete: " + remotePath + " -> " + destinationPath);
    }

    private void deleteFolderRecursive(String path) throws IOException {
        log.finer(() -> "deleteFolderRecursive listing " + path);
        FTPFile[] files = client.listFiles(path);
        if (files != null) {
            for (FTPFile file : files) {
                String name = file.getName();
                if (".".equals(name) || "..".equals(name)) continue;
                String fullPath = path.endsWith("/") ? path + name : path + "/" + name;
                if (file.isDirectory()) {
                    deleteFolderRecursive(fullPath);
                } else {
                    log.finer(() -> "deleteFolderRecursive deleting file " + fullPath);
                    client.deleteFile(fullPath);
                }
            }
        }
        log.finer(() -> "deleteFolderRecursive removing directory " + path);
        client.removeDirectory(path);
    }

    @Override
    public List<FileInfo> listFiles(String remotePath) throws IOException {
        log.fine(() -> "listFiles " + remotePath);
        FTPFile[] files = client.listFiles(remotePath);
        List<FileInfo> result = new ArrayList<>();
        if (files != null) {
            for (FTPFile file : files) {
                if (file.isFile()) {
                    FileInfo info = toFileInfo(remotePath, file);
                    log.finer(() -> "listFiles entry: " + info);
                    result.add(info);
                }
            }
        }
        log.fine(() -> "listFiles " + remotePath + " -> " + result.size() + " file(s)");
        return result;
    }

    @Override
    public List<FolderInfo> listFolders(String remotePath) throws IOException {
        log.fine(() -> "listFolders " + remotePath);
        FTPFile[] files = client.listFiles(remotePath);
        List<FolderInfo> result = new ArrayList<>();
        if (files != null) {
            for (FTPFile file : files) {
                String name = file.getName();
                if (file.isDirectory() && !".".equals(name) && !"..".equals(name)) {
                    FolderInfo info = toFolderInfo(remotePath, file);
                    log.finer(() -> "listFolders entry: " + info);
                    result.add(info);
                }
            }
        }
        log.fine(() -> "listFolders " + remotePath + " -> " + result.size() + " folder(s)");
        return result;
    }

    @Override
    public FileInfo getFileInfo(String remotePath) throws IOException {
        log.fine(() -> "getFileInfo " + remotePath);
        int lastSlash = remotePath.lastIndexOf('/');
        String parent = lastSlash > 0 ? remotePath.substring(0, lastSlash) : "/";
        String name   = remotePath.substring(lastSlash + 1);
        FTPFile[] files = client.listFiles(parent);
        if (files != null) {
            for (FTPFile file : files) {
                if (file.isFile() && name.equals(file.getName())) {
                    FileInfo info = toFileInfo(parent, file);
                    log.finer(() -> "getFileInfo result: " + info);
                    return info;
                }
            }
        }
        throw new IOException("File not found: " + remotePath);
    }

    @Override
    public FolderInfo getFolderInfo(String remotePath) throws IOException {
        log.fine(() -> "getFolderInfo " + remotePath);
        int lastSlash = remotePath.lastIndexOf('/');
        String parent = lastSlash > 0 ? remotePath.substring(0, lastSlash) : "/";
        String name   = remotePath.substring(lastSlash + 1);
        FTPFile[] files = client.listFiles(parent);
        if (files != null) {
            for (FTPFile file : files) {
                if (file.isDirectory() && name.equals(file.getName())) {
                    FolderInfo info = toFolderInfo(parent, file);
                    log.finer(() -> "getFolderInfo result: " + info);
                    return info;
                }
            }
        }
        throw new IOException("Folder not found: " + remotePath);
    }

    private FileInfo toFileInfo(String basePath, FTPFile file) {
        String name = file.getName();
        String path = basePath.endsWith("/") ? basePath + name : basePath + "/" + name;
        LocalDateTime lastModified = file.getTimestamp() != null
                ? file.getTimestamp().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime()
                : null;
        return new FileInfo(name, path, file.getSize(), lastModified);
    }

    private FolderInfo toFolderInfo(String basePath, FTPFile file) {
        String name = file.getName();
        String path = basePath.endsWith("/") ? basePath + name : basePath + "/" + name;
        LocalDateTime lastModified = file.getTimestamp() != null
                ? file.getTimestamp().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime()
                : null;
        return new FolderInfo(name, path, lastModified);
    }

    @Override
    public void close() throws IOException {
        log.fine(() -> "FTP closing connection to " + host);
        if (client.isConnected()) {
            try {
                client.logout();
            } finally {
                client.disconnect();
            }
        }
        log.finer(() -> "FTP connection closed: " + host);
    }
}
