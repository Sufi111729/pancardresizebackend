package com.sufi.pancardresizer.dto;

public class RenderPhotoRequest {
    private String fileId;
    private int rotate;
    private CropRect crop;
    private int maxKb;

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

    public CropRect getCrop() {
        return crop;
    }

    public void setCrop(CropRect crop) {
        this.crop = crop;
    }

    public int getMaxKb() {
        return maxKb;
    }

    public void setMaxKb(int maxKb) {
        this.maxKb = maxKb;
    }
}
