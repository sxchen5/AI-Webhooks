package com.scanplatform.repository;

import com.scanplatform.entity.PlatformSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlatformSkillRepository extends JpaRepository<PlatformSkill, Long> {

    @Query(
            "SELECT s FROM PlatformSkill s WHERE "
                    + "LOWER(s.skillName) LIKE LOWER(CONCAT('%', :kw, '%')) OR "
                    + "LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :kw, '%'))")
    Page<PlatformSkill> pageByKeyword(@Param("kw") String kw, Pageable pageable);

    Optional<PlatformSkill> findBySkillNameAndStatus(String skillName, Integer status);

    boolean existsBySkillName(String skillName);

    boolean existsBySkillNameAndIdNot(String skillName, Long id);

    List<PlatformSkill> findByStatusOrderBySkillNameAsc(Integer status);
}
