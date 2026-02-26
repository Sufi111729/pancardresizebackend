package com.sufi.pancardresizer.dto;

public class KbSizeResponse {
    private long sizeBytes;
    private boolean exact;

    public KbSizeResponse() {
    }

    public KbSizeResponse(long sizeBytes, boolean exact) {
        this.sizeBytes = sizeBytes;
        this.exact = exact;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean isExact() {
        return exact;
    }

    public void setExact(boolean exact) {
        this.exact = exact;
    }

    public double getSizeKb() {
        return sizeBytes / 1024.0;
    }
}
