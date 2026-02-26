package com.sufi.pancardresizer.dto;

public class PreviewRequest {
    private String fileId;
    private int rotate;
    private double zoom;
    private CropRect crop;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public CropRect getCrop() {
        return crop;
    }

    public void setCrop(CropRect crop) {
        this.crop = crop;
    }
}
