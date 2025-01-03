package com.example.Graduation_project.service;

import com.example.Graduation_project.entity.VtuberEntity;
import com.example.Graduation_project.repository.VtuberRepository;
import com.example.Graduation_project.repository.ExceptVtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class UploadVtuberService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;
    private final List<String> queries = Arrays.asList(
            "버튜버", "Vtuber", "버츄얼 유튜버", "버츄버", "v-youtuber", "스텔라이브", "V-LUP", "이세계아이돌", "VRECORD",
            "일루전 라이브", "버츄얼 헤르츠", "싸이코드", "PLAVE", "츠라이컴퍼니", "러브다이아", "스타데이즈", "에스더",
            "라이브루리", "HeartSoniC"
    );
    private static final Logger logger = Logger.getLogger(UploadVtuberService.class.getName());
    private static final int MIN_SUBSCRIBERS = 3000;
    private static final int MAX_PAGES_PER_QUERY = 3;
    private static final int MAX_QUOTA_PER_KEY = 10000; // 예시 할당량 제한

    private final List<String> excludeKeywordsInTitle = Arrays.asList(
            "응원", "통계", "번역", "다시보기", "게임", "저장", "일상", "브이로그", "보관", "잼민",
            "TV", "코인", "주식", "Tj", "tv", "팬계정", "창고", "박스", "팬", "클립", "키리누키",
            "vlog", "유튜버", "youtube", "YOUTUBE", "유튜브", "코딩", "코드", "로블록스", "덕질",
            "음식", "기도", "교회", "여행", "VOD", "풀영상"
    );

    private final List<String> excludeKeywordsInDescription = Arrays.asList(
            "팬클립", "팬영상", "팬채널", "저장소", "브이로그", "학년", "초등학", "중학", "고등학",
            "코딩", "로블록스", "덕질", "음식", "지식", "협찬", "비지니스", "광고", "병맛",
            "종합게임", "종겜", "기도", "교회", "목사", "여행", "애니메이션", "개그", "코미디",
            "뷰티", "fashion", "패션", "vlog", "게임채널", "연주", "강의", "더빙", "일상",
            "다시보기", "풀영상", "쇼츠", "서브채널", "그림", "팬페이지", "개그맨", "다이어트",
            "4K", "커뮤니티", "악세사리", "쇼핑몰", "경제", "키리누키", "채널 옮겼습니다",
            "클립", "넷플릭스", "게이머", "팬 채널", "게임방송", "리뷰", "예술", "Fashion",
            "자기관리", "예능", "희극인", "장애인", "실물"
    );

    private final List<String> priorityKeywords = Arrays.asList(
            "버츄얼 유튜버", "버튜버", "V-Youtuber", "버츄얼 유튜버"
    );

    private final Cache<String, Boolean> processedCache = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    private final List<AtomicInteger> apiKeyUsage;

    private final RateLimiter rateLimiter = RateLimiter.create(5.0);

    private final Counter searchApiCounter;
    private final Counter channelsApiCounter;

    public UploadVtuberService(YouTube youTube,
                               VtuberRepository vtuberRepository,
                               ExceptVtuberRepository exceptVtuberRepository,
                               @Value("${youtube.api.keys}") String apiKeys,
                               MeterRegistry meterRegistry) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.apiKeys = Arrays.asList(apiKeys.split(","));
        this.apiKeyUsage = apiKeys.lines()
                .map(key -> new AtomicInteger(0))
                .collect(Collectors.toList());

        this.searchApiCounter = meterRegistry.counter("youtube.api.search");
        this.channelsApiCounter = meterRegistry.counter("youtube.api.channels");
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private synchronized void rotateApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        if (currentKeyIndex == 0) {
            logger.severe("모든 API 키의 할당량이 소진되었습니다.");
            throw new RuntimeException("모든 API 키의 할당량이 소진되었습니다.");
        }
        logger.warning("API 키를 다음 키로 전환했습니다: " + getCurrentApiKey());
    }

    private void incrementApiKeyUsage() {
        apiKeyUsage.get(currentKeyIndex).incrementAndGet();
        if (apiKeyUsage.get(currentKeyIndex).get() >= MAX_QUOTA_PER_KEY) {
            rotateApiKey();
        }
    }

    public void fetchAndSaveVtuberChannels() {
        logger.info("=== fetchAndSaveVtuberChannels 시작 ===");
        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        logger.info("제외된 채널 ID 목록: " + exceptChannelIds);

        Set<String> processedChannelIds = new HashSet<>(vtuberRepository.findAllChannelIds());
        logger.info("이미 처리된 채널 ID 목록: " + processedChannelIds);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

        for (String query : queries) {
            logger.info("쿼리 실행: " + query);
            String pageToken = null;
            int pagesFetched = 0;

            do {
                if (pagesFetched >= MAX_PAGES_PER_QUERY) {
                    logger.info("쿼리 '" + query + "'에 대해 최대 페이지 수를 초과했습니다.");
                    break;
                }
                pagesFetched++;

                try {
                    rateLimiter.acquire();
                    logger.info("YouTube Search API 호출 시작 (쿼리: " + query + ", 페이지: " + pagesFetched + ")");

                    YouTube.Search.List search = youTube.search().list("id,snippet");
                    search.setQ(query);
                    search.setType("channel");
                    search.setFields("nextPageToken,items(id/channelId,snippet/title,snippet/description)");
                    search.setMaxResults(50L);
                    search.setKey(getCurrentApiKey());
                    search.setPageToken(pageToken);

                    incrementApiKeyUsage();
                    searchApiCounter.increment();

                    SearchListResponse searchResponse = search.execute();
                    List<SearchResult> searchResultList = searchResponse.getItems();
                    logger.info("API 응답 수신: " + searchResultList.size() + " 개 채널");

                    List<String> channelIds = searchResultList.stream()
                            .map(result -> result.getId().getChannelId())
                            .filter(channelId -> !exceptChannelIds.contains(channelId))
                            .filter(channelId -> !processedChannelIds.contains(channelId))
                            .filter(channelId -> processedCache.getIfPresent(channelId) == null)
                            .collect(Collectors.toList());

                    logger.info("필터링된 채널 ID 목록: " + channelIds);

                    if (!channelIds.isEmpty()) {
                        List<List<String>> partitions = partitionList(channelIds, 50);
                        for (List<String> partition : partitions) {
                            executor.submit(() -> {
                                processChannels(partition, exceptChannelIds, processedChannelIds);
                                partition.forEach(id -> processedCache.put(id, true));
                            });
                        }
                    }

                    pageToken = searchResponse.getNextPageToken();
                } catch (IOException e) {
                    logger.severe("API 호출 중 오류 발생: " + e.getMessage());
                    rotateApiKey();
                }
            } while (pageToken != null);
        }

        updateExistingChannelsMissingImages(executor);

        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
                executor.shutdownNow();
                logger.warning("작업이 시간 내에 완료되지 않아 강제 종료되었습니다.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            logger.severe("스레드 인터럽트 발생: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        logger.info("=== fetchAndSaveVtuberChannels 종료 ===");
    }

    private void updateExistingChannelsMissingImages(ThreadPoolExecutor executor) {
        List<VtuberEntity> vtubersWithMissingImages = vtuberRepository.findByChannelImgIsNull();
        logger.info("프로필 이미지가 없는 VTuber 수: " + vtubersWithMissingImages.size());

        for (VtuberEntity vtuber : vtubersWithMissingImages) {
            executor.submit(() -> {
                String imageUrl = fetchChannelProfileImage(vtuber.getChannelId());
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    vtuber.setChannelImg(imageUrl);
                    vtuberRepository.save(vtuber);
                    logger.info("프로필 이미지 업데이트 완료: " + vtuber.getName());
                } else {
                    logger.warning("프로필 이미지 가져오기 실패: " + vtuber.getName());
                }
            });
        }
    }

    private void processChannels(List<String> channelIds, Set<String> exceptChannelIds, Set<String> processedChannelIds) {
        logger.info("processChannels 시작 - 채널 ID 개수: " + channelIds.size());

        try {
            rateLimiter.acquire();

            YouTube.Channels.List channelRequest = youTube.channels().list("snippet,statistics");
            channelRequest.setId(String.join(",", channelIds));
            channelRequest.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");
            channelRequest.setKey(getCurrentApiKey());

            incrementApiKeyUsage();
            channelsApiCounter.increment();

            ChannelListResponse channelResponse = channelRequest.execute();
            List<Channel> channels = channelResponse.getItems();

            if (channels == null || channels.isEmpty()) {
                logger.warning("채널 정보를 가져오지 못했습니다.");
                return;
            }

            for (Channel channel : channels) {
                String channelId = channel.getId();

                if (exceptChannelIds.contains(channelId) || processedChannelIds.contains(channelId)) {
                    logger.info("제외된 채널: " + channel.getSnippet().getTitle() + " - " + channelId);
                    continue;
                }

                if (isKoreanVtuber(channel) && channel.getStatistics().getSubscriberCount().compareTo(BigInteger.valueOf(MIN_SUBSCRIBERS)) >= 0) {
                    if (vtuberRepository.findByChannelId(channelId).isEmpty()) {
                        VtuberEntity vtuber = new VtuberEntity();
                        vtuber.setChannelId(channelId);
                        vtuber.setTitle(channel.getSnippet().getTitle());

                        String description = channel.getSnippet().getDescription();
                        if (description != null && description.length() > 255) {
                            description = description.substring(0, 255);
                        }
                        vtuber.setDescription(description);

                        vtuber.setName(channel.getSnippet().getTitle());
                        vtuber.setSubscribers(channel.getStatistics().getSubscriberCount());
                        vtuber.setAddedTime(LocalDateTime.now());

                        if (channel.getSnippet().getThumbnails() != null &&
                                channel.getSnippet().getThumbnails().getDefault() != null) {
                            vtuber.setChannelImg(channel.getSnippet().getThumbnails().getDefault().getUrl());
                        }

                        vtuber.setStatus("new");

                        vtuberRepository.save(vtuber);
                        processedChannelIds.add(channelId);
                        logger.info("새로운 VTuber 저장: " + vtuber.getName() + " - " + channelId);
                    }
                } else {
                    logger.info("걸러진 채널: " + channel.getSnippet().getTitle() + " - " + channelId);
                }
            }
        } catch (IOException e) {
            logger.severe("API 호출 중 오류 발생: " + e.getMessage());
            rotateApiKey();
        }
        logger.info("processChannels 종료");
    }

    private boolean isKoreanVtuber(Channel channel) {
        String title = channel.getSnippet().getTitle();
        String description = channel.getSnippet().getDescription();

        logger.info("채널 확인 중: " + title + " - " + description);

        boolean containsKoreanInTitle = title.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsKoreanInDescription = description != null && description.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsVtuberInTitle = title.contains("버튜버") || title.contains("Vtuber");
        boolean containsVtuberInDescription = description != null && (description.contains("버튜버") || description.contains("Vtuber"));

        boolean containsPriorityKeyword = priorityKeywords.stream().anyMatch(keyword ->
                title.contains(keyword) || (description != null && description.contains(keyword))
        );

        boolean containsExcludeKeywordsInTitle = excludeKeywordsInTitle.stream().anyMatch(title::contains);
        boolean containsExcludeKeywordsInDescription = description != null && excludeKeywordsInDescription.stream().anyMatch(description::contains);

        logger.info("containsKoreanInTitle: " + containsKoreanInTitle);
        logger.info("containsKoreanInDescription: " + containsKoreanInDescription);
        logger.info("containsVtuberInTitle: " + containsVtuberInTitle);
        logger.info("containsVtuberInDescription: " + containsVtuberInDescription);
        logger.info("containsPriorityKeyword: " + containsPriorityKeyword);
        logger.info("containsExcludeKeywordsInTitle: " + containsExcludeKeywordsInTitle);
        logger.info("containsExcludeKeywordsInDescription: " + containsExcludeKeywordsInDescription);

        if ((containsExcludeKeywordsInTitle || containsExcludeKeywordsInDescription) && !containsPriorityKeyword) {
            logger.info("제외된 이유 - 제목/설명에 제외 키워드 포함: " + title);
            return false;
        }

        boolean result = (containsKoreanInTitle && containsVtuberInTitle) ||
                (containsKoreanInDescription && containsVtuberInDescription) ||
                containsPriorityKeyword;

        logger.info("채널 필터링 결과: " + title + " - " + result);
        return result;
    }

    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private String fetchChannelProfileImage(String channelId) {
        try {
            rateLimiter.acquire();

            YouTube.Channels.List channelsList = youTube.channels().list("snippet");
            channelsList.setId(channelId);
            channelsList.setKey(getCurrentApiKey());
            channelsList.setFields("items(snippet/thumbnails/default/url)");

            incrementApiKeyUsage();
            channelsApiCounter.increment();

            ChannelListResponse response = channelsList.execute();
            List<Channel> channels = response.getItems();
            if (!channels.isEmpty() && channels.get(0).getSnippet().getThumbnails() != null) {
                return channels.get(0).getSnippet().getThumbnails().getDefault().getUrl();
            }
        } catch (Exception e) {
            logger.warning("프로필 이미지 가져오기 실패 - 채널 ID: " + channelId + " - 오류: " + e.getMessage());
            rotateApiKey();
        }
        return null;
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void scheduledFetchAndSaveVtuberChannels() {
        fetchAndSaveVtuberChannels();
    }

}
