package com.example.controller;

import com.example.model.License;
import com.example.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("v1/organization/{organizationId}/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping("/{licenseId}")
    public ResponseEntity<License> getLicense(@PathVariable String organizationId, @PathVariable String licenseId) {
        License license = licenseService.getLicense(organizationId, licenseId);
        license.add(linkTo(methodOn(LicenseController.class).getLicense(organizationId, licenseId)).withSelfRel());
        license.add(linkTo(methodOn(LicenseController.class).createLicense(organizationId, license)).withRel("createLicense"));
        license.add(linkTo(methodOn(LicenseController.class).updateLicense(organizationId, licenseId, license)).withRel("updateLicense"));
        license.add(linkTo(methodOn(LicenseController.class).deleteLicense(organizationId, licenseId)).withRel("deleteLicense"));

        return ResponseEntity.ok(license);
    }

    @PostMapping
    public ResponseEntity<License> createLicense(@PathVariable String organizationId, @RequestBody License license) {
        return ResponseEntity.ok(licenseService.createLicense(organizationId, license));
    }

    @PutMapping("/{licenseId}")
    public ResponseEntity<License> updateLicense(@PathVariable String organizationId, @PathVariable String licenseId, @RequestBody License license) {
        return ResponseEntity.ok(licenseService.updateLicense(organizationId, licenseId, license));
    }

    @DeleteMapping("/{licenseId}")
    public ResponseEntity<String> deleteLicense(@PathVariable String organizationId, @PathVariable String licenseId) {
        return ResponseEntity.ok(licenseService.deleteLicense(organizationId, licenseId));
    }

    @GetMapping("/")
    public List<License> getLicenses(@PathVariable String organizationId) {
        return licenseService.getLicensesByOrganization(organizationId);
    }

}
