package com.example.service;

import com.example.config.ServiceConfig;
import com.example.model.License;
import com.example.model.Organization;
import com.example.repository.LicenseRepository;
import com.example.service.client.OrganizationFeignClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final ServiceConfig serviceConfig;
    private final MessageSource messageSource;
    private final LicenseRepository licenseRepository;
    private final OrganizationFeignClient organizationFeignClient;

    public License getLicense(String organizationId, String licenseId) {
        License license = licenseRepository.findByLicenseIdAndOrganizationId(licenseId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(messageSource.getMessage(
                        "license.search.error.message", null, Locale.getDefault()), licenseId, organizationId)));

        Organization organization = organizationFeignClient.getOrganization(organizationId);
        license.setOrganizationName(organization.getName());
        license.setContactName(organization.getContactName());
        license.setContactPhone(organization.getContactPhone());
        license.setContactEmail(organization.getContactEmail());

        license.withComment(serviceConfig.getProperty());
        return license;
    }

    public License createLicense(String organizationId, License license) {
        license.setLicenseId(UUID.randomUUID().toString());
        license.setOrganizationId(organizationId);
        return licenseRepository.save(license.withComment(serviceConfig.getProperty()));
    }

    public License updateLicense(String organizationId, String licenseId, License newLicense) {
        License license = licenseRepository.findByLicenseIdAndOrganizationId(licenseId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(messageSource.getMessage(
                        "license.search.error.message", null, Locale.getDefault()), licenseId, organizationId)));

        license.setDescription(newLicense.getDescription());
        license.setOrganizationId(organizationId);
        license.setProductName(newLicense.getProductName());
        license.setLicenseType(newLicense.getLicenseType());
        return licenseRepository.save(license.withComment(serviceConfig.getProperty()));
    }

    public String deleteLicense(String organizationId, String licenseId) {
        String responseMessage;

        License license = licenseRepository.findByLicenseIdAndOrganizationId(licenseId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(messageSource.getMessage(
                        "license.search.error.message", null, Locale.getDefault()), licenseId, organizationId)));

        licenseRepository.delete(license);

        responseMessage = String.format(messageSource.getMessage("license.delete.message", null,
                    Locale.getDefault()), licenseId, organizationId);

        return responseMessage;
    }

    @CircuitBreaker(name = "licenseService", fallbackMethod = "buildFallbackLicenseList")
    @Retry(name = "retryLicenseService", fallbackMethod = "buildFallbackLicenseList")
    @Bulkhead(name = "bulkheadLicenseService", fallbackMethod = "buildFallbackLicenseList")
    public List<License> getLicensesByOrganization(String organizationId) {
        randomlyRunLong();
        return licenseRepository.findByOrganizationId(organizationId);
    }

    @SuppressWarnings("unused")
    private List<License> buildFallbackLicenseList(String organizationId, Throwable t){
        List<License> fallbackList = new ArrayList<>();
        License license = new License();
        license.setLicenseId("0000000-00-00000");
        license.setOrganizationId(organizationId);
        license.setProductName("Sorry no licensing information currently available");
        fallbackList.add(license);
        return fallbackList;
    }

    private void randomlyRunLong() {
        Random rand = new Random();
        int randomNum = rand.nextInt(3) + 1;
        if (randomNum==3) sleep();
    }

    @SneakyThrows
    private void sleep(){
        Thread.sleep(5000);
        throw new java.util.concurrent.TimeoutException();
    }
}
