package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HLSService {

    @Value("${app.uploads.root:uploads}")
    private String uploadsRoot;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.ffprobe.path:ffprobe}")
    private String ffprobePath;

    private Path uploadsRootPath() {
        return Paths.get(System.getProperty("user.dir"))
                .resolve(uploadsRoot)
                .normalize();
    }

    /**
     * Konvertuje video u HLS format sa segmentima od 4 sekunde
     * @param videoRelativePath relativna putanja originalnog videa
     * @return relativna putanja do .m3u8 playlist fajla
     */
    public String convertToHLS(String videoRelativePath) {
        log.info("=== convertToHLS START ===");
        log.info("Input video path: {}", videoRelativePath);

        try {
            Path videoPath = uploadsRootPath().resolve(videoRelativePath);
            log.info("Resolved absolute video path: {}", videoPath);

            if (!Files.exists(videoPath)) {
                log.error("Video file does NOT exist at path: {}", videoPath);
                throw new ApiExcepiton("Video file not found: " + videoPath);
            }

            log.info("Video file exists, size: {} bytes", Files.size(videoPath));

            // Kreiraj HLS direktorijum
            String hlsFolder = videoRelativePath.replace(".mp4", "_hls");
            Path hlsDir = uploadsRootPath().resolve(hlsFolder);
            log.info("Creating HLS directory: {}", hlsDir);
            Files.createDirectories(hlsDir);

            Path playlistPath = hlsDir.resolve("playlist.m3u8");
            Path segmentPattern = hlsDir.resolve("segment_%03d.ts");

            log.info("HLS playlist will be: {}", playlistPath);
            log.info("HLS segments pattern: {}", segmentPattern);
            log.info("FFmpeg path: {}", ffmpegPath);

            // Kreiraj FFmpeg proces
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", videoPath.toString(),
                    "-c:v", "copy",
                    "-c:a", "copy",
                    "-f", "hls",
                    "-hls_time", "4",
                    "-hls_list_size", "0",
                    "-hls_segment_filename", segmentPattern.toString(),
                    playlistPath.toString()
            );

            log.info("FFmpeg command: {}", String.join(" ", pb.command()));

            // Redirect stderr to capture FFmpeg output
            pb.redirectErrorStream(true);

            log.info("Starting FFmpeg process...");
            Process process = pb.start();

            // Read FFmpeg output
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            log.info("FFmpeg exit code: {}", exitCode);

            if (!output.isEmpty()) {
                log.info("FFmpeg output:\n{}", output);
            }

            if (exitCode != 0) {
                log.error("FFmpeg failed with exit code {}", exitCode);
                log.error("FFmpeg output:\n{}", output);
                throw new ApiExcepiton("FFmpeg exited with code " + exitCode);
            }

            // Verify playlist was created
            if (!Files.exists(playlistPath)) {
                log.error("Playlist file was NOT created at: {}", playlistPath);
                throw new ApiExcepiton("HLS playlist file was not created");
            }

            long playlistSize = Files.size(playlistPath);
            log.info("Playlist created successfully, size: {} bytes", playlistSize);

            // Count segments
            long segmentCount = Files.list(hlsDir)
                    .filter(p -> p.toString().endsWith(".ts"))
                    .count();
            log.info("Created {} HLS segments", segmentCount);

            String result = Paths.get(hlsFolder, "playlist.m3u8")
                    .toString()
                    .replace("\\", "/");

            log.info("=== convertToHLS SUCCESS ===");
            log.info("Returning playlist path: {}", result);
            return result;

        } catch (IOException | InterruptedException e) {
            log.error("=== convertToHLS FAILED ===");
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new ApiExcepiton("Failed to convert video to HLS: " + e.getMessage());
        }
    }

    /**
     * Dobija trajanje videa u sekundama
     */
    public int getVideoDuration(String videoRelativePath) {
        log.info("=== getVideoDuration START ===");
        log.info("Video path: {}", videoRelativePath);

        try {
            Path videoPath = uploadsRootPath().resolve(videoRelativePath);
            log.info("Resolved absolute path: {}", videoPath);

            if (!Files.exists(videoPath)) {
                log.error("Video file not found: {}", videoPath);
                return 0;
            }

            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath.toString()
            );

            log.info("FFprobe command: {}", String.join(" ", pb.command()));

            Process process = pb.start();
            String durationStr = new String(process.getInputStream().readAllBytes()).trim();

            // Also capture error output
            String errorOutput = new String(process.getErrorStream().readAllBytes()).trim();

            int exitCode = process.waitFor();

            log.info("FFprobe exit code: {}", exitCode);
            log.info("FFprobe output: '{}'", durationStr);

            if (!errorOutput.isEmpty()) {
                log.warn("FFprobe error output: {}", errorOutput);
            }

            if (exitCode != 0 || durationStr.isEmpty()) {
                log.error("FFprobe failed for file: {}", videoPath);
                return 0;
            }

            double duration = Double.parseDouble(durationStr);
            if (duration <= 0) {
                log.error("Invalid video duration: {}", duration);
                return 0;
            }

            int result = (int) Math.ceil(duration);
            log.info("=== getVideoDuration SUCCESS: {} seconds ===", result);
            return result;

        } catch (IOException | InterruptedException e) {
            log.error("=== getVideoDuration FAILED ===");
            log.error("Exception:", e);
            return 0;
        }
    }
}