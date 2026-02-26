package com.sufi.pancardresizer.dto;

import java.util.List;

public class UploadResponse {
    private String batchId;
    private List<UploadFileMeta> files;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public List<UploadFileMeta> getFiles() {
        return files;
    }

    public void setFiles(List<UploadFileMeta> files) {
        this.files = files;
    }
}
