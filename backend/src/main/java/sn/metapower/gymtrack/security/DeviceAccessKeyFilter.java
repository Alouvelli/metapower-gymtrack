package sn.metapower.gymtrack.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authentifie les requêtes provenant des tablettes de scan QR à l'accueil des salles.
 * Une clé partagée est envoyée dans l'en-tête {@code X-Device-Key}. Si elle correspond à
 * la valeur configurée, on injecte une Authentication portant le rôle ROLE_DEVICE.
 *
 * Ce filtre ne s'applique qu'à l'endpoint public /api/pointages/scan.
 */
@Component
public class DeviceAccessKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Device-Key";
    public static final String DEVICE_AUTHORITY = "ROLE_DEVICE";
    public static final String SCAN_PATH = "/api/pointages/scan";
    /** Vérification de la clé par l'écran de configuration de la tablette. */
    public static final String VERIF_PATH = "/api/pointages/device/verifier";

    @Value("${app.device-access-key}")
    private String configuredKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (SCAN_PATH.equals(uri) || VERIF_PATH.equals(uri)) {
            String provided = request.getHeader(HEADER);
            if (provided != null && configuredKey != null && provided.trim().equals(configuredKey.trim())) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                "device", null,
                                List.of(new SimpleGrantedAuthority(DEVICE_AUTHORITY)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
