package com.bmart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final com.bmart.repository.JwtTokenRepository jwtTokenRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Check if token exists in database (not deleted by logout)
                if (jwtTokenRepository.count() > 0 && jwtTokenRepository.findByToken(jwt).isEmpty()) {
                    logger.info("JWT token has been revoked/deleted via logout.");
                    filterChain.doFilter(request, response);
                    return;
                }
                io.jsonwebtoken.Claims claims = tokenProvider.getClaimsFromJWT(jwt);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                UserDetails userDetails;
                if (StringUtils.hasText(role)) {
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    userDetails = new org.springframework.security.core.userdetails.User(
                            username,
                            "",
                            java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority))
                    );
                } else {
                    userDetails = customUserDetailsService.loadUserByUsername(username);
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
