package com.example.Graduation_project.service;

import com.example.Graduation_project.entity.VtuberSongsEntity;
import com.example.Graduation_project.repository.VtuberSongsRepository;
import com.example.Graduation_project.repository.VtuberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SortSongsService {

    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberRepository vtuberRepository;

    public SortSongsService(VtuberSongsRepository vtuberSongsRepository, VtuberRepository vtuberRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.vtuberRepository = vtuberRepository;
    }

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseWeek(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByViewsIncreaseWeekDescAndChannelIds(channelIds);
    }

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseDay(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByViewsIncreaseDayDescAndChannelIds(channelIds);
    }

    public List<VtuberSongsEntity> getTop9SongsByPublishedAt(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop9ByPublishedAtDescAndChannelIds(channelIds);
    }

    public List<VtuberSongsEntity> getTop10ShortsByViewsIncreaseWeek(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ShortsByViewsIncreaseWeekDescAndChannelIds(channelIds);
    }

    public List<VtuberSongsEntity> getTop9ShortsByPublishedAt(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop9ShortsByPublishedAtDescAndChannelIds(channelIds);
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
