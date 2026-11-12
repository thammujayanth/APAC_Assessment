package com.apac.assessment.repository;

import com.apac.assessment.entity.Policy;
import com.apac.assessment.enums.PolicyStatus;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolicyRepository extends JpaRepository<Policy, UUID>, JpaSpecificationExecutor<Policy> {

	long countByStatus(PolicyStatus status);

	long countByStatusAndExpiryDateBetween(PolicyStatus status, LocalDate from, LocalDate to);

	@Query("""
			SELECT p.lineOfBusiness AS lineOfBusiness,
			       p.currency AS currency,
			       SUM(p.premiumAmount) AS totalPremium
			FROM Policy p
			GROUP BY p.lineOfBusiness, p.currency
			ORDER BY p.lineOfBusiness, p.currency
			""")
	List<PolicyPremiumTotalProjection> summarizePremiumByLineOfBusinessAndCurrency();
}
