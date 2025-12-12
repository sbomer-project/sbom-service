package org.jboss.sbomer.sbom.service.adapter.out.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import lombok.extern.slf4j.Slf4j;

/**
 * A thread-safe, in-memory implementation of the StatusRepository.
 * Simulates a database with relationships (One-to-Many) by resolving lists dynamically on fetch.
 */
@ApplicationScoped
@Alternative
@Priority(1)
@Slf4j
public class SimpleInMemStatusRepository implements StatusRepository {

    // Simulating tables
    private final Map<String, RequestRecord> requests = new ConcurrentHashMap<>();
    private final Map<String, GenerationRecord> generations = new ConcurrentHashMap<>();
    private final Map<String, EnhancementRecord> enhancements = new ConcurrentHashMap<>();

    // --- REQUESTS ---

    @Override
    public void saveRequestRecord(RequestRecord record) {
        requests.put(record.getId(), record);
        // If the record comes with children, save them too to ensure consistency
        if (record.getGenerationRecords() != null) {
            record.getGenerationRecords().forEach(this::saveGeneration);
        }
    }

    @Override
    public RequestRecord findRequestById(String requestId) {
        RequestRecord request = requests.get(requestId);
        if (request != null) {
            // Mimic Eager Loading: Populate the generations list from our 'generations' table
            List<GenerationRecord> gens = findGenerationsByRequestId(requestId);
            request.setGenerationRecords(gens);
        }
        return request;
    }

    @Override
    public Page<RequestRecord> findAllRequests(int pageIndex, int pageSize) {
        // Sort by ID to ensure stable pagination order
        List<RequestRecord> allContent = requests.values().stream()
                .sorted(Comparator.comparing(RequestRecord::getId))
                .collect(Collectors.toList());

        return paginate(allContent, pageIndex, pageSize);
    }

    // --- GENERATIONS ---

    @Override
    public void saveGeneration(GenerationRecord record) {
        generations.put(record.getId(), record);
        // If record comes with enhancements, save them too
        if (record.getEnhancements() != null) {
            record.getEnhancements().forEach(this::saveEnhancement);
        }
    }

    @Override
    public GenerationRecord findGenerationById(String generationId) {
        GenerationRecord gen = generations.get(generationId);
        if (gen != null) {
            // Mimic Eager Loading: Populate enhancements list
            gen.setEnhancements(findEnhancementsByGenerationId(generationId));
        }
        return gen;
    }

    @Override
    public List<GenerationRecord> findGenerationsByRequestId(String requestId) {
        return generations.values().stream()
                .filter(g -> requestId.equals(g.getRequestId()))
                .peek(g -> g.setEnhancements(findEnhancementsByGenerationId(g.getId()))) // Load children
                .collect(Collectors.toList());
    }

    @Override
    public Page<GenerationRecord> findGenerationsByRequestId(String requestId, int pageIndex, int pageSize) {
        List<GenerationRecord> filtered = findGenerationsByRequestId(requestId);
        // Sort for stable pagination (e.g., by Created date or ID)
        filtered.sort(Comparator.comparing(GenerationRecord::getId));
        return paginate(filtered, pageIndex, pageSize);
    }

    @Override
    public List<GenerationRecord> findByGenerationStatus(GenerationStatus status) {
        return generations.values().stream()
                .filter(g -> g.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public void updateGeneration(GenerationRecord record) {
        saveGeneration(record); // Map.put acts as update
    }

    // --- ENHANCEMENTS ---

    @Override
    public void saveEnhancement(EnhancementRecord record) {
        enhancements.put(record.getId(), record);
    }

    @Override
    public EnhancementRecord findEnhancementById(String enhancementId) {
        return enhancements.get(enhancementId);
    }

    @Override
    public List<EnhancementRecord> findByEnhancementStatus(EnhancementStatus status) {
        return enhancements.values().stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public void updateEnhancement(EnhancementRecord record) {
        saveEnhancement(record);
    }

    // --- LOGIC HELPERS ---

    private List<EnhancementRecord> findEnhancementsByGenerationId(String generationId) {
        return enhancements.values().stream()
                .filter(e -> generationId.equals(e.getGenerationId()))
                .sorted(Comparator.comparingInt(EnhancementRecord::getIndex)) // Ensure order
                .collect(Collectors.toList());
    }

    @Override
    public boolean isGenerationAndEnhancementsFinished(String generationId) {
        GenerationRecord gen = findGenerationById(generationId);
        if (gen == null) return false;

        // 1. Check Generation Status
        if (!GenerationStatus.FINISHED.equals(gen.getStatus())) {
            return false;
        }

        // 2. Check Enhancements Status
        List<EnhancementRecord> children = findEnhancementsByGenerationId(generationId);
        if (children.isEmpty()) {
            return true; // No enhancements, generation is finished, so we are done.
        }

        return children.stream()
                .allMatch(e -> EnhancementStatus.FINISHED.equals(e.getStatus()));
    }

    @Override
    public boolean isAllGenerationRequestsFinished(String requestId) {
        List<GenerationRecord> gens = findGenerationsByRequestId(requestId);
        if (gens.isEmpty()) return false;

        // Check if ALL generations in the request are strictly FINISHED (Success).
        // (Based on your requirement that we only send the final event if everything succeeded)
        return gens.stream().allMatch(g -> isGenerationAndEnhancementsFinished(g.getId()));
    }

    @Override
    public List<String> getFinalSbomUrlsForCompletedGeneration(String generationId) {
        GenerationRecord gen = findGenerationById(generationId);
        if (gen == null) return List.of();

        List<EnhancementRecord> children = findEnhancementsByGenerationId(generationId);

        if (children.isEmpty()) {
            return List.copyOf(gen.getGenerationSbomUrls());
        }

        // Find the highest index enhancement that is FINISHED
        return List.copyOf(children.stream()
                .filter(e -> EnhancementStatus.FINISHED.equals(e.getStatus()))
                .max(Comparator.comparingInt(EnhancementRecord::getIndex))
                .map(EnhancementRecord::getEnhancedSbomUrls)
                .orElse(gen.getGenerationSbomUrls()));
    }

    // --- PAGINATION UTILITY ---

    private <T> Page<T> paginate(List<T> allContent, int pageIndex, int pageSize) {
        long totalHits = allContent.size();
        int totalPages = (int) Math.ceil((double) totalHits / pageSize);

        List<T> pagedContent = allContent.stream()
                .skip((long) pageIndex * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        return Page.<T>builder()
                .content(pagedContent)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }
}
