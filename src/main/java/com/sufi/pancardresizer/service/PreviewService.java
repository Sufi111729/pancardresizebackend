package com.sufi.pancardresizer.service;

import com.sufi.pancardresizer.dto.CropRect;
import com.sufi.pancardresizer.dto.PreviewRequest;
import com.sufi.pancardresizer.exception.AppException;
import com.sufi.pancardresizer.model.StoredFile;
import com.sufi.pancardresizer.util.ImageUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class PreviewService {
    private final StorageService storageService;

    public PreviewService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Cacheable(value = "previewCache", key = "#request.fileId + ':' + #request.rotate + ':' + #request.zoom + ':' + (#request.crop == null ? 'none' : (#request.crop.x + ',' + #request.crop.y + ',' + #request.crop.width + ',' + #request.crop.height))")
    public byte[] buildPreview(PreviewRequest request) {
        StoredFile stored = storageService.getFile(request.getFileId());
        if (stored.getFormat() == null || !stored.getFormat().equals("image")) {
            throw new AppException("Preview only supports images", "preview_unsupported");
        }

        BufferedImage src;
        try {
            src = ImageIO.read(stored.getPath().toFile());
        } catch (IOException e) {
            throw new AppException("Failed to read image", "image_read_failed");
        }

        BufferedImage image = ImageUtils.rotate(src, request.getRotate());
        CropRect crop = request.getCrop();
        if (crop != null) {
            image = ImageUtils.crop(image, crop.getX(), crop.getY(), crop.getWidth(), crop.getHeight());
        }
        try {
            image = ImageUtils.zoom(image, request.getZoom());
            image = ImageUtils.resizeKeepAspect(image, 900, 900);
        } catch (IOException e) {
            throw new AppException("Failed to build preview", "preview_failed");
        }

        try {
            return ImageUtils.writeJpegWithQualityAndDpi(image, 0.75f, 96);
        } catch (IOException e) {
            throw new AppException("Failed to build preview", "preview_failed");
        }
    }
}
