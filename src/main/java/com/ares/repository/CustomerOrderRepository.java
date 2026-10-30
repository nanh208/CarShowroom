package com.ares.repository;

import com.ares.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByCode(String code);

    @EntityGraph(attributePaths = "items")
    @Query("select distinct o from CustomerOrder o where o.createdAt >= :from and o.createdAt < :until and upper(o.status) not in :excludedStatuses")
    List<CustomerOrder> findForecastableOrders(@Param("from") LocalDateTime from,
                                                @Param("until") LocalDateTime until,
                                                @Param("excludedStatuses") Set<String> excludedStatuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.code = :code")
    Optional<CustomerOrder> findByCodeForUpdate(@Param("code") String code);
}
