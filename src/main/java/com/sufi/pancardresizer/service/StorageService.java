package com.sufi.pancardresizer.service;

import com.sufi.pancardresizer.config.AppProperties;
import com.sufi.pancardresizer.exception.AppException;
import com.sufi.pancardresizer.model.StoredFile;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StorageService {
    private final AppProperties properties;
    private final Map<String, StoredFile> store = new ConcurrentHashMap<>();
    private final Path baseDir;

    public StorageService(AppProperties properties) throws IOException {
        this.properties = properties;
        this.baseDir = Paths.get(properties.getTempDir());
        Files.createDirectories(this.baseDir);
    }

    public List<StoredFile> storeFiles(MultipartFile[] files) {
        long total = Arrays.stream(files).mapToLong(MultipartFile::getSize).sum();
        if (total > properties.getUpload().getMaxTotalBytes()) {
            throw new AppException("Total upload exceeds limit", "upload_too_large");
        }

        List<StoredFile> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            if (file.getSize() > properties.getUpload().getMaxPerFileBytes()) {
                throw new AppException("File exceeds per-file limit", "file_too_large");
            }
            String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("upload");
            String ext = FilenameUtils.getExtension(originalName).toLowerCase(Locale.ROOT);
            String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

            if (!isAllowed(ext, contentType)) {
                throw new AppException("Unsupported file type", "unsupported_type");
            }

            String fileId = UUID.randomUUID().toString();
            String safeExt = ext.isEmpty() ? "bin" : ext;
            Path target = baseDir.resolve(fileId + "." + safeExt);

            try (InputStream is = file.getInputStream()) {
                Files.copy(is, target);
            } catch (IOException e) {
                throw new AppException("Failed to store file", "store_failed");
            }

            Integer width = null;
            Integer height = null;
            String format = null;
            if (isImage(ext, contentType)) {
                try {
                    BufferedImage img = ImageIO.read(target.toFile());
                    if (img == null) {
                        throw new AppException("Invalid image file", "invalid_image");
                    }
                    width = img.getWidth();
                    height = img.getHeight();
                    format = "image";
                } catch (IOException e) {
                    throw new AppException("Invalid image file", "invalid_image");
                }
            } else if (isPdf(ext, contentType)) {
                format = "pdf";
            }

            StoredFile stored = new StoredFile(fileId, originalName, target, contentType, file.getSize(), width, height, format);
            store.put(fileId, stored);
            result.add(stored);
        }
        return result;
    }

    public StoredFile getFile(String fileId) {
        StoredFile stored = store.get(fileId);
        if (stored == null) {
            throw new AppException("File not found or expired", "file_not_found");
        }
        return stored;
    }

    public void removeFile(String fileId) {
        StoredFile stored = store.remove(fileId);
        if (stored != null) {
            try {
                Files.deleteIfExists(stored.getPath());
            } catch (IOException ignored) {
            }
        }
    }

    public void cleanupExpired() {
        Instant cutoff = Instant.now().minusSeconds(properties.getCleanupMinutes() * 60L);
        for (StoredFile stored : new ArrayList<>(store.values())) {
            if (stored.getCreatedAt().isBefore(cutoff)) {
                removeFile(stored.getFileId());
            }
        }
    }

    private boolean isAllowed(String ext, String contentType) {
        return isImage(ext, contentType) || isPdf(ext, contentType);
    }

    private boolean isImage(String ext, String contentType) {
        return contentType.startsWith("image/") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png");
    }

    private boolean isPdf(String ext, String contentType) {
        return contentType.equals("application/pdf") || ext.equals("pdf");
    }
}
