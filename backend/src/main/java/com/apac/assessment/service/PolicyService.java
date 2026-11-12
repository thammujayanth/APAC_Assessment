package com.apac.assessment.service;

import com.apac.assessment.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private final PolicyRepository policyRepository;
}
