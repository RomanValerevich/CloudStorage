package ru.netology.cloudstorage.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.cloudstorage.service.AuthService;

import java.io.IOException;
import java.util.Collections;

public class JwtTokenFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public JwtTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        final String uri = request.getRequestURI();
        final String method = request.getMethod();
        final String authHeader = request.getHeader("auth-token");

        // Allow preflight requests and public auth endpoints
        if ("OPTIONS".equalsIgnoreCase(method)
                || "/login".equals(uri)
                || "/register".equals(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader != null && !authHeader.isEmpty()) {
            try {
                String header = authHeader.trim();
                String token = header;
                if (header.length() >= 7 && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    token = header.substring(7).trim();
                }
                String username = authService.validateTokenAndGetUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.emptyList());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing auth-token header");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
