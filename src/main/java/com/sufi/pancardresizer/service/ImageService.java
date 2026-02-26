package com.sufi.pancardresizer.service;

import com.sufi.pancardresizer.dto.CropRect;
import com.sufi.pancardresizer.dto.RenderPhotoRequest;
import com.sufi.pancardresizer.dto.RenderSignatureRequest;
import com.sufi.pancardresizer.exception.AppException;
import com.sufi.pancardresizer.model.StoredFile;
import com.sufi.pancardresizer.util.ImageUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImageService {
    public static final int DPI = 200;
    public static final int PHOTO_WIDTH_PX = (int) Math.round(2.5 / 2.54 * DPI);
    public static final int PHOTO_HEIGHT_PX = (int) Math.round(3.5 / 2.54 * DPI);
    public static final int SIGN_WIDTH_PX = (int) Math.round(4.5 / 2.54 * DPI);
    public static final int SIGN_HEIGHT_PX = (int) Math.round(2.0 / 2.54 * DPI);

    private final StorageService storageService;
    private final Map<String, SizeCacheEntry> sizeCache = new ConcurrentHashMap<>();
    private static final long SIZE_CACHE_TTL_MS = 10 * 60 * 1000L;

    public ImageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public byte[] renderPhoto(RenderPhotoRequest request) {
        int maxKb = request.getMaxKb() <= 0 ? 50 : request.getMaxKb();
        if (maxKb != 30 && maxKb != 50) {
            maxKb = 50;
        }
        BufferedImage base = loadImage(request.getFileId());
        BufferedImage processed = applyOperations(base, request.getRotate(), request.getCrop());
        BufferedImage resized;
        try {
            resized = ImageUtils.resize(processed, PHOTO_WIDTH_PX, PHOTO_HEIGHT_PX);
        } catch (IOException e) {
            throw new AppException("Failed to resize image", "resize_failed");
        }
        return compressToMaxBytes(resized, DPI, maxKb * 1024);
    }

    public byte[] renderSignature(RenderSignatureRequest request) {
        BufferedImage base = loadImage(request.getFileId());
        BufferedImage processed = applyOperations(base, request.getRotate(), request.getCrop());
        if (request.isBw()) {
            processed = ImageUtils.toBlackAndWhite(processed);
        }
        BufferedImage resized;
        try {
            resized = ImageUtils.resize(processed, SIGN_WIDTH_PX, SIGN_HEIGHT_PX);
        } catch (IOException e) {
            throw new AppException("Failed to resize image", "resize_failed");
        }
        return compressToMaxBytes(resized, DPI, 50 * 1024);
    }

    public byte[] renderPhotoByKb(RenderPhotoRequest request) {
        int targetKb = request.getMaxKb() <= 0 ? 100 : request.getMaxKb();
        targetKb = Math.max(10, Math.min(500, targetKb));
        BufferedImage base = loadImage(request.getFileId());
        BufferedImage processed = applyOperations(base, request.getRotate(), request.getCrop());
        return compressToExactBytes(processed, DPI, targetKb * 1024);
    }

    public byte[] renderPhotoByKbBestEffort(RenderPhotoRequest request) {
        int targetKb = request.getMaxKb() <= 0 ? 100 : request.getMaxKb();
        targetKb = Math.max(10, Math.min(500, targetKb));
        BufferedImage base = loadImage(request.getFileId());
        BufferedImage processed = applyOperations(base, request.getRotate(), request.getCrop());
        return compressToMaxBytes(processed, DPI, targetKb * 1024);
    }

    public SizeResult getKbSize(RenderPhotoRequest request) {
        String key = buildSizeKey(request);
        SizeCacheEntry cached = sizeCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return new SizeResult(cached.data.length, cached.exact);
        }

        try {
            byte[] data = renderPhotoByKb(request);
            sizeCache.put(key, new SizeCacheEntry(data, true));
            return new SizeResult(data.length, true);
        } catch (AppException ex) {
            byte[] data = renderPhotoByKbFastSize(request);
            sizeCache.put(key, new SizeCacheEntry(data, false));
            return new SizeResult(data.length, false);
        }
    }

    private BufferedImage loadImage(String fileId) {
        StoredFile stored = storageService.getFile(fileId);
        if (stored.getFormat() == null || !stored.getFormat().equals("image")) {
            throw new AppException("Unsupported file for image render", "image_required");
        }
        try {
            BufferedImage img = ImageIO.read(stored.getPath().toFile());
            if (img == null) {
                throw new AppException("Invalid image file", "invalid_image");
            }
            return img;
        } catch (IOException e) {
            throw new AppException("Failed to read image", "image_read_failed");
        }
    }

    private BufferedImage applyOperations(BufferedImage src, int rotate, CropRect crop) {
        BufferedImage image = ImageUtils.rotate(src, rotate);
        if (crop != null) {
            image = ImageUtils.crop(image, crop.getX(), crop.getY(), crop.getWidth(), crop.getHeight());
        }
        return ImageUtils.toRgb(image);
    }

    private byte[] compressToMaxBytes(BufferedImage image, int dpi, int maxBytes) {
        return compressToMaxBytes(image, dpi, maxBytes, 8);
    }

    private byte[] compressToExactBytes(BufferedImage image, int dpi, int targetBytes) {
        BufferedImage current = image;
        byte[] data = null;

        for (int i = 0; i < 10; i++) {
            data = compressToMaxBytes(current, dpi, targetBytes);
            if (data.length <= targetBytes) {
                return ImageUtils.padJpegToSize(data, targetBytes);
            }
            if (current.getWidth() < 120 || current.getHeight() < 120) {
                break;
            }
            try {
                current = ImageUtils.zoom(current, 0.9);
            } catch (IOException e) {
                throw new AppException("Failed to resize image", "resize_failed");
            }
        }

        if (data != null && data.length <= targetBytes) {
            return ImageUtils.padJpegToSize(data, targetBytes);
        }
        throw new AppException("Target size not reachable. Try a smaller KB value.", "target_size_unreachable");
    }

    private byte[] compressToMaxBytes(BufferedImage image, int dpi, int maxBytes, int iterations) {
        float low = 0.2f;
        float high = 0.95f;
        byte[] best = null;
        int safeIterations = Math.max(1, iterations);
        for (int i = 0; i < safeIterations; i++) {
            float mid = (low + high) / 2f;
            byte[] data;
            try {
                data = ImageUtils.writeJpegWithQualityAndDpi(image, mid, dpi);
            } catch (IOException e) {
                throw new AppException("Failed to encode image", "encode_failed");
            }
            if (data.length <= maxBytes) {
                best = data;
                low = mid;
            } else {
                high = mid;
            }
        }
        if (best != null) {
            return best;
        }
        try {
            return ImageUtils.writeJpegWithQualityAndDpi(image, low, dpi);
        } catch (IOException e) {
            throw new AppException("Failed to encode image", "encode_failed");
        }
    }

    private byte[] renderPhotoByKbFastSize(RenderPhotoRequest request) {
        int targetKb = request.getMaxKb() <= 0 ? 100 : request.getMaxKb();
        targetKb = Math.max(10, Math.min(500, targetKb));
        BufferedImage base = loadImage(request.getFileId());
        BufferedImage processed = applyOperations(base, request.getRotate(), request.getCrop());

        byte[] data = compressToMaxBytes(processed, DPI, targetKb * 1024, 4);
        if (data.length <= targetKb * 1024) {
            return data;
        }
        try {
            BufferedImage smaller = ImageUtils.zoom(processed, 0.9);
            return compressToMaxBytes(smaller, DPI, targetKb * 1024, 4);
        } catch (IOException e) {
            throw new AppException("Failed to resize image", "resize_failed");
        }
    }

    private String buildSizeKey(RenderPhotoRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getFileId()).append('|')
            .append(request.getRotate()).append('|')
            .append(request.getMaxKb()).append('|');
        CropRect crop = request.getCrop();
        if (crop == null) {
            sb.append("nocrop");
        } else {
            sb.append(crop.getX()).append(',')
                .append(crop.getY()).append(',')
                .append(crop.getWidth()).append(',')
                .append(crop.getHeight());
        }
        return sb.toString();
    }

    private static class SizeCacheEntry {
        private final byte[] data;
        private final boolean exact;
        private final long createdAt;

        private SizeCacheEntry(byte[] data, boolean exact) {
            this.data = data;
            this.exact = exact;
            this.createdAt = Instant.now().toEpochMilli();
        }

        private boolean isExpired() {
            return (Instant.now().toEpochMilli() - createdAt) > SIZE_CACHE_TTL_MS;
        }
    }

    public static class SizeResult {
        private final long sizeBytes;
        private final boolean exact;

        public SizeResult(long sizeBytes, boolean exact) {
            this.sizeBytes = sizeBytes;
            this.exact = exact;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public boolean isExact() {
            return exact;
        }
    }
}
