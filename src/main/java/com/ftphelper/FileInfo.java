package com.ftphelper;

import java.time.LocalDateTime;

/**
 * Metadata about a remote file.
 */
public class FileInfo {

    private final String name;
    private final String path;
    private final long size;
    private final LocalDateTime lastModified;

    public FileInfo(String name, String path, long size, LocalDateTime lastModified) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getName()              { return name; }
    public String getPath()              { return path; }
    public long getSize()                { return size; }
    public LocalDateTime getLastModified() { return lastModified; }

    @Override
    public String toString() {
        return "FileInfo{name='" + name + "', path='" + path + "', size=" + size
                + ", lastModified=" + lastModified + '}';
    }
}
