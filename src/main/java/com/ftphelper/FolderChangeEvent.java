package com.ftphelper;

/** A single change detected during folder watching. */
public class FolderChangeEvent {

    private final FolderChangeType type;
    private final FileInfo file;

    public FolderChangeEvent(FolderChangeType type, FileInfo file) {
        this.type = type;
        this.file = file;
    }

    /** The kind of change (ADDED, DELETED, or MODIFIED). */
    public FolderChangeType getType() { return type; }

    /** Metadata for the file that changed. For DELETED events, this reflects the last known state. */
    public FileInfo getFile() { return file; }

    @Override
    public String toString() {
        return "FolderChangeEvent{type=" + type + ", file=" + file + '}';
    }
}
