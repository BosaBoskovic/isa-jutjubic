package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.uploads.root:uploads}")
    private String uploadsRoot;

    @Value("${app.uploads.timeout-ms:15000}")
    private long uploadTimeoutMs;

    private Path uploadsRootPath() {
        return Paths.get(System.getProperty("user.dir"))
                .resolve(uploadsRoot)
                .normalize();
    }


    public String saveWithTimeout(MultipartFile file, String subfolder, String extensionHint) {
        if (file == null || file.isEmpty()) {
            throw new ApiExcepiton("File is missing.");
        }

        Path target = null;

        try {
            String safeName = UUID.randomUUID() + (extensionHint != null ? extensionHint : "");

            Path root = Paths.get(System.getProperty("user.dir")).resolve(uploadsRoot).normalize();

            Path dir = root.resolve(subfolder);
            Files.createDirectories(dir);

            target = dir.resolve(safeName);

            long start = System.currentTimeMillis();

            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);

                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed > uploadTimeoutMs) {
                        throw new ApiExcepiton("Upload took too long (timeout).");
                    }
                }
            }

            return Paths.get(subfolder, safeName)
                    .toString()
                    .replace("\\", "/");

        } catch (ApiExcepiton e) {
            if (target != null) {
                try { Files.deleteIfExists(target); } catch (Exception ignored) {}
            }
            throw e;

        } catch (Exception e) {
            if (target != null) {
                try { Files.deleteIfExists(target); } catch (Exception ignored) {}
            }
            throw new ApiExcepiton("Failed to save file.");
        }
    }

    public Path resolveAbsolute(String storedRelativePath) {
        if (storedRelativePath == null || storedRelativePath.isBlank()) {
            throw new ApiExcepiton("File path is missing.");
        }

        Path p = Paths.get(storedRelativePath);
        if (p.isAbsolute()) return p.normalize();

        Path root = uploadsRootPath();
        return root.resolve(p).normalize();


    }

    public byte[] readBytes(String storedRelativePath) {
        Path p = resolveAbsolute(storedRelativePath);
        System.out.println("READ FILE ABS: " + p.toAbsolutePath());
        try {
            if (!Files.exists(p)) {
                throw new ApiExcepiton("File not found: " + p);
            }
            return Files.readAllBytes(p);
        } catch (ApiExcepiton e) {
            throw e;
        } catch (Exception e) {
            throw new ApiExcepiton("Cannot read file: " + p);
        }
    }

    public void deleteIfExists(String storedRelativePath) {
        if (storedRelativePath == null || storedRelativePath.isBlank()) return;

        try {
            Path p = resolveAbsolute(storedRelativePath);
            Files.deleteIfExists(p);
        } catch (Exception ignored) {}
    }
}
