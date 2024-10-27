package com.example.Graduation_project.service;

import com.example.Graduation_project.entity.VtuberSongsEntity;
import com.example.Graduation_project.repository.VtuberSongsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SortSongsService {

    private final VtuberSongsRepository vtuberSongsRepository;

    public SortSongsService(VtuberSongsRepository vtuberSongsRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
    }

    // 7일 기준 조회수 최상위 정렬 (노래만)
    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseWeek() {
        return vtuberSongsRepository.findTop10ByStatusOrderByViewsIncreaseWeekDesc("existing");
    }

    // 1일 기준 조회수 최상위 정렬 (노래만)
    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseDay() {
        return vtuberSongsRepository.findTop10ByStatusOrderByViewsIncreaseDayDesc();
    }

    // 최신순 정렬 (노래만)
    public List<VtuberSongsEntity> getTop9SongsByPublishedAt() {
        return vtuberSongsRepository.findTop10ByPublishedAtDesc();
    }

    // 7일 기준 조회수 최상위 정렬 (쇼츠만)
    public List<VtuberSongsEntity> getTop10ShortsByViewsIncreaseWeek() {
        return vtuberSongsRepository.findTop10ShortsByViewsIncreaseWeekDesc();
    }

    // 최신순 정렬 (쇼츠만)
    public List<VtuberSongsEntity> getTop9ShortsByPublishedAt() {
        return vtuberSongsRepository.findTop9ShortsByPublishedAtDesc();
    }
}
