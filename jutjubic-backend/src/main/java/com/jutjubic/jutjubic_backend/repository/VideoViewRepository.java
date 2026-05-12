package com.jutjubic.jutjubic_backend.repository;

import com.jutjubic.jutjubic_backend.model.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    /**
     * Grupisanje pregleda po video_post_id i danu za poslednjih 7 dana
     * Vraća: video_post_id, date, count
     */
    @Query(value = """
        SELECT 
            v.video_post_id as videoPostId,
            DATE(v.viewed_at) as viewDate,
            COUNT(*) as viewCount
        FROM video_views v
        WHERE v.viewed_at >= :startDate
        GROUP BY v.video_post_id, DATE(v.viewed_at)
        ORDER BY v.video_post_id, viewDate
    """, nativeQuery = true)
    List<ViewCountByDay> findViewsGroupedByDayForLast7Days(@Param("startDate") LocalDateTime startDate);

    interface ViewCountByDay {
        Long getVideoPostId();
        java.sql.Date getViewDate();
        Long getViewCount();
    }

    /**
     * Briše stare preglede starije od određenog datuma
     */
    void deleteByViewedAtBefore(LocalDateTime date);
}