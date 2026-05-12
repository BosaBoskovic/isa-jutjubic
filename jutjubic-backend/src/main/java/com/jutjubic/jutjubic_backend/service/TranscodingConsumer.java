package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.config.RabbitConfig;
import com.jutjubic.jutjubic_backend.dto.TranscodingRequest;
import org.apache.catalina.connector.InputBuffer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component
public class TranscodingConsumer {

    @Value("${app.uploads.root}")
    private String uploadsRoot; // Uzima "uploads" iz properties

    @RabbitListener(
            queues = RabbitConfig.QUEUE,
            concurrency = "2"
    )
    public void handle(TranscodingRequest request) {
        System.out.println("📥 Processing video ID: " + request.getVideoId());
        boolean success = runFfmpeg(request);

        if (success) {
            generateHLSSegments(request);
        }
    }

    private boolean runFfmpeg(TranscodingRequest request) {
        // relativePath je npr. "videos/abc123.mp4"
        String relativePath = request.getVideoPath();

        // Puna putanja: uploads/videos/abc123.mp4
        String inputPath = uploadsRoot + "/" + relativePath;
        String outputPath = inputPath.replace(".mp4", "_720p.mp4");

        System.out.println("🎬 Starting transcoding:");
        System.out.println("   Input:  " + inputPath);
        System.out.println("   Output: " + outputPath);

        // Proveri da li fajl postoji
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            System.err.println(" ERROR: Video file not found at: " + inputPath);
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputPath,
                    "-vf", "scale=1280:720",
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-y", // overwrite output
                    outputPath
            );

            pb.inheritIO(); // Prikazuje FFmpeg output u konzoli

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println(" Transcoding SUCCESS for video ID: " + request.getVideoId());
                System.out.println("   Output saved: " + outputPath);
                return true;
            } else {
                System.err.println(" FFmpeg failed with exit code: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println(" Transcoding error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void generateHLSSegments(TranscodingRequest request) {
        try {
            String relativePath = request.getVideoPath();
            String inputPath = uploadsRoot + "/" + relativePath.replace(".mp4", "_720p.mp4");
            String outputDir = uploadsRoot + "/hls/" + request.getVideoId() + "/";

            // Kreiraj HLS direktorijum
            File hlsDir = new File(outputDir);
            if (!hlsDir.exists()) {
                hlsDir.mkdirs();
            }

            String outputPlaylist = outputDir + "playlist.m3u8";

            System.out.println(" Starting HLS segmentation:");
            System.out.println("   Input:  " + inputPath);
            System.out.println("   Output: " + outputPlaylist);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputPath,
                    "-codec:", "copy",
                    "-start_number", "0",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-f", "hls",
                    outputPlaylist
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader (process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(" HLS: " + line);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("HLS segments created for video ID: " + request.getVideoId());
                System.out.println("   Playlist: " + outputPlaylist);
            } else {
                System.err.println(" HLS generation failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            System.err.println(" HLS generation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}