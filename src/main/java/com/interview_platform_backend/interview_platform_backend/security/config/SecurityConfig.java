package com.interview_platform_backend.interview_platform_backend.security.config;

import com.interview_platform_backend.interview_platform_backend.security.apikey.filter.ApiKeyAuthenticationFilter;
import com.interview_platform_backend.interview_platform_backend.security.jwt.CustomUserDetailsService;
import com.interview_platform_backend.interview_platform_backend.security.jwt.JwtAuthenticationFilter;
import com.interview_platform_backend.interview_platform_backend.sso.config.DynamicRelyingPartyRegistrationRepository;
import com.interview_platform_backend.interview_platform_backend.sso.config.SamlAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.interview_platform_backend.interview_platform_backend.security.oauth2.CookieAuthorizationRequestRepository;
import com.interview_platform_backend.interview_platform_backend.security.oauth2.OAuth2FailureHandler;
import com.interview_platform_backend.interview_platform_backend.security.oauth2.OAuth2SuccessHandler;
import com.interview_platform_backend.interview_platform_backend.security.oauth2.PkceAuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final PkceAuthorizationRequestResolver pkceAuthorizationRequestResolver;
    private final CookieAuthorizationRequestRepository cookieAuthorizationRequestRepository;
    private final RateLimitingFilter rateLimitingFilter;
    private final XssSanitizingFilter xssSanitizingFilter;
    private final DynamicRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private final SamlAuthenticationSuccessHandler samlAuthenticationSuccessHandler;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                          CustomUserDetailsService customUserDetailsService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          OAuth2FailureHandler oAuth2FailureHandler,
                          PkceAuthorizationRequestResolver pkceAuthorizationRequestResolver,
                          CookieAuthorizationRequestRepository cookieAuthorizationRequestRepository,
                          RateLimitingFilter rateLimitingFilter,
                          XssSanitizingFilter xssSanitizingFilter,
                          DynamicRelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
                          SamlAuthenticationSuccessHandler samlAuthenticationSuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.pkceAuthorizationRequestResolver = pkceAuthorizationRequestResolver;
        this.cookieAuthorizationRequestRepository = cookieAuthorizationRequestRepository;
        this.rateLimitingFilter = rateLimitingFilter;
        this.xssSanitizingFilter = xssSanitizingFilter;
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
        this.samlAuthenticationSuccessHandler = samlAuthenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("Content-Security-Policy",
                                    "default-src 'self'; frame-ancestors 'none'; form-action 'self'");
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/admin/**").authenticated()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/jobs/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/saml2/**",
                                "/login/saml2/**",
                                "/api/v1/sso/tenant/*/login-urls",
                                "/.well-known/jwks.json",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/ws/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        }))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(pkceAuthorizationRequestResolver)
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
                .saml2Login(saml2 -> saml2
                        .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository)
                        .successHandler(samlAuthenticationSuccessHandler))
                .addFilterBefore(xssSanitizingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-API-Key"
        ));
        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
