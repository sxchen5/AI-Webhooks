package com.scanplatform.service;

import com.scanplatform.entity.GitQaChatMessage;
import com.scanplatform.repository.GitQaChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GitQaChatMessageService {

    private final GitQaChatMessageRepository repository;

    @Transactional
    public void deleteById(Long messageId) {
        repository.deleteById(messageId);
    }

    @Transactional(readOnly = true)
    public Page<GitQaChatMessage> page(Long projectId, Pageable pageable) {
        return repository.findByProjectIdOrderByIdAsc(projectId, pageable);
    }

    @Transactional
    public Long saveUser(Long projectId, String content) {
        GitQaChatMessage m = new GitQaChatMessage();
        m.setProjectId(projectId);
        m.setRole("USER");
        m.setContent(content != null ? content : "");
        return repository.save(m).getId();
    }

    @Transactional
    public Long saveAssistant(Long projectId, String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        GitQaChatMessage m = new GitQaChatMessage();
        m.setProjectId(projectId);
        m.setRole("ASSISTANT");
        m.setContent(content);
        return repository.save(m).getId();
    }

    @Transactional
    public void delete(Long projectId, Long messageId) {
        GitQaChatMessage m = repository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        if (!projectId.equals(m.getProjectId())) {
            throw new IllegalArgumentException("消息不属于该问答配置");
        }
        repository.deleteById(messageId);
    }

    @Transactional
    public void deleteAllByProject(Long projectId) {
        repository.deleteByProjectId(projectId);
    }
}
