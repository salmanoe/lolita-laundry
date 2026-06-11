package id.co.lolita.laundry.catalog.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ItemJpaRepository extends JpaRepository<ItemJpaEntity, Long> {
    List<ItemJpaEntity> findByActiveTrue();

    Page<ItemJpaEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByName(String name);
}
