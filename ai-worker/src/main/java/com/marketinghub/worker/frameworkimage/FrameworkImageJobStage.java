package com.marketinghub.worker.frameworkimage;

public enum FrameworkImageJobStage {
    WAITING_AI_WORKER,
    CLAIMED,
    SENT_TO_OPENAI_BATCH,
    WAITING_OPENAI_BATCH,
    OPENAI_IMAGE_READY,
    UPLOADED_TO_CLOUDFLARE,
    NOTIFIED_BACKEND,
    WAITING_WEBNIZATION,
    WEB_READY,
    FAILED
}
