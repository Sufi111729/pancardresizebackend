package com.sufi.pancardresizer.controller;

import com.sufi.pancardresizer.dto.*;
import com.sufi.pancardresizer.model.StoredFile;
import com.sufi.pancardresizer.service.DocumentService;
import com.sufi.pancardresizer.service.ImageService;
import com.sufi.pancardresizer.service.PreviewService;
import com.sufi.pancardresizer.service.StorageService;
import com.sufi.pancardresizer.exception.AppException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PanController {
    private final StorageService storageService;
    private final PreviewService previewService;
    private final ImageService imageService;
    private final DocumentService documentService;

    public PanController(StorageService storageService, PreviewService previewService, ImageService imageService, DocumentService documentService) {
        this.storageService = storageService;
        this.previewService = previewService;
        this.imageService = imageService;
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestPart("files") MultipartFile[] files) {
        List<StoredFile> stored = storageService.storeFiles(files);
        UploadResponse response = new UploadResponse();
        response.setBatchId(UUID.randomUUID().toString());
        response.setFiles(stored.stream().map(this::toMeta).collect(Collectors.toList()));
        return response;
    }

    @PostMapping(value = "/preview", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> preview(@RequestBody PreviewRequest request) {
        byte[] data = previewService.buildPreview(request);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(data);
    }

    @PostMapping(value = "/render/photo", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> renderPhoto(@RequestBody RenderPhotoRequest request) {
        byte[] data = imageService.renderPhoto(request);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pan-photo.jpg")
            .body(data);
    }

    @PostMapping(value = "/render/photo-kb", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> renderPhotoKb(@RequestBody RenderPhotoRequest request) {
        byte[] data = imageService.renderPhotoByKb(request);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pan-photo-kb.jpg")
            .body(data);
    }

    @PostMapping(value = "/kb/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadKb(@RequestPart("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new AppException("Image is required", "image_required");
        }
        if (files.length > 1) {
            throw new AppException("KB editor accepts one image at a time", "single_image_required");
        }
        MultipartFile file = files[0];
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean isImage = contentType.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
        if (!isImage) {
            throw new AppException("Only JPG/PNG images are allowed for KB editor", "image_required");
        }
        return upload(files);
    }

    @PostMapping(value = "/kb/preview", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> previewKb(@RequestBody PreviewRequest request) {
        return preview(request);
    }

    @PostMapping(value = "/kb/render/photo", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> renderKbPhoto(@RequestBody RenderPhotoRequest request) {
        return renderPhotoKb(request);
    }

    @PostMapping(value = "/kb/size/photo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KbSizeResponse> sizeKbPhoto(@RequestBody RenderPhotoRequest request) {
        ImageService.SizeResult result = imageService.getKbSize(request);
        return ResponseEntity.ok(new KbSizeResponse(result.getSizeBytes(), result.isExact()));
    }

    @PostMapping(value = "/render/signature", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> renderSignature(@RequestBody RenderSignatureRequest request) {
        byte[] data = imageService.renderSignature(request);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pan-signature.jpg")
            .body(data);
    }

    @PostMapping(value = "/render/documents", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> renderDocuments(@RequestBody RenderDocumentsRequest request) {
        byte[] data = documentService.renderDocuments(request.getFileIds());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pan-documents.pdf")
            .body(data);
    }

    @PostMapping(value = "/render/document-image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> renderDocumentImage(@RequestBody RenderDocumentsRequest request) {
        byte[] data = documentService.renderDocumentImage(request.getFileIds());
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pan-document.jpg")
            .body(data);
    }

    @PostMapping(value = "/pdf/render/documents", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> renderDocumentsByKb(@RequestBody RenderDocumentsRequest request) {
        byte[] data = documentService.renderDocumentsByKb(request.getFileIds(), request.getMaxKb());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pan-documents.pdf")
            .body(data);
    }

    private UploadFileMeta toMeta(StoredFile stored) {
        UploadFileMeta meta = new UploadFileMeta();
        meta.setFileId(stored.getFileId());
        meta.setOriginalName(stored.getOriginalName());
        meta.setSizeBytes(stored.getSizeBytes());
        meta.setWidth(stored.getWidth());
        meta.setHeight(stored.getHeight());
        meta.setFormat(stored.getFormat());
        meta.setContentType(stored.getContentType());
        return meta;
    }
}
