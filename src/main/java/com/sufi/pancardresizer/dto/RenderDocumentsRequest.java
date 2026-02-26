package com.sufi.pancardresizer.dto;

import java.util.List;

public class RenderDocumentsRequest {
    private List<String> fileIds;
    private int maxKb;

    public List<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<String> fileIds) {
        this.fileIds = fileIds;
    }

    public int getMaxKb() {
        return maxKb;
    }

    public void setMaxKb(int maxKb) {
        this.maxKb = maxKb;
    }
}
