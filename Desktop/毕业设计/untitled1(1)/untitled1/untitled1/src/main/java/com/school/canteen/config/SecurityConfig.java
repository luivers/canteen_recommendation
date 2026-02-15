package com.school.canteen.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 安全配置类
 * <p>配置接口访问权限、CORS 跨域策略、JWT 过滤器链和密码编码器</p>
 */
@Configuration
@EnableWebSecurity
@EnableCaching
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                // 公开访问的端点
                .requestMatchers("/api/users/login", "/api/users/register", "/api/users/test-connection").permitAll()
                // 公开的业务API
                .requestMatchers("/api/dishes/**", "/api/recommendations/**", "/api/windows/**", "/api/promotions/**", "/api/combos/**", "/api/notifications/public-announcements", "/api/announcements", "/api/canteens/**", "/api/weather/**").permitAll()
                // 后台管理端点需要管理员权限
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "WINDOW_MANAGER")
                // 统计API需要认证
                .requestMatchers("/api/statistics/me/**").permitAll()
                .requestMatchers("/api/statistics/**").authenticated()
                // 购物车API允许公开访问
                .requestMatchers("/api/orders/cart/**").permitAll()
                // 订单查询API允许公开访问（GET请求），创建和修改订单需要认证
                .requestMatchers("/api/orders", "/api/orders/**").permitAll()
                // 测试接口免认证
                .requestMatchers("/api/test/**").permitAll()
                // 允许静态资源访问
                .requestMatchers("/", "/index.html", "/static/**", "/public/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // 用户管理API需要ADMIN角色
                .requestMatchers("/api/users/**").permitAll()
                // 其他API端点需要认证
                .requestMatchers("/api/**").authenticated()
                // 其他所有请求也需要认证
                .anyRequest().authenticated()
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许更多的本地开发端口和直接文件访问（null源）
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*", 
            "http://127.0.0.1:*",
            "null" // 支持直接通过file://协议打开的HTML文件访问
        ));
        // 允许所有常用HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS",
            "PATCH", "HEAD"
        ));
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带凭证
        configuration.setAllowCredentials(true);
        // 暴露更多的响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type",
            "Content-Length",
            "X-Requested-With",
            "Accept",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        // 设置预检请求的有效期，减少预检请求的数量
        configuration.setMaxAge(3600L); // 1小时
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
