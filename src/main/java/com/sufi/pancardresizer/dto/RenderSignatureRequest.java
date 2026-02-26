package com.sufi.pancardresizer.dto;

public class RenderSignatureRequest {
    private String fileId;
    private int rotate;
    private CropRect crop;
    private boolean bw;

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

    public boolean isBw() {
        return bw;
    }

    public void setBw(boolean bw) {
        this.bw = bw;
    }
}
