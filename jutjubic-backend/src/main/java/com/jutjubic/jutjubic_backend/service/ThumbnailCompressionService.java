package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ThumbnailCompressionService {

    private final VideoPostRepository postRepo;
    private final FileStorageService fileStorage;

    public ThumbnailCompressionService(VideoPostRepository postRepo, FileStorageService fileStorage) {
        this.postRepo = postRepo;
        this.fileStorage = fileStorage;
    }

    // ✅ Pokreće se svaki dan u 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void compressOldThumbnails() {
        System.out.println("🗜️ Starting daily thumbnail compression job...");

        // Datum pre 30 dana
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);

        // Pronađi sve postove starije od 30 dana koji NISU kompresovani
        List<VideoPost> oldPosts = postRepo.findByCreatedAtBeforeAndThumbnailCompressedFalse(oneMonthAgo);

        System.out.println("📊 Found " + oldPosts.size() + " thumbnails to compress");

        int successCount = 0;
        int failCount = 0;

        for (VideoPost post : oldPosts) {
            try {
                compressThumbnail(post);
                successCount++;
            } catch (Exception e) {
                System.err.println("❌ Failed to compress thumbnail for post ID: " + post.getId());
                e.printStackTrace();
                failCount++;
            }
        }

        System.out.println("✅ Compression job completed!");
        System.out.println("   Success: " + successCount);
        System.out.println("   Failed: " + failCount);
    }

    private void compressThumbnail(VideoPost post) throws Exception {
        String originalPath = post.getThumbnailPath();

        // Apsolutna putanja do originala
        Path originalFile = fileStorage.resolveAbsolute(originalPath);

        if (!originalFile.toFile().exists()) {
            System.err.println("⚠️ Original thumbnail not found: " + originalFile);
            return;
        }

        // Kreiraj ime za kompresovanu sliku: thumbnails/abc123.jpg → thumbnails/abc123_compressed.jpg
        String compressedPath = originalPath.replace(".", "_compressed.");
        Path compressedFile = fileStorage.resolveAbsolute(compressedPath);

        // Proveri da li već postoji
        if (compressedFile.toFile().exists()) {
            System.out.println("⏭️ Compressed version already exists, skipping: " + compressedPath);
            post.setCompressedThumbnailPath(compressedPath);
            post.setThumbnailCompressed(true);
            postRepo.save(post);
            return;
        }

        System.out.println("🗜️ Compressing: " + originalPath);
        System.out.println("   Output: " + compressedPath);

        // Original veličina
        long originalSize = originalFile.toFile().length();

        // Kompreuj sliku (quality 0.6 = 60% kvalitet, smanjuje veličinu)
        Thumbnails.of(originalFile.toFile())
                .scale(1.0)  // Zadrži istu rezoluciju
                .outputQuality(0.6)  // 60% kvalitet (možeš menjati 0.5-0.9)
                .toFile(compressedFile.toFile());

        // Nova veličina
        long compressedSize = compressedFile.toFile().length();
        long savedBytes = originalSize - compressedSize;
        double savedPercent = (savedBytes * 100.0) / originalSize;

        System.out.println("✅ Compressed successfully!");
        System.out.println("   Original: " + formatBytes(originalSize));
        System.out.println("   Compressed: " + formatBytes(compressedSize));
        System.out.println("   Saved: " + formatBytes(savedBytes) + " (" + String.format("%.1f", savedPercent) + "%)");

        // Ažuriraj bazu
        post.setCompressedThumbnailPath(compressedPath);
        post.setThumbnailCompressed(true);
        postRepo.save(post);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    // ✅ Manuelno pokretanje za testiranje
    public void runManually() {
        System.out.println("🔧 Manual compression triggered");
        compressOldThumbnails();
    }
}