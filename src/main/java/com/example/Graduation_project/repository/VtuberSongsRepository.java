package com.example.Graduation_project.repository;

import com.example.Graduation_project.entity.VtuberSongsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VtuberSongsRepository extends JpaRepository<VtuberSongsEntity, Long> {
    Optional<VtuberSongsEntity> findByVideoId(String videoId);

    List<VtuberSongsEntity> findByStatus(String status);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'videos' ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<VtuberSongsEntity> findRandomVideoSongs(int limit);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'shorts' ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<VtuberSongsEntity> findRandomShortsSongs(int limit);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'videos' AND channel_id IN (:channelIds) ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<VtuberSongsEntity> findRandomVideoSongsByChannelIds(@Param("channelIds") List<String> channelIds, @Param("limit") int limit);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'shorts' AND channel_id IN (:channelIds) ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<VtuberSongsEntity> findRandomShortsSongsByChannelIds(@Param("channelIds") List<String> channelIds, @Param("limit") int limit);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'videos' AND channel_id IN (:channelIds) ORDER BY views_increase_week DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByViewsIncreaseWeekDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'videos' AND channel_id IN (:channelIds) ORDER BY views_increase_day DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByViewsIncreaseDayDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'videos' AND channel_id IN (:channelIds) ORDER BY published_at DESC LIMIT 9", nativeQuery = true)
    List<VtuberSongsEntity> findTop9ByPublishedAtDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'shorts' AND channel_id IN (:channelIds) ORDER BY views_increase_week DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ShortsByViewsIncreaseWeekDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'shorts' AND channel_id IN (:channelIds) ORDER BY published_at DESC LIMIT 9", nativeQuery = true)
    List<VtuberSongsEntity> findTop9ShortsByPublishedAtDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE title LIKE %:title% AND classification = :classification ORDER BY view_count DESC", nativeQuery = true)
    List<VtuberSongsEntity> findAllByTitleContainingAndClassificationOrderByViewCountDesc(@Param("title") String title, @Param("classification") String classification);

    @Query(value = "SELECT * FROM vtuber_songs WHERE vtuber_name = :vtuberName ORDER BY view_count DESC", nativeQuery = true)
    List<VtuberSongsEntity> findAllByVtuberNameOrderByViewCountDesc(@Param("vtuberName") String vtuberName);

    @Query("SELECT COUNT(v) FROM VtuberSongsEntity v WHERE v.channelId = :channelId")
    int countByChannelId(@Param("channelId") String channelId);

    List<VtuberSongsEntity> findByChannelId(String channelId);
}
