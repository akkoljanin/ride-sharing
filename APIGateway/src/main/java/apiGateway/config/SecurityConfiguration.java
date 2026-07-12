package apiGateway.config;

import api.dtos.UserDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange

                        // Public endpoints
                        .pathMatchers(HttpMethod.POST, "/users/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/admin").permitAll()
                        .pathMatchers(HttpMethod.GET,  "/users/email").permitAll()

                        // User management – ADMIN only
                        .pathMatchers(HttpMethod.GET,    "/users/**").hasAnyRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT,    "/users/**").hasAnyRole("ADMIN", "RIDER", "DRIVER")
                        .pathMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")

                        // Driver management
                        .pathMatchers(HttpMethod.GET,  "/drivers/**").hasAnyRole("ADMIN", "RIDER", "DRIVER")
                        .pathMatchers(HttpMethod.POST, "/drivers/**").hasAnyRole("ADMIN", "DRIVER")
                        .pathMatchers(HttpMethod.PUT,  "/drivers/**").hasAnyRole("ADMIN", "DRIVER")
                        .pathMatchers(HttpMethod.DELETE, "/drivers/**").hasRole("ADMIN")

                        // Rides
                        .pathMatchers(HttpMethod.POST, "/rides").hasRole("RIDER")
                        .pathMatchers(HttpMethod.PUT,  "/rides/*/accept").hasRole("DRIVER")
                        .pathMatchers(HttpMethod.PUT,  "/rides/*/complete").hasRole("DRIVER")
                        .pathMatchers(HttpMethod.PUT,  "/rides/*/cancel").hasAnyRole("RIDER", "DRIVER", "ADMIN")
                        .pathMatchers(HttpMethod.GET,  "/rides/**").hasAnyRole("RIDER", "DRIVER", "ADMIN")

                        // Pricing – read only for everyone
                        .pathMatchers(HttpMethod.GET, "/pricing/**").hasAnyRole("RIDER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/pricing/**").hasAnyRole("RIDER", "ADMIN")

                        // Notifications
                        .pathMatchers("/notifications/**").hasAnyRole("RIDER", "DRIVER", "ADMIN")

                        .anyExchange().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService(
            WebClient.Builder webClientBuilder, BCryptPasswordEncoder encoder) {

        WebClient client = webClientBuilder
                .baseUrl("http://users-service:8770").build();

        return username -> client.get()
                .uri(uri -> uri.path("/users/email").queryParam("email", username).build())
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        resp -> Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .bodyToMono(UserDto.class)
                .map(dto -> User.withUsername(dto.getEmail())
                        .password(encoder.encode(dto.getPassword()))
                        .roles(dto.getRole())
                        .build());
    }

    // Forward authenticated user identity to downstream services
    @Bean
    public WebFilter userHeaderFilter() {
        return (exchange, chain) -> exchange.getPrincipal()
                .cast(org.springframework.security.core.Authentication.class)
                .flatMap(auth -> {
                    String role  = auth.getAuthorities().iterator().next().getAuthority();
                    String email = auth.getName();
                    var mutated  = exchange.getRequest().mutate()
                            .header("X-User-Role",  role)
                            .header("X-User-Email", email)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
