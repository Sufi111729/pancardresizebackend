package com.sufi.pancardresizer.service;

import com.sufi.pancardresizer.exception.AppException;
import com.sufi.pancardresizer.model.StoredFile;
import com.sufi.pancardresizer.util.ImageUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class DocumentService {
    private final StorageService storageService;

    public DocumentService(StorageService storageService) {
        this.storageService = storageService;
    }

    public byte[] renderDocuments(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new AppException("No documents provided", "documents_required");
        }

        return renderDocumentsByMaxBytes(fileIds, 2 * 1024 * 1024, false, false);
    }

    public byte[] renderDocumentsByKb(List<String> fileIds, int maxKb) {
        int safeKb = maxKb <= 0 ? 250 : maxKb;
        safeKb = Math.max(50, Math.min(2048, safeKb));
        return renderDocumentsByMaxBytes(fileIds, safeKb * 1024, true, true);
    }

    public byte[] renderDocumentImage(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new AppException("No documents provided", "documents_required");
        }
        String fileId = fileIds.get(0);
        StoredFile stored = storageService.getFile(fileId);
        BufferedImage image;
        try {
            if ("pdf".equals(stored.getFormat())) {
                try (PDDocument doc = Loader.loadPDF(stored.getPath().toFile())) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    image = renderer.renderImageWithDPI(0, 240f);
                }
            } else if ("image".equals(stored.getFormat())) {
                image = ImageIO.read(stored.getPath().toFile());
                if (image == null) {
                    throw new AppException("Invalid image file", "invalid_image");
                }
            } else {
                throw new AppException("Unsupported file for documents", "unsupported_type");
            }

            image = ImageUtils.toRgb(image);
            image = ImageUtils.trimWhiteBorders(image);
            int maxDim = 2000;
            if (image.getWidth() > maxDim || image.getHeight() > maxDim) {
                image = ImageUtils.resizeKeepAspect(image, maxDim, maxDim);
            }
            return ImageUtils.writeJpegWithQualityAndDpi(image, 0.9f, 200);
        } catch (IOException e) {
            throw new AppException("Failed to build document image", "document_image_failed");
        }
    }

    private byte[] renderDocumentsByMaxBytes(List<String> fileIds, int maxBytes, boolean rasterizePdf, boolean bestEffort) {
        float scale = 1.0f;
        float quality = 0.9f;
        float minScale = 0.8f;
        float minQuality = 0.8f;
        byte[] lastOutput = null;
        for (int attempt = 0; attempt < 9; attempt++) {
            byte[] output = buildPdf(fileIds, scale, quality, rasterizePdf);
            lastOutput = output;
            if (output.length <= maxBytes) {
                return output;
            }
            float nextScale = scale * 0.85f;
            float nextQuality = quality - 0.05f;
            if (nextScale < minScale && nextQuality < minQuality) {
                break;
            }
            if (nextScale >= minScale) {
                scale = nextScale;
            }
            if (nextQuality >= minQuality) {
                quality = nextQuality;
            }
        }
        if (bestEffort && lastOutput != null) {
            return lastOutput;
        }
        throw new AppException("Target size not reachable without quality loss", "pdf_quality_floor");
    }

    private byte[] buildPdf(List<String> fileIds, float scale, float quality, boolean rasterizePdf) {
        try (PDDocument output = new PDDocument()) {
            for (String fileId : fileIds) {
                StoredFile stored = storageService.getFile(fileId);
                if ("pdf".equals(stored.getFormat())) {
                    if (!rasterizePdf) {
                        try (PDDocument doc = Loader.loadPDF(stored.getPath().toFile())) {
                            for (PDPage page : doc.getPages()) {
                                output.importPage(page);
                            }
                        }
                    } else {
                        try (PDDocument doc = Loader.loadPDF(stored.getPath().toFile())) {
                            PDFRenderer renderer = new PDFRenderer(doc);
                            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                                float dpi = 240f * scale;
                                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                                image = ImageUtils.toRgb(image);
                                image = ImageUtils.trimWhiteBorders(image);
                                writeImagePage(output, image, quality);
                            }
                        }
                    }
                } else if ("image".equals(stored.getFormat())) {
                    BufferedImage image = ImageIO.read(stored.getPath().toFile());
                    if (image == null) {
                        throw new AppException("Invalid image file", "invalid_image");
                    }
                    image = ImageUtils.toRgb(image);
                    image = ImageUtils.trimWhiteBorders(image);
                    int maxDim = Math.round(2000 * scale);
                    if (image.getWidth() > maxDim || image.getHeight() > maxDim) {
                        image = ImageUtils.resizeKeepAspect(image, maxDim, maxDim);
                    }
                    writeImagePage(output, image, quality);
                } else {
                    throw new AppException("Unsupported file for documents", "unsupported_type");
                }
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                output.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new AppException("Failed to build PDF", "pdf_failed");
        }
    }

    private void writeImagePage(PDDocument output, BufferedImage image, float quality) throws IOException {
        float widthPts = (image.getWidth() / 200f) * 72f;
        float heightPts = (image.getHeight() / 200f) * 72f;
        float padPts = 12f;
        PDPage page = new PDPage(new PDRectangle(widthPts + padPts * 2, heightPts + padPts * 2));
        output.addPage(page);
        BlackLayer layer = splitBlackMask(image);
        PDImageXObject baseImage = JPEGFactory.createFromImage(output, layer.base, quality);
        try (PDPageContentStream cs = new PDPageContentStream(output, page)) {
            cs.drawImage(baseImage, padPts, padPts, widthPts, heightPts);
            if (layer.hasMask) {
                PDImageXObject maskImage = LosslessFactory.createFromImage(output, layer.mask);
                cs.drawImage(maskImage, padPts, padPts, widthPts, heightPts);
            }
        }
    }

    private BlackLayer splitBlackMask(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage base = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        boolean hasMask = false;
        int blackThreshold = 24;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                boolean isBlack = r <= blackThreshold && g <= blackThreshold && b <= blackThreshold;
                if (isBlack) {
                    base.setRGB(x, y, 0xFFFFFF);
                    mask.setRGB(x, y, 0xFF000000);
                    hasMask = true;
                } else {
                    base.setRGB(x, y, rgb & 0xFFFFFF);
                    mask.setRGB(x, y, 0x00000000);
                }
            }
        }

        return new BlackLayer(base, mask, hasMask);
    }

    private static class BlackLayer {
        private final BufferedImage base;
        private final BufferedImage mask;
        private final boolean hasMask;

        private BlackLayer(BufferedImage base, BufferedImage mask, boolean hasMask) {
            this.base = base;
            this.mask = mask;
            this.hasMask = hasMask;
        }
    }
}
