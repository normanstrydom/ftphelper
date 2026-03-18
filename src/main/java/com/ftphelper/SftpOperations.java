package com.ftphelper;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * SFTP implementation using JSch. Package-private.
 */
class SftpOperations implements RemoteOperations {

    private static final Logger log = Logger.getLogger(SftpOperations.class.getName());

    private final Session session;
    private final ChannelSftp channel;
    private final String host;

    SftpOperations(ConnectionDetails conn) throws IOException {
        this.host = conn.getHost() + ":" + conn.getPort();
        log.fine(() -> "SFTP connecting to " + host + " as " + conn.getUsername());
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(conn.getUsername(), conn.getHost(), conn.getPort());
            session.setPassword(conn.getPassword());
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            log.finer(() -> "SFTP session established, opening channel");
            Channel ch = session.openChannel("sftp");
            ch.connect();
            channel = (ChannelSftp) ch;
            log.fine(() -> "SFTP connected: " + host);
        } catch (JSchException e) {
            throw new IOException("Failed to connect via SFTP to " + conn.getHost(), e);
        }
    }

    @Override
    public void writeFile(String remotePath, InputStream data) throws IOException {
        log.fine(() -> "writeFile " + remotePath);
        try {
            channel.put(data, remotePath);
        } catch (SftpException e) {
            throw new IOException("Failed to write file: " + remotePath, e);
        }
        log.fine(() -> "writeFile complete: " + remotePath);
    }

    @Override
    public InputStream readFile(String remotePath) throws IOException {
        log.fine(() -> "readFile " + remotePath);
        try {
            InputStream is = channel.get(remotePath);
            byte[] bytes = is.readAllBytes();
            log.finer(() -> "readFile complete: " + remotePath + " (" + bytes.length + " bytes)");
            return new ByteArrayInputStream(bytes);
        } catch (SftpException e) {
            throw new IOException("Failed to read file: " + remotePath, e);
        }
    }

    @Override
    public void deleteFile(String remotePath) throws IOException {
        log.fine(() -> "deleteFile " + remotePath);
        try {
            channel.rm(remotePath);
        } catch (SftpException e) {
            throw new IOException("Failed to delete file: " + remotePath, e);
        }
        log.fine(() -> "deleteFile complete: " + remotePath);
    }

    @Override
    public void deleteFolder(String remotePath, boolean includeContents) throws IOException {
        log.fine(() -> "deleteFolder " + remotePath + " includeContents=" + includeContents);
        try {
            if (includeContents) {
                deleteFolderRecursive(remotePath);
            } else {
                channel.rmdir(remotePath);
            }
        } catch (SftpException e) {
            throw new IOException("Failed to delete folder: " + remotePath, e);
        }
        log.fine(() -> "deleteFolder complete: " + remotePath);
    }

    private void deleteFolderRecursive(String path) throws SftpException {
        log.finer(() -> "deleteFolderRecursive listing " + path);
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> entries = channel.ls(path);
        for (ChannelSftp.LsEntry entry : entries) {
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) continue;
            String fullPath = path.endsWith("/") ? path + name : path + "/" + name;
            if (entry.getAttrs().isDir()) {
                deleteFolderRecursive(fullPath);
            } else {
                log.finer(() -> "deleteFolderRecursive deleting file " + fullPath);
                channel.rm(fullPath);
            }
        }
        log.finer(() -> "deleteFolderRecursive removing directory " + path);
        channel.rmdir(path);
    }

    @Override
    public List<FileInfo> listFiles(String remotePath) throws IOException {
        log.fine(() -> "listFiles " + remotePath);
        try {
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = channel.ls(remotePath);
            List<FileInfo> result = new ArrayList<>();
            for (ChannelSftp.LsEntry entry : entries) {
                String name = entry.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;
                if (!entry.getAttrs().isDir()) {
                    FileInfo info = toFileInfo(remotePath, entry);
                    log.finer(() -> "listFiles entry: " + info);
                    result.add(info);
                }
            }
            log.fine(() -> "listFiles " + remotePath + " -> " + result.size() + " file(s)");
            return result;
        } catch (SftpException e) {
            throw new IOException("Failed to list files in: " + remotePath, e);
        }
    }

    @Override
    public List<FolderInfo> listFolders(String remotePath) throws IOException {
        log.fine(() -> "listFolders " + remotePath);
        try {
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = channel.ls(remotePath);
            List<FolderInfo> result = new ArrayList<>();
            for (ChannelSftp.LsEntry entry : entries) {
                String name = entry.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;
                if (entry.getAttrs().isDir()) {
                    FolderInfo info = toFolderInfo(remotePath, entry);
                    log.finer(() -> "listFolders entry: " + info);
                    result.add(info);
                }
            }
            log.fine(() -> "listFolders " + remotePath + " -> " + result.size() + " folder(s)");
            return result;
        } catch (SftpException e) {
            throw new IOException("Failed to list folders in: " + remotePath, e);
        }
    }

    @Override
    public FileInfo getFileInfo(String remotePath) throws IOException {
        log.fine(() -> "getFileInfo " + remotePath);
        try {
            SftpATTRS attrs = channel.stat(remotePath);
            String name = remotePath.substring(remotePath.lastIndexOf('/') + 1);
            LocalDateTime lastModified = LocalDateTime.ofEpochSecond(attrs.getMTime(), 0, ZoneOffset.UTC);
            FileInfo info = new FileInfo(name, remotePath, attrs.getSize(), lastModified);
            log.finer(() -> "getFileInfo result: " + info);
            return info;
        } catch (SftpException e) {
            throw new IOException("Failed to get file info: " + remotePath, e);
        }
    }

    @Override
    public FolderInfo getFolderInfo(String remotePath) throws IOException {
        log.fine(() -> "getFolderInfo " + remotePath);
        try {
            SftpATTRS attrs = channel.stat(remotePath);
            String name = remotePath.substring(remotePath.lastIndexOf('/') + 1);
            LocalDateTime lastModified = LocalDateTime.ofEpochSecond(attrs.getMTime(), 0, ZoneOffset.UTC);
            FolderInfo info = new FolderInfo(name, remotePath, lastModified);
            log.finer(() -> "getFolderInfo result: " + info);
            return info;
        } catch (SftpException e) {
            throw new IOException("Failed to get folder info: " + remotePath, e);
        }
    }

    private FileInfo toFileInfo(String basePath, ChannelSftp.LsEntry entry) {
        String name = entry.getFilename();
        String path = basePath.endsWith("/") ? basePath + name : basePath + "/" + name;
        LocalDateTime lastModified = LocalDateTime.ofEpochSecond(entry.getAttrs().getMTime(), 0, ZoneOffset.UTC);
        return new FileInfo(name, path, entry.getAttrs().getSize(), lastModified);
    }

    private FolderInfo toFolderInfo(String basePath, ChannelSftp.LsEntry entry) {
        String name = entry.getFilename();
        String path = basePath.endsWith("/") ? basePath + name : basePath + "/" + name;
        LocalDateTime lastModified = LocalDateTime.ofEpochSecond(entry.getAttrs().getMTime(), 0, ZoneOffset.UTC);
        return new FolderInfo(name, path, lastModified);
    }

    @Override
    public void close() {
        log.fine(() -> "SFTP closing connection to " + host);
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        log.finer(() -> "SFTP connection closed: " + host);
    }
}
