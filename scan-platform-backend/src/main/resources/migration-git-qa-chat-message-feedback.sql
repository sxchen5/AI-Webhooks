-- Git 问答聊天记录：助手消息点赞/点踩（MySQL；已存在列时请勿重复执行）
ALTER TABLE git_qa_chat_message ADD COLUMN feedback TINYINT NULL COMMENT '助手消息反馈：1点赞 -1点踩';
