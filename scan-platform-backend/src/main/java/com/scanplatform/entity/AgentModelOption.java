package com.scanplatform.entity;

import com.scanplatform.agent.AgentCliKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_model_option")
@Getter
@Setter
public class AgentModelOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "cli_kind", nullable = false, length = 16)
    private AgentCliKind cliKind = AgentCliKind.CURSOR;

    @Column(name = "model_key", nullable = false, length = 64)
    private String modelKey;

    @Column(name = "display_label", length = 128)
    private String displayLabel;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
