package com.ftphelper;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Polls a remote folder at a fixed interval and fires {@link FolderChangeEvent}s
 * for additions, deletions, and modifications.
 *
 * <p>Obtain an instance via {@link FtpHelper#watchFolder}. Call {@link #close()}
 * (or use try-with-resources) to stop polling.</p>
 *
 * <pre>
 * try (FolderWatcher watcher = FtpHelper.watchFolder(conn, "/remote/path", 10, TimeUnit.SECONDS, event -> {
 *     System.out.println(event.getType() + " " + event.getFile().getName());
 * })) {
 *     Thread.sleep(60_000); // watch for 60 seconds
 * }
 * </pre>
 */
public class FolderWatcher implements Closeable {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ftphelper-folder-watcher");
        t.setDaemon(true);
        return t;
    });

    FolderWatcher(ConnectionDetails conn, String remotePath,
                  long interval, TimeUnit unit,
                  Consumer<FolderChangeEvent> listener) {

        // Snapshot is captured lazily on the first poll so we don't block the constructor.
        Map<String, FileInfo> snapshot = new HashMap<>();
        boolean[] initialized = {false};

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<FileInfo> current = FtpHelper.listFiles(conn, remotePath);
                Map<String, FileInfo> currentMap = new HashMap<>();
                for (FileInfo f : current) {
                    currentMap.put(f.getPath(), f);
                }

                if (!initialized[0]) {
                    // First poll: establish the baseline without firing events.
                    snapshot.putAll(currentMap);
                    initialized[0] = true;
                    return;
                }

                // Detect additions and modifications.
                for (Map.Entry<String, FileInfo> entry : currentMap.entrySet()) {
                    FileInfo previous = snapshot.get(entry.getKey());
                    if (previous == null) {
                        listener.accept(new FolderChangeEvent(FolderChangeType.ADDED, entry.getValue()));
                    } else if (isModified(previous, entry.getValue())) {
                        listener.accept(new FolderChangeEvent(FolderChangeType.MODIFIED, entry.getValue()));
                    }
                }

                // Detect deletions.
                for (Map.Entry<String, FileInfo> entry : snapshot.entrySet()) {
                    if (!currentMap.containsKey(entry.getKey())) {
                        listener.accept(new FolderChangeEvent(FolderChangeType.DELETED, entry.getValue()));
                    }
                }

                snapshot.clear();
                snapshot.putAll(currentMap);

            } catch (IOException e) {
                // Swallow and retry on next tick; callers can log via the listener if desired.
            }
        }, 0, interval, unit);
    }

    private static boolean isModified(FileInfo previous, FileInfo current) {
        if (previous.getSize() != current.getSize()) return true;
        if (previous.getLastModified() == null && current.getLastModified() == null) return false;
        if (previous.getLastModified() == null || current.getLastModified() == null) return true;
        return !previous.getLastModified().equals(current.getLastModified());
    }

    /**
     * Stops polling. Blocks until the background thread has terminated (up to 5 seconds).
     */
    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
