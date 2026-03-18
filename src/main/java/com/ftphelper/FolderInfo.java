package com.ftphelper;

import java.time.LocalDateTime;

/**
 * Metadata about a remote folder/directory.
 */
public class FolderInfo {

    private final String name;
    private final String path;
    private final LocalDateTime lastModified;

    public FolderInfo(String name, String path, LocalDateTime lastModified) {
        this.name = name;
        this.path = path;
        this.lastModified = lastModified;
    }

    public String getName()              { return name; }
    public String getPath()              { return path; }
    public LocalDateTime getLastModified() { return lastModified; }

    @Override
    public String toString() {
        return "FolderInfo{name='" + name + "', path='" + path + "', lastModified=" + lastModified + '}';
    }
}
