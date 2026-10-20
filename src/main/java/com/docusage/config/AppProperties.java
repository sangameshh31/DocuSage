package com.docusage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All DocuSage settings live under the {@code docusage.*} prefix
 * (see application.properties). Providers are pluggable:
 * embeddings and chat each have a zero-config local default and an
 * optional Gemini implementation enabled purely by configuration.
 */
@ConfigurationProperties(prefix = "docusage")
public class AppProperties {

    /** Characters per chunk when splitting document text. */
    private int chunkSize = 900;

    /** Characters of overlap between consecutive chunks (keeps context across boundaries). */
    private int chunkOverlap = 120;

    /** How many chunks to retrieve per question. */
    private int topK = 4;

    private final Embedding embedding = new Embedding();
    private final Chat chat = new Chat();
    private final Gemini gemini = new Gemini();

    public static class Embedding {
        /** "hashing" (local, no key needed) or "gemini". */
        private String provider = "hashing";
        /** Vector size produced by the local hashing provider. */
        private int dimension = 384;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
    }

    public static class Chat {
        /** "extractive" (local, no key needed) or "gemini". */
        private String provider = "extractive";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }

    public static class Gemini {
        /** Read from the GEMINI_API_KEY environment variable by default. */
        private String apiKey = "";
        private String embeddingModel = "text-embedding-004";
        private String chatModel = "gemini-2.0-flash";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public Embedding getEmbedding() { return embedding; }
    public Chat getChat() { return chat; }
    public Gemini getGemini() { return gemini; }
}
