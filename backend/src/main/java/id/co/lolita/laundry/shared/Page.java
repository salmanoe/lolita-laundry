package id.co.lolita.laundry.shared;

import java.util.List;
import java.util.function.Function;

/**
 * Framework-free page of results returned across module ports (mirrors the fields of
 * Spring Data's {@code Page} without the dependency). Serialises directly to JSON as the
 * API list-response shape: {@code { content, page, size, totalElements, totalPages }}.
 *
 * @param page zero-based page index
 */
public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * Transforms the content (e.g. domain → response DTO) while preserving page metadata.
     */
    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new Page<>(mapped, page, size, totalElements, totalPages);
    }
}
