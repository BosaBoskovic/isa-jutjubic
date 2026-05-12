package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.dto.PopularVideoDto;
import com.jutjubic.jutjubic_backend.model.PopularVideo;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.PopularVideoRepository;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.repository.VideoViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularVideoService {

    private final VideoViewRepository videoViewRepository;
    private final VideoPostRepository videoPostRepository;
    private final PopularVideoRepository popularVideoRepository;


    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void runDailyETLPipeline() {
        log.info("=== STARTING DAILY ETL PIPELINE ===");
        LocalDateTime pipelineRunAt = LocalDateTime.now();

        try {
            // EXTRACT
            log.info("EXTRACT: Fetching view data from last 7 days...");
            Map<Long, Map<LocalDate, Long>> viewsByVideoAndDay = extractViewData();
            log.info("EXTRACT: Found data for {} videos", viewsByVideoAndDay.size());

            // TRANSFORM
            log.info("TRANSFORM: Calculating popularity scores...");
            List<VideoPopularityScore> scores = transformToPopularityScores(viewsByVideoAndDay);
            log.info("TRANSFORM: Calculated scores for {} videos", scores.size());

            // LOAD
            log.info("LOAD: Saving top 3 popular videos...");
            loadTopVideos(scores, pipelineRunAt);
            log.info("LOAD: Successfully saved top 3 videos");

            cleanupOldData();

            log.info("ETL PIPELINE COMPLETED SUCCESSFULLY ");

        } catch (Exception e) {
            log.error("ETL PIPELINE FAILED ", e);
            throw e;
        }
    }

    private Map<Long, Map<LocalDate, Long>> extractViewData() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<VideoViewRepository.ViewCountByDay> rawData =
                videoViewRepository.findViewsGroupedByDayForLast7Days(sevenDaysAgo);

        Map<Long, Map<LocalDate, Long>> result = new HashMap<>();

        for (VideoViewRepository.ViewCountByDay record : rawData) {
            Long videoId = record.getVideoPostId();
            LocalDate date = record.getViewDate().toLocalDate();
            Long count = record.getViewCount();

            result.computeIfAbsent(videoId, k -> new HashMap<>())
                    .put(date, count);
        }

        return result;
    }


    private List<VideoPopularityScore> transformToPopularityScores(
            Map<Long, Map<LocalDate, Long>> viewsByVideoAndDay) {

        LocalDate today = LocalDate.now();
        List<VideoPopularityScore> scores = new ArrayList<>();

        for (Map.Entry<Long, Map<LocalDate, Long>> entry : viewsByVideoAndDay.entrySet()) {
            Long videoId = entry.getKey();
            Map<LocalDate, Long> viewsByDay = entry.getValue();

            double totalScore = 0.0;
            long totalViews = 0;


            for (int daysAgo = 0; daysAgo < 7; daysAgo++) {
                LocalDate date = today.minusDays(daysAgo);
                Long viewCount = viewsByDay.getOrDefault(date, 0L);

                int weight = 7 - daysAgo;

                totalScore += viewCount * weight;
                totalViews += viewCount;

                if (viewCount > 0) {
                    log.debug("Video {}: {} views on {} (weight {})",
                            videoId, viewCount, date, weight);
                }
            }

            if (totalScore > 0) {
                scores.add(new VideoPopularityScore(videoId, totalScore, totalViews));
            }
        }

        scores.sort(Comparator.comparing(VideoPopularityScore::score).reversed());

        return scores;
    }

    private void loadTopVideos(List<VideoPopularityScore> scores, LocalDateTime pipelineRunAt) {
        if (scores.isEmpty()) {
            log.warn("No videos with views found, skipping LOAD phase");
            return;
        }


        List<VideoPopularityScore> top3 = scores.stream()
                .limit(3)
                .toList();

        log.info("Top 3 popular videos:");
        for (int i = 0; i < top3.size(); i++) {
            VideoPopularityScore score = top3.get(i);
            log.info("  {}. Video ID {} - Score: {}, Views: {}",
                    i + 1, score.videoId(), score.score(), score.totalViews());
        }


        for (int rank = 0; rank < top3.size(); rank++) {
            VideoPopularityScore score = top3.get(rank);

            VideoPost videoPost = videoPostRepository.findById(score.videoId())
                    .orElse(null);

            if (videoPost == null) {
                log.warn("Video ID {} not found in database, skipping", score.videoId());
                continue;
            }

            PopularVideo popularVideo = new PopularVideo();
            popularVideo.setPipelineRunAt(pipelineRunAt);
            popularVideo.setVideoPost(videoPost);
            popularVideo.setPopularityRank(rank + 1); // 1, 2, 3
            popularVideo.setPopularityScore(score.score());
            popularVideo.setViewsLast7Days(score.totalViews());

            popularVideoRepository.save(popularVideo);
        }
    }


    private void cleanupOldData() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        log.info("CLEANUP: Deleting video views older than 30 days...");
        videoViewRepository.deleteByViewedAtBefore(thirtyDaysAgo);

        log.info("CLEANUP: Deleting old popular video records older than 30 days...");
        popularVideoRepository.deleteByPipelineRunAtBefore(thirtyDaysAgo);
    }

    private record VideoPopularityScore(
            Long videoId,
            Double score,
            Long totalViews
    ) {}

    @Transactional(readOnly = true)
    public List<PopularVideoDto> getTop3PopularVideos() {
        log.info("Fetching top 3 popular videos from latest ETL run");

        List<PopularVideo> popularVideos = popularVideoRepository.findTop3FromLatestRun();

        log.info("Found {} popular videos", popularVideos.size());

        return popularVideos.stream()
                .map(PopularVideoDto::from)
                .collect(Collectors.toList());
    }
}