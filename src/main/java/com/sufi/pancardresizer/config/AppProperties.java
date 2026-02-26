package com.sufi.pancardresizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String tempDir;
    private int cleanupMinutes;
    private String corsAllowedOrigins;
    private Upload upload = new Upload();

    public static class Upload {
        private long maxPerFileBytes;
        private long maxTotalBytes;

        public long getMaxPerFileBytes() {
            return maxPerFileBytes;
        }

        public void setMaxPerFileBytes(long maxPerFileBytes) {
            this.maxPerFileBytes = maxPerFileBytes;
        }

        public long getMaxTotalBytes() {
            return maxTotalBytes;
        }

        public void setMaxTotalBytes(long maxTotalBytes) {
            this.maxTotalBytes = maxTotalBytes;
        }
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public int getCleanupMinutes() {
        return cleanupMinutes;
    }

    public void setCleanupMinutes(int cleanupMinutes) {
        this.cleanupMinutes = cleanupMinutes;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }
}
