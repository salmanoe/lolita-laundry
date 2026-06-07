package id.co.lolita.laundry.client.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface ClientPriceListJpaRepository extends JpaRepository<ClientPriceListJpaEntity, Long> {

    Optional<ClientPriceListJpaEntity> findByClientIdAndItemIdAndEffectiveDate(
            Long clientId, Long itemId, LocalDate effectiveDate);

    /**
     * Current effective price for every item for a client.
     * Uses a subquery to pick the latest row per (client, item) pair as of today.
     */
    @Query("""
            SELECT p FROM ClientPriceListJpaEntity p
            WHERE p.clientId = :clientId
              AND p.effectiveDate = (
                  SELECT MAX(p2.effectiveDate)
                  FROM ClientPriceListJpaEntity p2
                  WHERE p2.clientId = p.clientId
                    AND p2.itemId = p.itemId
                    AND p2.effectiveDate <= CURRENT_DATE
              )
            ORDER BY p.itemId
            """)
    List<ClientPriceListJpaEntity> findCurrentPrices(@Param("clientId") Long clientId);

    /**
     * Effective price for a specific item on a specific date.
     * Returns the most recent row with effectiveDate <= asOf.
     */
    @Query("""
            SELECT p FROM ClientPriceListJpaEntity p
            WHERE p.clientId = :clientId
              AND p.itemId   = :itemId
              AND p.effectiveDate <= :asOf
            ORDER BY p.effectiveDate DESC
            LIMIT 1
            """)
    Optional<ClientPriceListJpaEntity> findEffectivePrice(
            @Param("clientId") Long clientId,
            @Param("itemId") Long itemId,
            @Param("asOf") LocalDate asOf
    );
}
