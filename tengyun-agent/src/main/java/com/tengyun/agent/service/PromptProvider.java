package com.tengyun.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PromptProvider {

    private final String fallbackPrompt;
    private final String promptFilePath;
    private final long reloadIntervalMs;

    private volatile String cachedPrompt;
    private volatile long lastLoadedAt;
    private volatile long lastModifiedAt = -1L;

    public PromptProvider(
            @Value("${agent.system-prompt:You are an e-commerce shopping assistant.}") String fallbackPrompt,
            @Value("${agent.prompt.file-path:}") String promptFilePath,
            @Value("${agent.prompt.reload-interval-ms:3000}") long reloadIntervalMs
    ) {
        this.fallbackPrompt = fallbackPrompt;
        this.promptFilePath = promptFilePath == null ? "" : promptFilePath.trim();
        this.reloadIntervalMs = reloadIntervalMs <= 0 ? 3000 : reloadIntervalMs;
    }

    public String getSystemPrompt() {
        if (promptFilePath.isBlank()) {
            return fallbackPrompt;
        }

        long now = System.currentTimeMillis();
        if (cachedPrompt != null && now - lastLoadedAt < reloadIntervalMs) {
            return cachedPrompt;
        }

        synchronized (this) {
            if (cachedPrompt != null && now - lastLoadedAt < reloadIntervalMs) {
                return cachedPrompt;
            }
            String loaded = loadFromFileOrFallback();
            cachedPrompt = loaded;
            lastLoadedAt = System.currentTimeMillis();
            return loaded;
        }
    }

    private String loadFromFileOrFallback() {
        try {
            Path path = Paths.get(promptFilePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return fallbackPrompt;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (cachedPrompt != null && modified == lastModifiedAt) {
                return cachedPrompt;
            }
            String content = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                return fallbackPrompt;
            }
            lastModifiedAt = modified;
            return content;
        } catch (IOException e) {
            return fallbackPrompt;
        }
    }
}
