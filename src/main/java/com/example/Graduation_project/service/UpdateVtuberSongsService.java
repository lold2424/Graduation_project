package com.example.Graduation_project.service;

import com.example.Graduation_project.entity.VtuberEntity;
import com.example.Graduation_project.entity.VtuberSongsEntity;
import com.example.Graduation_project.repository.VtuberRepository;
import com.example.Graduation_project.repository.VtuberSongsRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class UpdateVtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final List<String> apiKeys;
    private final List<Boolean> keyUsage;
    private int currentKeyIndex = 0;
    private static final Logger logger = Logger.getLogger(UpdateVtuberSongsService.class.getName());

    private static final List<String> SONG_KEYWORDS = List.of("music", "song", "cover", "original", "official", "mv", "뮤직", "노래", "커버");

    public UpdateVtuberSongsService(
            YouTube youTube,
            VtuberRepository vtuberRepository,
            VtuberSongsRepository vtuberSongsRepository,
            @Value("${youtube.api.keys}") List<String> apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.apiKeys = new ArrayList<>(apiKeys);
        this.keyUsage = new ArrayList<>(apiKeys.size());
        for (int i = 0; i < apiKeys.size(); i++) {
            this.keyUsage.add(true);
        }
    }

    @Scheduled(cron = "0 5 0 * * ?", zone = "Asia/Seoul")
    public void fetchVtuberSongs() {
        List<VtuberEntity> newVtubers = vtuberRepository.findByStatus("new");
        List<VtuberEntity> existingVtubers = vtuberRepository.findByStatus("existing");

        for (VtuberEntity vtuber : newVtubers) {
            fetchAllSongsFromPlaylist(vtuber.getChannelId(), vtuber.getName());
            vtuber.setStatus("existing");
            vtuberRepository.save(vtuber);
        }

        Instant threeDaysAgoInstant = LocalDateTime.now().minusDays(3).toInstant(ZoneOffset.UTC);
        for (VtuberEntity vtuber : existingVtubers) {
            fetchRecentSongsFromSearch(vtuber.getChannelId(), vtuber.getName(), threeDaysAgoInstant);
        }
    }

    @Scheduled(cron = "30 2 0 * * MON", zone = "Asia/Seoul")
    public void updateSongStatusToExisting() {
        List<VtuberSongsEntity> newSongs = vtuberSongsRepository.findByStatus("new");

        newSongs.forEach(song -> {
            song.setStatus("existing");
            song.setUpdateDayTime(LocalDateTime.now()); // 상태 변경 시간을 기록
            vtuberSongsRepository.save(song);
        });

        logger.info("Updated " + newSongs.size() + " songs from 'new' to 'existing'.");
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void updateViewCounts() {
        resetKeyUsage(); // API 키 사용 상태를 리셋
        List<VtuberSongsEntity> songs = vtuberSongsRepository.findAll();
        for (VtuberSongsEntity song : songs) {
            long newViewCount = fetchViewCount(song.getVideoId());

            if (newViewCount == 0) {
                logger.info("삭제된 동영상 감지: " + song.getTitle() + " (" + song.getVideoId() + ")");
                vtuberSongsRepository.delete(song);
                continue;
            }

            if (newViewCount > 0) {
                Long currentViewCount = (song.getViewCount() != null) ? song.getViewCount() : 0L;
                long viewIncreaseDay = newViewCount - currentViewCount;
                song.setViewCount(newViewCount);
                song.setViewsIncreaseDay(viewIncreaseDay);
                song.setUpdateDayTime(LocalDateTime.now());

                if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                    Long lastWeekViewCount = (song.getLastWeekViewCount() != null) ? song.getLastWeekViewCount() : 0L;
                    long viewIncreaseWeek = newViewCount - lastWeekViewCount;
                    song.setViewsIncreaseWeek(viewIncreaseWeek);
                    song.setLastWeekViewCount(newViewCount);
                    song.setUpdateWeekTime(LocalDateTime.now());
                }

                vtuberSongsRepository.save(song); // DB에 업데이트
            }
        }
        logger.info("조회수 업데이트 및 삭제된 동영상 제거 완료");
    }

    private void fetchAllSongsFromPlaylist(String channelId, String channelName) {
        logger.info("채널 [" + channelName + "]에서 모든 노래를 가져옵니다. 채널 ID: " + channelId);
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(channelId);
            if (uploadsPlaylistId == null) {
                logger.severe("Uploads playlist not found for channel ID: " + channelId);
                return;
            }
            logger.info("업로드 플레이리스트 ID: " + uploadsPlaylistId);

            String pageToken = null;
            do {
                logger.info("플레이리스트 페이지 조회 중...");
                YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems()
                        .list("contentDetails,snippet");
                playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
                playlistItemsRequest.setMaxResults(50L);
                playlistItemsRequest.setPageToken(pageToken);
                playlistItemsRequest.setKey(apiKeys.get(currentKeyIndex));

                PlaylistItemListResponse playlistItemResult = playlistItemsRequest.execute();
                List<PlaylistItem> playlistItems = playlistItemResult.getItems();

                logger.info("조회된 동영상 수: " + playlistItems.size());

                List<String> videoIds = new ArrayList<>();
                for (PlaylistItem item : playlistItems) {
                    videoIds.add(item.getContentDetails().getVideoId());
                }

                fetchAndProcessVideos(videoIds, channelName);

                pageToken = playlistItemResult.getNextPageToken();
            } while (pageToken != null);
            logger.info("플레이리스트 노래 조회 완료. 채널: " + channelName);
        } catch (GoogleJsonResponseException e) {
            handleApiException(e, channelName, channelId);
        } catch (IOException e) {
            logger.severe("IOException while fetching playlist items for channel ID: " + channelId + " - " + e.getMessage());
        }
    }

    private String getUploadsPlaylistId(String channelId) throws IOException {
        YouTube.Channels.List channelRequest = youTube.channels().list("contentDetails");
        channelRequest.setId(channelId);
        channelRequest.setKey(apiKeys.get(currentKeyIndex));

        ChannelListResponse channelResult = channelRequest.execute();
        List<Channel> channelsList = channelResult.getItems();
        if (channelsList != null && !channelsList.isEmpty()) {
            return channelsList.get(0).getContentDetails().getRelatedPlaylists().getUploads();
        }
        return null;
    }

    private void fetchAndProcessVideos(List<String> videoIds, String channelName) {
        logger.info("동영상 상세 정보를 가져옵니다. 비디오 ID 수: " + videoIds.size());
        try {
            YouTube.Videos.List videosRequest = youTube.videos().list("id,snippet,contentDetails,statistics");
            videosRequest.setId(String.join(",", videoIds));
            videosRequest.setKey(apiKeys.get(currentKeyIndex));

            VideoListResponse videoResponse = videosRequest.execute();
            List<Video> videos = videoResponse.getItems();

            for (Video video : videos) {
                String videoId = video.getId();
                String title = video.getSnippet().getTitle();
                String lowerTitle = title.toLowerCase();

                if (isSongRelated(lowerTitle)) {
                    logger.info("노래로 판단된 동영상: " + title + " (" + videoId + ")");
                    Optional<VtuberSongsEntity> existingSongOpt = vtuberSongsRepository.findByVideoId(videoId);
                    if (!existingSongOpt.isPresent()) {
                        String classification = determineClassification(video);
                        if ("ignore".equals(classification)) {
                            logger.info("무시된 동영상 (길이 초과): " + title);
                            continue;
                        }
                        saveNewSong(video, channelName, classification);
                    } else {
                        logger.info("이미 존재하는 노래: " + title);
                    }
                } else {
                    logger.info("노래와 관련 없는 동영상 필터링: " + title);
                }
            }
        } catch (IOException e) {
            logger.severe("IOException while fetching video details - " + e.getMessage());
            switchApiKey();
        }
    }

    private String determineClassification(Video video) {
        String title = video.getSnippet().getTitle();
        String durationStr = video.getContentDetails().getDuration();
        Duration videoDuration = Duration.parse(durationStr);

        if (videoDuration.compareTo(Duration.ofMinutes(8)) > 0) {
            return "ignore";
        }
        if (videoDuration.compareTo(Duration.ofMinutes(1)) <= 0 || title.toLowerCase().contains("short")) {
            return "shorts";
        }
        return "videos";
    }

    private void saveNewSong(Video video, String channelName, String classification) {
        logger.info("새 노래 저장: " + video.getSnippet().getTitle() + " | 분류: " + classification);
        VtuberSongsEntity song = new VtuberSongsEntity();
        song.setChannelId(video.getSnippet().getChannelId());
        song.setVideoId(video.getId());
        song.setTitle(video.getSnippet().getTitle());
        song.setPublishedAt(Instant.ofEpochMilli(video.getSnippet().getPublishedAt().getValue())
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
        song.setAddedTime(LocalDateTime.now());
        song.setUpdateDayTime(LocalDateTime.now());
        song.setUpdateWeekTime(LocalDateTime.now());
        song.setVtuberName(channelName);
        song.setViewCount(video.getStatistics().getViewCount().longValue());
        song.setViewsIncreaseDay(0L);
        song.setViewsIncreaseWeek(0L);
        song.setLastWeekViewCount(0L);
        song.setStatus("new");
        song.setClassification(classification);

        vtuberSongsRepository.save(song);
        logger.info("노래 저장 완료: " + song.getTitle());
    }

    private void fetchRecentSongsFromSearch(String channelId, String channelName, Instant publishedAfterInstant) {
        logger.info("채널 [" + channelName + "]에서 최근 노래를 검색합니다. 채널 ID: " + channelId);
        String combinedQuery = "music|cover|original|official";
        String pageToken = null;

        do {
            try {
                logger.info("노래 검색 중... publishedAfter: " + publishedAfterInstant);
                YouTube.Search.List search = youTube.search().list("id,snippet");
                search.setChannelId(channelId);
                search.setQ(combinedQuery);
                search.setType("video");
                search.setOrder("date");
                search.setFields("nextPageToken,items(id/videoId,snippet/title,snippet/publishedAt,snippet/channelId)");
                search.setMaxResults(50L);
                search.setKey(apiKeys.get(currentKeyIndex));
                search.setPageToken(pageToken);
                search.setPublishedAfter(new com.google.api.client.util.DateTime(publishedAfterInstant.toEpochMilli()));

                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResults = searchResponse.getItems();

                logger.info("검색된 동영상 수: " + searchResults.size());
                handleSearchResults(searchResults, channelName);

                pageToken = searchResponse.getNextPageToken();
            } catch (IOException e) {
                logger.severe("API 호출 중 오류 발생 - 채널명: " + channelName + ", 채널 ID: " + channelId + " - 오류 메시지: " + e.getMessage());
                keyUsage.set(currentKeyIndex, false);
                switchApiKey();
            }
        } while (pageToken != null);
        logger.info("최근 노래 검색 완료. 채널: " + channelName);
    }

    private void handleSearchResults(List<SearchResult> searchResults, String channelName) {
        List<String> videoIds = new ArrayList<>();
        for (SearchResult result : searchResults) {
            videoIds.add(result.getId().getVideoId());
        }

        try {
            YouTube.Videos.List videosRequest = youTube.videos().list("id,snippet,contentDetails,statistics");
            videosRequest.setId(String.join(",", videoIds));
            videosRequest.setKey(apiKeys.get(currentKeyIndex));

            VideoListResponse videoResponse = videosRequest.execute();
            List<Video> videos = videoResponse.getItems();

            for (Video video : videos) {
                String videoId = video.getId();
                String title = video.getSnippet().getTitle();
                String lowerTitle = title.toLowerCase();

                if (isSongRelated(lowerTitle)) {
                    Optional<VtuberSongsEntity> existingSongOpt = vtuberSongsRepository.findByVideoId(videoId);
                    if (!existingSongOpt.isPresent()) {
                        // Determine classification
                        String classification = determineClassification(video);
                        if ("ignore".equals(classification)) {
                            continue;
                        }
                        saveNewSong(video, channelName, classification);
                    }
                } else {
                    logger.info("노래와 관련 없는 동영상 필터링: " + title);
                }
            }
        } catch (IOException e) {
            logger.severe("Error fetching video details: " + e.getMessage());
            switchApiKey();
        }
    }

    // 제목에 노래 관련 키워드가 포함되어 있는지 확인하는 함수
    private boolean isSongRelated(String title) {
        return SONG_KEYWORDS.stream().anyMatch(title::contains);
    }

    private long fetchViewCount(String videoId) {
        try {
            YouTube.Videos.List request = youTube.videos().list("statistics");
            request.setId(videoId);
            request.setKey(apiKeys.get(currentKeyIndex));
            var response = request.execute();
            if (!response.getItems().isEmpty()) {
                return response.getItems().get(0).getStatistics().getViewCount().longValue();
            }
        } catch (IOException e) {
            logger.severe("Failed to fetch view counts: " + e.getMessage());
            switchApiKey();
        }
        return 0;
    }

    private void switchApiKey() {
        int initialKeyIndex = currentKeyIndex;
        do {
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
            if (keyUsage.get(currentKeyIndex)) {
                logger.info("API 키 전환: " + apiKeys.get(currentKeyIndex));
                return;
            }
        } while (currentKeyIndex != initialKeyIndex);

        logger.severe("모든 API 키의 할당량이 소진됨. 작업을 중단합니다.");
        throw new RuntimeException("모든 API 키의 할당량이 소진되었습니다.");
    }

    private void resetKeyUsage() {
        for (int i = 0; i < keyUsage.size(); i++) {
            keyUsage.set(i, true);
        }
    }

    private void handleApiException(GoogleJsonResponseException e, String channelName, String channelId) {
        logger.severe("API 호출 중 오류 발생 - 채널명: " + channelName + ", 채널 ID: " + channelId + " - 오류 메시지: " + e.getMessage());
        keyUsage.set(currentKeyIndex, false);
        switchApiKey();
    }
}
