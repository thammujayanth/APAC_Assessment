package com.apac.assessment.entity;

import com.apac.assessment.enums.LineOfBusiness;
import com.apac.assessment.enums.LineOfBusinessConverter;
import com.apac.assessment.enums.PolicyStatus;
import com.apac.assessment.enums.PolicyStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "policies", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private UUID id;

	@Column(name = "policy_number", nullable = false, length = 10)
	private String policyNumber;

	@Column(name = "policyholder_name", nullable = false, length = 255)
	private String policyholderName;

	@Convert(converter = LineOfBusinessConverter.class)
	@Column(name = "line_of_business", nullable = false, length = 20)
	private LineOfBusiness lineOfBusiness;

	@Convert(converter = PolicyStatusConverter.class)
	@Column(name = "status", nullable = false, length = 20)
	private PolicyStatus status;

	@Column(name = "premium_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal premiumAmount;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "effective_date", nullable = false)
	private LocalDate effectiveDate;

	@Column(name = "expiry_date", nullable = false)
	private LocalDate expiryDate;

	@Column(name = "region", nullable = false, length = 50)
	private String region;

	@Column(name = "underwriter", nullable = false, length = 255)
	private String underwriter;

	@Column(name = "flagged_for_review", nullable = false)
	private boolean flaggedForReview;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	private void setCreationTimestamps() {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	private void setUpdateTimestamp() {
		updatedAt = LocalDateTime.now(ZoneOffset.UTC);
	}
}
