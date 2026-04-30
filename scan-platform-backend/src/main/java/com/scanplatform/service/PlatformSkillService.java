package com.scanplatform.service;

import com.scanplatform.dto.PlatformSkillDto;
import com.scanplatform.dto.PlatformSkillOptionDto;
import com.scanplatform.entity.PlatformSkill;
import com.scanplatform.repository.PlatformSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSkillService {

    private final PlatformSkillRepository repository;

    @Transactional(readOnly = true)
    public Page<PlatformSkill> page(String keyword, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        if (kw.isEmpty()) {
            return repository.findAll(pageable);
        }
        return repository.pageByKeyword(kw, pageable);
    }

    /** 启用中的平台技能，供前端下拉 */
    @Transactional(readOnly = true)
    public List<PlatformSkillOptionDto> listEnabledOptions() {
        return repository.findByStatusOrderBySkillNameAsc(1).stream()
                .map(s -> new PlatformSkillOptionDto(s.getSkillName(), s.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlatformSkill get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("技能不存在"));
    }

    @Transactional
    public PlatformSkill create(PlatformSkillDto dto) {
        String name = dto.getSkillName().trim();
        validateSkillName(name);
        if (repository.existsBySkillName(name)) {
            throw new IllegalArgumentException("技能名已存在");
        }
        PlatformSkill e = new PlatformSkill();
        apply(dto, e, name);
        return repository.save(e);
    }

    @Transactional
    public PlatformSkill update(Long id, PlatformSkillDto dto) {
        PlatformSkill e = get(id);
        String name = dto.getSkillName().trim();
        validateSkillName(name);
        if (!e.getSkillName().equals(name) && repository.existsBySkillName(name)) {
            throw new IllegalArgumentException("技能名已被占用");
        }
        apply(dto, e, name);
        return repository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        log.info("删除平台技能 id={}", id);
    }

    private static void validateSkillName(String name) {
        if (!name.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*$")) {
            throw new IllegalArgumentException("技能名仅允许字母数字及 ._-，且不能以符号开头");
        }
    }

    private void apply(PlatformSkillDto dto, PlatformSkill e, String name) {
        e.setSkillName(name);
        e.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        e.setSkillBody(dto.getSkillBody().trim());
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }
}
