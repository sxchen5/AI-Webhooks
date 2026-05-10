package com.scanplatform.repository;

import com.scanplatform.agent.AgentCliKind;
import com.scanplatform.entity.AgentModelOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentModelOptionRepository extends JpaRepository<AgentModelOption, Long> {

    List<AgentModelOption> findByCliKindAndStatusOrderBySortOrderAscModelKeyAsc(AgentCliKind cliKind, Integer status);

    boolean existsByCliKindAndModelKeyAndStatus(AgentCliKind cliKind, String modelKey, Integer status);
}
