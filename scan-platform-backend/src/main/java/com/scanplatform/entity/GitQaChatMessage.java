package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Git 项目 AI 问答单条聊天记录（用户问题或助手回复）。
 */
@Entity
@Table(name = "git_qa_chat_message")
@Getter
@Setter
public class GitQaChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** USER 或 ASSISTANT */
    @Column(name = "role", length = 16, nullable = false)
    private String role;

    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    /**
     * 用户对助手回复的反馈：1=点赞，-1=点踩，null=未选。
     */
    @Column(name = "feedback")
    private Integer feedback;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
