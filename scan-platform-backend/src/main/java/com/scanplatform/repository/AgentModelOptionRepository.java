package com.scanplatform.repository;

import com.scanplatform.agent.AgentCliKind;
import com.scanplatform.entity.AgentModelOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AgentModelOptionRepository extends JpaRepository<AgentModelOption, Long>, JpaSpecificationExecutor<AgentModelOption> {

    List<AgentModelOption> findByCliKindAndStatusOrderBySortOrderAscModelKeyAsc(AgentCliKind cliKind, Integer status);

    boolean existsByCliKindAndModelKeyAndStatus(AgentCliKind cliKind, String modelKey, Integer status);
}
