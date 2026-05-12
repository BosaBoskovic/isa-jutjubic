package com.jutjubic.jutjubic_backend.repository;

import com.jutjubic.jutjubic_backend.model.PopularVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PopularVideoRepository extends JpaRepository<PopularVideo, Long> {


    @Query("""
        SELECT pv FROM PopularVideo pv 
        WHERE pv.pipelineRunAt = (
            SELECT MAX(pv2.pipelineRunAt) FROM PopularVideo pv2
        )
        ORDER BY pv.popularityRank ASC
    """)
    List<PopularVideo> findTop3FromLatestRun();


    void deleteByPipelineRunAtBefore(LocalDateTime date);
}