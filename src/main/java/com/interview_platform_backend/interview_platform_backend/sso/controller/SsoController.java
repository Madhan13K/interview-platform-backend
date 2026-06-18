package com.interview_platform_backend.interview_platform_backend.sso.controller;

import com.interview_platform_backend.interview_platform_backend.sso.dto.SsoConfigurationRequest;
import com.interview_platform_backend.interview_platform_backend.sso.dto.SsoConfigurationResponse;
import com.interview_platform_backend.interview_platform_backend.sso.service.SsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sso")
@Tag(name = "SSO/SAML", description = "Enterprise SSO/SAML configuration management")
public class SsoController {

    private final SsoService ssoService;

    public SsoController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @Operation(summary = "Create SSO/SAML configuration",
            description = "Configure a new SAML Identity Provider for a tenant. " +
                    "Supports Okta, OneLogin, Azure AD, and generic SAML 2.0 providers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSO configuration created"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration"),
            @ApiResponse(responseCode = "409", description = "Configuration already exists for this provider/tenant")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SsoConfigurationResponse> createConfiguration(
            @RequestBody @Valid SsoConfigurationRequest request) {
        return ResponseEntity.ok(ssoService.createConfiguration(request));
    }

    @Operation(summary = "Update SSO/SAML configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration updated"),
            @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @PutMapping("/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SsoConfigurationResponse> updateConfiguration(
            @PathVariable UUID configId,
            @RequestBody @Valid SsoConfigurationRequest request) {
        return ResponseEntity.ok(ssoService.updateConfiguration(configId, request));
    }

    @Operation(summary = "Get SSO configuration by ID")
    @ApiResponse(responseCode = "200", description = "Configuration found")
    @GetMapping("/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SsoConfigurationResponse> getConfiguration(@PathVariable UUID configId) {
        return ResponseEntity.ok(ssoService.getConfiguration(configId));
    }

    @Operation(summary = "Get all SSO configurations for a tenant")
    @ApiResponse(responseCode = "200", description = "List of configurations")
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SsoConfigurationResponse>> getConfigurationsForTenant(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(ssoService.getConfigurationsForTenant(tenantId));
    }

    @Operation(summary = "Enable or disable an SSO configuration")
    @ApiResponse(responseCode = "200", description = "Configuration toggled")
    @PatchMapping("/{configId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SsoConfigurationResponse> toggleConfiguration(
            @PathVariable UUID configId,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(ssoService.toggleConfiguration(configId, enabled));
    }

    @Operation(summary = "Delete an SSO configuration")
    @ApiResponse(responseCode = "204", description = "Configuration deleted")
    @DeleteMapping("/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable UUID configId) {
        ssoService.deleteConfiguration(configId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get SSO login URL for a tenant",
            description = "Returns the SAML login initiation URL. Redirect users here to start SSO flow.")
    @ApiResponse(responseCode = "200", description = "Login URLs returned")
    @GetMapping("/tenant/{tenantId}/login-urls")
    public ResponseEntity<List<SsoConfigurationResponse>> getSsoLoginUrls(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ssoService.getConfigurationsForTenant(tenantId));
    }
}
