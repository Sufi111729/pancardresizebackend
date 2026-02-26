package com.sufi.pancardresizer.scheduler;

import com.sufi.pancardresizer.service.StorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TempCleanupScheduler {
    private final StorageService storageService;

    public TempCleanupScheduler(StorageService storageService) {
        this.storageService = storageService;
    }

    @Scheduled(fixedDelayString = "#{${app.cleanup-minutes:30} * 60 * 1000}", initialDelayString = "300000")
    public void cleanup() {
        storageService.cleanupExpired();
    }
}
