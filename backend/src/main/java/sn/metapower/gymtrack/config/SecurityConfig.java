package sn.metapower.gymtrack.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sn.metapower.gymtrack.security.DeviceAccessKeyFilter;
import sn.metapower.gymtrack.services.MyUserDetailService;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final MyUserDetailService myUserDetailService;
    private final DeviceAccessKeyFilter deviceAccessKeyFilter;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    public SecurityConfig(MyUserDetailService myUserDetailService,
                          DeviceAccessKeyFilter deviceAccessKeyFilter) {
        this.myUserDetailService = myUserDetailService;
        this.deviceAccessKeyFilter = deviceAccessKeyFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // ── Converter JWT : scope "ROLE_ADMIN" → authority "ROLE_ADMIN" (sans préfixe SCOPE_) ──
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("");           // supprime le préfixe "SCOPE_"
        authoritiesConverter.setAuthoritiesClaimName("scope"); // lit le claim "scope"
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(ar -> ar
                        // Auth public
                        .requestMatchers("/auth/**").permitAll()
                        // Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Actuator health
                        .requestMatchers("/actuator/health").permitAll()
                        // Endpoint scan QR — DeviceAccessKeyFilter doit avoir injecté ROLE_DEVICE
                        // (via header X-Device-Key). Sans la clé valide, accès refusé.
                        .requestMatchers("/api/pointages/scan", "/api/pointages/device/verifier").hasAuthority("ROLE_DEVICE")
                        // Photos membres et reçus tickets en lecture publique
                        .requestMatchers("/files/view/**", "/files/download/**").permitAll()
                        // Ticket public (page WhatsApp du client, QR, reçu PDF) — par token UUID,
                        // jamais par id séquentiel (sinon on récupère les QR valides des autres)
                        .requestMatchers(HttpMethod.GET, "/api/public/tickets/**").permitAll()
                        // Salles actives (id + nom) pour configurer le scanner tablette sans session
                        .requestMatchers(HttpMethod.GET, "/api/public/sites").permitAll()
                        // QR code membres — chargé via <img src> (pas de JWT possible)
                        .requestMatchers("/api/membres/*/qrcode").permitAll()
                        // Tout le reste : JWT requis
                        .anyRequest().authenticated()
                )
                .addFilterBefore(deviceAccessKeyFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .oauth2ResourceServer(oa -> oa.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));

        return http.build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey.getBytes()));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(myUserDetailService).passwordEncoder(passwordEncoder());
        return builder.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(List.of(
                appBaseUrl,
                "http://localhost:4200",
                "http://localhost:4300",
                "http://192.168.*.*:4200",   // réseau local WiFi — tablette
                "http://192.168.*.*:8090"    // accès direct API depuis réseau local
        ));
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cors.addAllowedHeader("*");
        cors.setExposedHeaders(List.of("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
