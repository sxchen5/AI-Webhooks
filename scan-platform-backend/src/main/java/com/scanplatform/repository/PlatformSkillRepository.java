package com.scanplatform.repository;

import com.scanplatform.entity.PlatformSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSkillRepository extends JpaRepository<PlatformSkill, Long> {

    Optional<PlatformSkill> findBySkillNameAndStatus(String skillName, Integer status);

    boolean existsBySkillName(String skillName);

    boolean existsBySkillNameAndIdNot(String skillName, Long id);

    List<PlatformSkill> findByStatusOrderBySkillNameAsc(Integer status);
}
