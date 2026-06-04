package id.co.lolita.laundry.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ItemJpaRepository extends JpaRepository<ItemJpaEntity, Long> {
    List<ItemJpaEntity> findByActiveTrue();

    boolean existsByName(String name);
}
