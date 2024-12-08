package com.example.Graduation_project.service;

import com.example.Graduation_project.entity.VtuberSongsEntity;
import com.example.Graduation_project.repository.VtuberRepository;
import com.example.Graduation_project.repository.VtuberSongsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SongService {

    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberRepository vtuberRepository;

    public SongService(VtuberSongsRepository vtuberSongsRepository, VtuberRepository vtuberRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.vtuberRepository = vtuberRepository;
    }

    public List<VtuberSongsEntity> getRandomVideoSongs(int limit) {
        List<VtuberSongsEntity> randomSongs = vtuberSongsRepository.findRandomVideoSongs(limit);
        System.out.println("Fetched random songs: " + randomSongs.size());
        return randomSongs;
    }

    public List<VtuberSongsEntity> getRandomShortsSongs(int limit) {
        return vtuberSongsRepository.findRandomShortsSongs(limit);
    }

    // gender 필터링 적용된 메서드 추가
    public List<VtuberSongsEntity> getRandomVideoSongs(int limit, String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findRandomVideoSongsByChannelIds(channelIds, limit);
    }

    public List<VtuberSongsEntity> getRandomShortsSongs(int limit, String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findRandomShortsSongsByChannelIds(channelIds, limit);
    }

    private List<String> getChannelIdsByGender(String gender) {
        if (gender == null || gender.equalsIgnoreCase("all")) {
            return vtuberRepository.findAllChannelIds();
        } else if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("female")) {
            return vtuberRepository.findChannelIdsByGender(gender.toLowerCase());
        } else if (gender.equalsIgnoreCase("mixed")) { // null 값의 경우 'mixed'로 간주
            return vtuberRepository.findChannelIdsWithNullGender();
        } else {
            throw new IllegalArgumentException("Invalid gender parameter: " + gender);
        }
    }
}
