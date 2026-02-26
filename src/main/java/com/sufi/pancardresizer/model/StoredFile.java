package com.sufi.pancardresizer.model;

import java.nio.file.Path;
import java.time.Instant;

public class StoredFile {
    private final String fileId;
    private final String originalName;
    private final Path path;
    private final String contentType;
    private final long sizeBytes;
    private final Integer width;
    private final Integer height;
    private final String format;
    private final Instant createdAt;

    public StoredFile(String fileId, String originalName, Path path, String contentType, long sizeBytes, Integer width, Integer height, String format) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.path = path;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.format = format;
        this.createdAt = Instant.now();
    }

    public String getFileId() {
        return fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Path getPath() {
        return path;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getFormat() {
        return format;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
