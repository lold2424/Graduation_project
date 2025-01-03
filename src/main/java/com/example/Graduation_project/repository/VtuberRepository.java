package com.example.Graduation_project.repository;

import com.example.Graduation_project.entity.VtuberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VtuberRepository extends JpaRepository<VtuberEntity, Long> {

    void deleteByChannelId(String channelId);

    Optional<VtuberEntity> findByChannelId(String channelId);

    @Query("SELECT v.channelId FROM VtuberEntity v WHERE v.gender = :gender")
    List<String> findChannelIdsByGender(@Param("gender") String gender);

    @Query("SELECT v.channelId FROM VtuberEntity v")
    List<String> findAllChannelIds();

    @Query("SELECT v.channelId FROM VtuberEntity v WHERE v.gender IS NULL")
    List<String> findChannelIdsWithNullGender();

    List<VtuberEntity> findByChannelImgIsNull();

    List<VtuberEntity> findAllByNameContaining(String name);

    List<VtuberEntity> findByStatus(String status);

    List<VtuberEntity> findAllByGender(String gender);
}
