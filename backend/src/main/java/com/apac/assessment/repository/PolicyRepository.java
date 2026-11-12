package com.apac.assessment.repository;

import com.apac.assessment.entity.Policy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
}
