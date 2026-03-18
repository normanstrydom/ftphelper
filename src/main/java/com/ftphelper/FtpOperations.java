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

/**
 * FTP implementation using Apache Commons Net. Package-private.
 */
class FtpOperations implements RemoteOperations {

    private final FTPClient client;

    FtpOperations(ConnectionDetails conn) throws IOException {
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
    }

    @Override
    public void writeFile(String remotePath, InputStream data) throws IOException {
        if (!client.storeFile(remotePath, data)) {
            throw new IOException("Failed to write file: " + remotePath + " — " + client.getReplyString().trim());
        }
    }

    @Override
    public InputStream readFile(String remotePath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!client.retrieveFile(remotePath, baos)) {
            throw new IOException("Failed to read file: " + remotePath + " — " + client.getReplyString().trim());
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public void deleteFile(String remotePath) throws IOException {
        if (!client.deleteFile(remotePath)) {
            throw new IOException("Failed to delete file: " + remotePath + " — " + client.getReplyString().trim());
        }
    }

    @Override
    public void deleteFolder(String remotePath, boolean includeContents) throws IOException {
        if (includeContents) {
            deleteFolderRecursive(remotePath);
        } else {
            if (!client.removeDirectory(remotePath)) {
                throw new IOException("Failed to delete folder: " + remotePath + " — " + client.getReplyString().trim());
            }
        }
    }

    private void deleteFolderRecursive(String path) throws IOException {
        FTPFile[] files = client.listFiles(path);
        if (files != null) {
            for (FTPFile file : files) {
                String name = file.getName();
                if (".".equals(name) || "..".equals(name)) continue;
                String fullPath = path.endsWith("/") ? path + name : path + "/" + name;
                if (file.isDirectory()) {
                    deleteFolderRecursive(fullPath);
                } else {
                    client.deleteFile(fullPath);
                }
            }
        }
        client.removeDirectory(path);
    }

    @Override
    public List<FileInfo> listFiles(String remotePath) throws IOException {
        FTPFile[] files = client.listFiles(remotePath);
        List<FileInfo> result = new ArrayList<>();
        if (files != null) {
            for (FTPFile file : files) {
                if (file.isFile()) {
                    result.add(toFileInfo(remotePath, file));
                }
            }
        }
        return result;
    }

    @Override
    public List<FolderInfo> listFolders(String remotePath) throws IOException {
        FTPFile[] files = client.listFiles(remotePath);
        List<FolderInfo> result = new ArrayList<>();
        if (files != null) {
            for (FTPFile file : files) {
                String name = file.getName();
                if (file.isDirectory() && !".".equals(name) && !"..".equals(name)) {
                    result.add(toFolderInfo(remotePath, file));
                }
            }
        }
        return result;
    }

    @Override
    public FileInfo getFileInfo(String remotePath) throws IOException {
        int lastSlash = remotePath.lastIndexOf('/');
        String parent = lastSlash > 0 ? remotePath.substring(0, lastSlash) : "/";
        String name   = remotePath.substring(lastSlash + 1);
        FTPFile[] files = client.listFiles(parent);
        if (files != null) {
            for (FTPFile file : files) {
                if (file.isFile() && name.equals(file.getName())) {
                    return toFileInfo(parent, file);
                }
            }
        }
        throw new IOException("File not found: " + remotePath);
    }

    @Override
    public FolderInfo getFolderInfo(String remotePath) throws IOException {
        int lastSlash = remotePath.lastIndexOf('/');
        String parent = lastSlash > 0 ? remotePath.substring(0, lastSlash) : "/";
        String name   = remotePath.substring(lastSlash + 1);
        FTPFile[] files = client.listFiles(parent);
        if (files != null) {
            for (FTPFile file : files) {
                if (file.isDirectory() && name.equals(file.getName())) {
                    return toFolderInfo(parent, file);
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
        if (client.isConnected()) {
            try {
                client.logout();
            } finally {
                client.disconnect();
            }
        }
    }
}
