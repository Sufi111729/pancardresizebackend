package com.sufi.pancardresizer.util;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public final class ImageUtils {
    private ImageUtils() {
    }

    public static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    public static BufferedImage rotate(BufferedImage src, int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        if (normalized == 0) {
            return src;
        }
        double rads = Math.toRadians(normalized);
        double sin = Math.abs(Math.sin(rads));
        double cos = Math.abs(Math.cos(rads));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.translate((newW - w) / 2.0, (newH - h) / 2.0);
        g2d.rotate(rads, w / 2.0, h / 2.0);
        g2d.drawRenderedImage(src, null);
        g2d.dispose();
        return rotated;
    }

    public static BufferedImage crop(BufferedImage src, int x, int y, int width, int height) {
        int safeX = Math.max(0, x);
        int safeY = Math.max(0, y);
        int safeW = Math.min(width, src.getWidth() - safeX);
        int safeH = Math.min(height, src.getHeight() - safeY);
        if (safeW <= 0 || safeH <= 0) {
            return src;
        }
        return src.getSubimage(safeX, safeY, safeW, safeH);
    }

    public static BufferedImage resize(BufferedImage src, int targetW, int targetH) throws IOException {
        return Thumbnails.of(src)
            .size(targetW, targetH)
            .keepAspectRatio(false)
            .asBufferedImage();
    }

    public static BufferedImage resizeKeepAspect(BufferedImage src, int maxW, int maxH) throws IOException {
        return Thumbnails.of(src)
            .size(maxW, maxH)
            .keepAspectRatio(true)
            .asBufferedImage();
    }

    public static BufferedImage zoom(BufferedImage src, double zoom) throws IOException {
        if (zoom <= 0.05 || zoom == 1.0) {
            return src;
        }
        int w = (int) Math.max(1, Math.round(src.getWidth() * zoom));
        int h = (int) Math.max(1, Math.round(src.getHeight() * zoom));
        return Thumbnails.of(src).size(w, h).keepAspectRatio(false).asBufferedImage();
    }

    public static BufferedImage toBlackAndWhite(BufferedImage src) {
        BufferedImage bw = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int val = lum < 160 ? 0 : 255;
                int bwRgb = (val << 16) | (val << 8) | val;
                bw.setRGB(x, y, bwRgb);
            }
        }
        return bw;
    }

    public static BufferedImage trimWhiteBorders(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        int threshold = 245;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (r < threshold || g < threshold || b < threshold) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return src;
        }

        int cropW = Math.max(1, maxX - minX + 1);
        int cropH = Math.max(1, maxY - minY + 1);
        return src.getSubimage(minX, minY, cropW, cropH);
    }

    public static byte[] writeJpegWithQualityAndDpi(BufferedImage image, float quality, int dpi) throws IOException {
        BufferedImage rgb = toRgb(image);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB), param);
        setDpi(metadata, dpi);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(metadata, new IIOImage(rgb, null, metadata), param);
            writer.dispose();
            return baos.toByteArray();
        }
    }

    public static byte[] padJpegToSize(byte[] data, int targetBytes) {
        if (data == null || data.length >= targetBytes) {
            return data;
        }
        return Arrays.copyOf(data, targetBytes);
    }

    private static void setDpi(IIOMetadata metadata, int dpi) throws IOException {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
        IIOMetadataNode jfif = (IIOMetadataNode) root.getElementsByTagName("app0JFIF").item(0);
        jfif.setAttribute("resUnits", "1");
        jfif.setAttribute("Xdensity", Integer.toString(dpi));
        jfif.setAttribute("Ydensity", Integer.toString(dpi));
        metadata.setFromTree("javax_imageio_jpeg_image_1.0", root);
    }
}
