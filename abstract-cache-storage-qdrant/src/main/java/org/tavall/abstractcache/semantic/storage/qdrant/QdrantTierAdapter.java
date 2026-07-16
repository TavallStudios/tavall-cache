package org.tavall.abstractcache.semantic.storage.qdrant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StorageKeyUtil;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecordSerializer;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Qdrant-backed tier adapter for remote semantic storage.
 */
public final class QdrantTierAdapter implements CacheTierAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int SCROLL_PAGE_SIZE = 256;

    private final CacheTier tier;
    private final String baseUrl;
    private final String collectionName;
    private final HttpClient client;

    public QdrantTierAdapter(CacheTier tier, String baseUrl, String collectionName) {
        this.tier = tier;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.collectionName = collectionName;
        this.client = HttpClient.newHttpClient();
        ensureCollectionExists();
    }

    @Override
    public CacheTier tier() {
        return tier;
    }

    @Override
    public boolean isAvailable() {
        HttpRequest request = HttpRequest.newBuilder(uri("/readyz"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public Optional<StoredCacheRecord> get(SemanticCacheKey key) {
        HttpRequest request = HttpRequest.newBuilder(uri(collectionPath() + "/points/" + pointId(key)))
                .GET()
                .build();
        JsonNode response = sendJson(request, true);
        if (response == null || response.path("result").isNull() || response.path("result").isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(deserialize(response.path("result")));
    }

    @Override
    public void put(StoredCacheRecord record) {
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("adapterTier", tier.name());
        payload.put("storageKey", storageKey(record.key()));
        payload.put("fingerprint", record.key().fingerprint());
        payload.put("expiresAtMillis", record.expiresAt().toEpochMilli());
        payload.put("recordBytes", Base64.getEncoder().encodeToString(StoredCacheRecordSerializer.serialize(record)));

        ObjectNode point = OBJECT_MAPPER.createObjectNode();
        point.put("id", pointIdValue(record.key()));
        ArrayNode vector = point.putArray("vector");
        vector.add(0.0);
        point.set("payload", payload);

        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        ArrayNode points = body.putArray("points");
        points.add(point);

        sendJson(jsonRequest("PUT", collectionPath() + "/points?wait=true", body), false);
    }

    @Override
    public void remove(SemanticCacheKey key) {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        ArrayNode points = body.putArray("points");
        points.add(pointIdValue(key));
        sendJson(jsonRequest("POST", collectionPath() + "/points/delete?wait=true", body), false);
    }

    @Override
    public List<StoredCacheRecord> scanExpired(Instant now, int limit) {
        List<StoredCacheRecord> expired = new ArrayList<>();
        for (StoredCacheRecord record : snapshotRecords()) {
            if (record.isExpired(now)) {
                expired.add(record);
                if (expired.size() >= limit) {
                    break;
                }
            }
        }
        return expired;
    }

    @Override
    public List<StoredCacheRecord> snapshotRecords() {
        List<StoredCacheRecord> records = new ArrayList<>();
        JsonNode offset = null;
        while (true) {
            ObjectNode body = OBJECT_MAPPER.createObjectNode();
            body.put("limit", SCROLL_PAGE_SIZE);
            body.put("with_payload", true);
            body.put("with_vector", false);
            if (offset != null && !offset.isNull()) {
                body.set("offset", offset);
            }

            JsonNode response = sendJson(jsonRequest("POST", collectionPath() + "/points/scroll", body), false);
            JsonNode result = response.path("result");
            JsonNode points = result.path("points");
            if (!points.isArray() || points.isEmpty()) {
                return records;
            }
            for (JsonNode point : points) {
                records.add(deserialize(point));
            }
            offset = result.path("next_page_offset");
            if (offset.isMissingNode() || offset.isNull()) {
                return records;
            }
        }
    }

    public void clear() {
        dropCollection();
        ensureCollectionExists();
    }

    public void dropCollection() {
        HttpRequest request = HttpRequest.newBuilder(uri(collectionPath()))
                .DELETE()
                .build();
        sendJson(request, true);
    }

    private void ensureCollectionExists() {
        HttpRequest lookupRequest = HttpRequest.newBuilder(uri(collectionPath()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(lookupRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return;
            }
            if (response.statusCode() != 404) {
                throw new IllegalStateException("failed to inspect qdrant collection: " + response.body());
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("failed to inspect qdrant collection", exception);
        }

        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        ObjectNode vectors = body.putObject("vectors");
        vectors.put("size", 1);
        vectors.put("distance", "Cosine");
        body.put("on_disk_payload", true);

        sendJson(jsonRequest("PUT", collectionPath(), body), false);
    }

    private StoredCacheRecord deserialize(JsonNode pointNode) {
        JsonNode payload = pointNode.path("payload");
        String encoded = payload.path("recordBytes").asText(null);
        if (encoded == null) {
            throw new IllegalStateException("qdrant cache record is missing payload bytes");
        }
        return StoredCacheRecordSerializer.deserialize(Base64.getDecoder().decode(encoded));
    }

    private HttpRequest jsonRequest(String method, String path, JsonNode body) {
        try {
            return HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                    .build();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize qdrant request", exception);
        }
    }

    private JsonNode sendJson(HttpRequest request, boolean allowNotFound) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (allowNotFound && response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "qdrant request failed with status " + response.statusCode() + ": " + response.body()
                );
            }
            if (response.body() == null || response.body().isBlank()) {
                return OBJECT_MAPPER.nullNode();
            }
            return OBJECT_MAPPER.readTree(response.body());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("qdrant request failed", exception);
        }
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private String collectionPath() {
        return "/collections/" + URLEncoder.encode(collectionName, StandardCharsets.UTF_8);
    }

    private String pointId(SemanticCacheKey key) {
        return URLEncoder.encode(pointIdValue(key), StandardCharsets.UTF_8);
    }

    private String storageKey(SemanticCacheKey key) {
        return StorageKeyUtil.storageKey(key);
    }

    private String pointIdValue(SemanticCacheKey key) {
        return UUID.nameUUIDFromBytes(storageKey(key).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl cannot be null or blank");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
