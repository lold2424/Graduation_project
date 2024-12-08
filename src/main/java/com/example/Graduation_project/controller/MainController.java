package com.example.Graduation_project.controller;

import com.example.Graduation_project.dto.MainPageResponse;
import com.example.Graduation_project.entity.VtuberSongsEntity;
import com.example.Graduation_project.service.SongService;
import com.example.Graduation_project.service.SortSongsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/main")
public class MainController {

    private final SongService songService;
    private final SortSongsService sortSongsService;

    public MainController(SongService songService, SortSongsService sortSongsService) {
        this.songService = songService;
        this.sortSongsService = sortSongsService;
    }

    @GetMapping
    public MainPageResponse getMainPage(
            @RequestParam(value = "gender", required = false, defaultValue = "all") String gender) {

        List<VtuberSongsEntity> randomVideoSongs = songService.getRandomVideoSongs(9, gender);
        List<VtuberSongsEntity> top10WeeklySongs = sortSongsService.getTop10SongsByViewsIncreaseWeek(gender);
        List<VtuberSongsEntity> top10DailySongs = sortSongsService.getTop10SongsByViewsIncreaseDay(gender);
        List<VtuberSongsEntity> top10RecentSongs = sortSongsService.getTop9SongsByPublishedAt(gender);

        List<VtuberSongsEntity> randomShorts = songService.getRandomShortsSongs(9, gender);
        List<VtuberSongsEntity> top10WeeklyShorts = sortSongsService.getTop10ShortsByViewsIncreaseWeek(gender);
        List<VtuberSongsEntity> top9RecentShorts = sortSongsService.getTop9ShortsByPublishedAt(gender);

        return new MainPageResponse(randomVideoSongs, top10WeeklySongs, top10DailySongs, top10RecentSongs,
                randomShorts, top10WeeklyShorts, top9RecentShorts);
    }
}
