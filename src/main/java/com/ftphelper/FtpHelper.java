package com.ftphelper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified static utility for FTP and SFTP operations.
 *
 * <p>All methods accept a {@link ConnectionDetails} object that specifies the host,
 * port, credentials, and protocol (FTP or SFTP). The same method signatures work
 * for both protocols — the implementation is selected automatically.</p>
 *
 * <pre>
 * ConnectionDetails conn = new ConnectionDetails("myserver.com", "user", "pass", Protocol.SFTP);
 *
 * // Upload a file
 * FtpHelper.writeFile(conn, "/remote/path/file.txt", inputStream);
 *
 * // List all files in a directory
 * List&lt;FileInfo&gt; files = FtpHelper.listFiles(conn, "/remote/path");
 * </pre>
 */
public class FtpHelper {

    private FtpHelper() {}

    // -------------------------------------------------------------------------
    // File operations
    // -------------------------------------------------------------------------

    /**
     * Uploads data from {@code inputStream} to {@code remotePath} on the server.
     */
    public static void writeFile(ConnectionDetails conn, String remotePath, InputStream data)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            ops.writeFile(remotePath, data);
        }
    }

    /**
     * Downloads the file at {@code remotePath} and returns its content as an
     * {@link InputStream}. The stream is fully buffered in memory so the
     * connection is safely closed before returning.
     */
    public static InputStream readFile(ConnectionDetails conn, String remotePath)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            return ops.readFile(remotePath);
        }
    }

    /**
     * Deletes the file at {@code remotePath}.
     */
    public static void deleteFile(ConnectionDetails conn, String remotePath)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            ops.deleteFile(remotePath);
        }
    }

    // -------------------------------------------------------------------------
    // Folder operations
    // -------------------------------------------------------------------------

    /**
     * Deletes the folder at {@code remotePath}.
     *
     * @param includeContents if {@code true}, recursively deletes all contents
     *                        before removing the folder; if {@code false}, the
     *                        folder must be empty or the operation will fail.
     */
    public static void deleteFolder(ConnectionDetails conn, String remotePath, boolean includeContents)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            ops.deleteFolder(remotePath, includeContents);
        }
    }

    // -------------------------------------------------------------------------
    // Listing — files
    // -------------------------------------------------------------------------

    /**
     * Returns metadata for all files directly inside {@code remotePath}.
     */
    public static List<FileInfo> listFiles(ConnectionDetails conn, String remotePath)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            return ops.listFiles(remotePath);
        }
    }

    /**
     * Returns metadata for files inside {@code remotePath} whose last-modified
     * date falls within [{@code from}, {@code to}] inclusive.
     */
    public static List<FileInfo> listFiles(ConnectionDetails conn, String remotePath,
                                           LocalDate from, LocalDate to)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            return ops.listFiles(remotePath).stream()
                    .filter(f -> f.getLastModified() != null)
                    .filter(f -> {
                        LocalDate date = f.getLastModified().toLocalDate();
                        return !date.isBefore(from) && !date.isAfter(to);
                    })
                    .collect(Collectors.toList());
        }
    }

    // -------------------------------------------------------------------------
    // Listing — folders
    // -------------------------------------------------------------------------

    /**
     * Returns metadata for all sub-folders directly inside {@code remotePath}.
     */
    public static List<FolderInfo> listFolders(ConnectionDetails conn, String remotePath)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            return ops.listFolders(remotePath);
        }
    }

    /**
     * Returns metadata for sub-folders inside {@code remotePath} whose
     * last-modified date falls within [{@code from}, {@code to}] inclusive.
     */
    public static List<FolderInfo> listFolders(ConnectionDetails conn, String remotePath,
                                               LocalDate from, LocalDate to)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            return ops.listFolders(remotePath).stream()
                    .filter(f -> f.getLastModified() != null)
                    .filter(f -> {
                        LocalDate date = f.getLastModified().toLocalDate();
                        return !date.isBefore(from) && !date.isAfter(to);
                    })
                    .collect(Collectors.toList());
        }
    }

    // -------------------------------------------------------------------------
    // Info
    // -------------------------------------------------------------------------

    /**
     * Returns metadata for a single file at {@code remotePath}.
     *
     * @throws IOException if the file does not exist
     */
    public static FileInfo getFileInfo(ConnectionDetails conn, String remotePath)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            return ops.getFileInfo(remotePath);
        }
    }

    /**
     * Returns metadata for a single folder at {@code remotePath}.
     *
     * @throws IOException if the folder does not exist
     */
    public static FolderInfo getFolderInfo(ConnectionDetails conn, String remotePath)
            throws IOException {
        try (RemoteOperations ops = connect(conn)) {
            return ops.getFolderInfo(remotePath);
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static RemoteOperations connect(ConnectionDetails conn) throws IOException {
        return conn.getProtocol() == Protocol.SFTP
                ? new SftpOperations(conn)
                : new FtpOperations(conn);
    }
}
