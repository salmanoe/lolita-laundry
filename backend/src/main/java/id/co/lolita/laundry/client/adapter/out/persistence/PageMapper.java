package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.shared.PageQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Translates the framework-free {@link PageQuery} into a Spring Data {@link Pageable}.
 */
final class PageMapper {

    private PageMapper() {
    }

    static Pageable toPageable(PageQuery query) {
        if (query.sortBy() == null) {
            return PageRequest.of(query.page(), query.size());
        }
        var direction = query.direction() == PageQuery.Direction.DESC
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(query.page(), query.size(), Sort.by(direction, query.sortBy()));
    }
}
