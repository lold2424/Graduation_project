package com.example.Graduation_project.repository;

import com.example.Graduation_project.entity.VtuberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VtuberRepository extends JpaRepository<VtuberEntity, Long> {
    Optional<VtuberEntity> findByChannelId(String channelId);

    @Query("SELECT v.channelId FROM VtuberEntity v")
    List<String> findAllChannelIds();

    List<VtuberEntity> findByChannelImgIsNull();

    List<VtuberEntity> findAllByNameContaining(String name);

    List<VtuberEntity> findByStatus(String status);
    List<VtuberEntity> findAllByGender(String gender);
}
