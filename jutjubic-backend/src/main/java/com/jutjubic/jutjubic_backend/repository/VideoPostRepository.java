package com.jutjubic.jutjubic_backend.repository;

import com.jutjubic.jutjubic_backend.model.VideoPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface VideoPostRepository extends JpaRepository<VideoPost, Long> {

    List<VideoPost> findAllByOrderByCreatedAtDesc();

    List<VideoPost> findByAuthor_IdOrderByCreatedAtDesc(Long authorId);

    @EntityGraph(attributePaths = {"tags", "author"})
    Optional<VideoPost> findWithTagsById(Long id);

    @Query("SELECT v FROM VideoPost v JOIN v.author a WHERE a.username = :username ORDER BY v.createdAt DESC")
    List<VideoPost> findAllByAuthorUsername(@Param("username") String username);

    @Query(" SELECT v FROM VideoPost v WHERE v.location IS NOT NULL AND v.location.latitude BETWEEN :minLat AND :maxLat AND v.location.longitude BETWEEN :minLng AND :maxLng" )
    List<VideoPost> findInBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query("SELECT v FROM VideoPost v WHERE v.location IS NOT NULL " +
            "AND v.location.latitude BETWEEN :minLat AND :maxLat " +
            "AND v.location.longitude BETWEEN :minLng AND :maxLng " +
            "AND v.createdAt >= :fromDate " +
            "ORDER BY v.createdAt DESC")
    List<VideoPost> findInBoundingBoxAndFromDate(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
SELECT v FROM VideoPost v
WHERE (:fromDate IS NULL OR v.createdAt >= :fromDate)
ORDER BY v.createdAt DESC
""")
    List<VideoPost> findForMapByTime(@Param("fromDate") java.time.LocalDateTime fromDate);

    List<VideoPost> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(LocalDateTime fromDate);

    List<VideoPost> findByCreatedAtBeforeAndThumbnailCompressedFalse(LocalDateTime date);

}

