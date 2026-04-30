-- 已有库：Git 问答消息正文需 utf8mb4 以保存 emoji（如 📋）；否则报 SQL 1366 Incorrect string value
ALTER TABLE git_qa_chat_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
