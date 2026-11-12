package com.apac.assessment.repository;

import com.apac.assessment.dto.request.PolicyListQueryDto;
import com.apac.assessment.entity.Policy;
import com.apac.assessment.enums.LineOfBusiness;
import com.apac.assessment.enums.PolicyStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class PolicySpecifications {

	private PolicySpecifications() {
	}

	public static Specification<Policy> matching(
			PolicyListQueryDto query,
			PolicyStatus status,
			LineOfBusiness lineOfBusiness) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (lineOfBusiness != null) {
				predicates.add(criteriaBuilder.equal(root.get("lineOfBusiness"), lineOfBusiness));
			}
			if (query.region() != null) {
				predicates.add(criteriaBuilder.equal(root.get("region"), query.region()));
			}
			if (query.effectiveDateFrom() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(
						root.get("effectiveDate"), query.effectiveDateFrom()));
			}
			if (query.effectiveDateTo() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(
						root.get("effectiveDate"), query.effectiveDateTo()));
			}
			if (query.search() != null && !query.search().isEmpty()) {
				String searchPattern = "%" + query.search().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("policyNumber")), searchPattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("policyholderName")), searchPattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("underwriter")), searchPattern)));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
