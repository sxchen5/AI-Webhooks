package com.scanplatform.service;

import com.scanplatform.agent.AgentCliKind;
import com.scanplatform.dto.AgentModelOptionDto;
import com.scanplatform.dto.AgentModelOptionItemDto;
import com.scanplatform.entity.AgentModelOption;
import com.scanplatform.repository.AgentModelOptionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentModelCatalogService {

    private final AgentModelOptionRepository repository;

    @Transactional(readOnly = true)
    public List<AgentModelOptionItemDto> listEnabledOptions(AgentCliKind kind) {
        return repository.findByCliKindAndStatusOrderBySortOrderAscModelKeyAsc(kind, 1).stream()
                .map(o -> AgentModelOptionItemDto.of(
                        o.getModelKey(),
                        StringUtils.hasText(o.getDisplayLabel()) ? o.getDisplayLabel() : o.getModelKey()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> listEnabledModelKeys(AgentCliKind kind) {
        return repository.findByCliKindAndStatusOrderBySortOrderAscModelKeyAsc(kind, 1).stream()
                .map(AgentModelOption::getModelKey)
                .collect(Collectors.toList());
    }

    /**
     * 非空模型名校验；库中无启用项时放行任意非空短字符串（便于首次部署前仍能跑通）。
     */
    public String normalizeOrNull(AgentCliKind kind, String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String m = raw.trim();
        if (m.length() > 64) {
            throw new IllegalArgumentException("模型名过长");
        }
        if (!repository.existsByCliKindAndModelKeyAndStatus(kind, m, 1)) {
            long cnt = repository.findByCliKindAndStatusOrderBySortOrderAscModelKeyAsc(kind, 1).size();
            if (cnt == 0) {
                return m;
            }
            throw new IllegalArgumentException("不支持的模型: " + m + "（CLI=" + kind + "）");
        }
        return m;
    }

    public String appendModelFlag(AgentCliKind kind, String command, String model) {
        if (command == null) {
            return null;
        }
        String m = normalizeOrNull(kind, model);
        if (m == null) {
            return command;
        }
        if (command.contains("--model")) {
            return command;
        }
        return command + " --model " + m;
    }

    @Transactional(readOnly = true)
    public Page<AgentModelOption> page(String cliFilterRaw, String keywordRaw, Pageable pageable) {
        String kw = StringUtils.hasText(keywordRaw) ? keywordRaw.trim() : "";
        Specification<AgentModelOption> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (StringUtils.hasText(cliFilterRaw)) {
                preds.add(cb.equal(root.get("cliKind"), AgentCliKind.fromDb(cliFilterRaw.trim())));
            }
            if (StringUtils.hasText(kw)) {
                String like = "%" + kw.toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("modelKey")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("displayLabel"), cb.literal(""))), like)));
            }
            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public AgentModelOption get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在"));
    }

    @Transactional
    public AgentModelOption create(AgentModelOptionDto dto) {
        AgentModelOption e = new AgentModelOption();
        apply(dto, e);
        return repository.save(e);
    }

    @Transactional
    public AgentModelOption update(Long id, AgentModelOptionDto dto) {
        AgentModelOption e = get(id);
        apply(dto, e);
        return repository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private static void apply(AgentModelOptionDto dto, AgentModelOption e) {
        e.setCliKind(dto.getCliKind() != null ? dto.getCliKind() : AgentCliKind.CURSOR);
        e.setModelKey(dto.getModelKey().trim());
        e.setDisplayLabel(StringUtils.hasText(dto.getDisplayLabel()) ? dto.getDisplayLabel().trim() : null);
        e.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }
}
