package com.example.Amica.Config;

import com.example.Amica.Common.Result;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter implements Filter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        //先把接受的参数强转成Http格式
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        //标记特殊路径，以便放行，但目前先全部放行
        String path = req.getRequestURI();
        if (path.contains("/auth/")) {
            chain.doFilter(request, response);
            return;
        }
        //接收首部进行验证
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            //调用私有问题处理
            send401(res, "未登录");
            return;
        }
        String token = authHeader.substring(7);
        try {
            jwtUtil.validateToken(token);
        } catch (Exception e) {
            send401(res, "token无效或已过期");
            return;
        }
        long userId = jwtUtil.extractUserId(token);
        req.setAttribute("userId", userId);
        chain.doFilter(request, response);

    }

    //私有问题处理，返回JSON格式错误报告
    private void send401(HttpServletResponse res, String msg) throws IOException {
        Result<Void> result = Result.error(401, msg);
        String json = new ObjectMapper().writeValueAsString(result);
        res.setStatus(401);
        res.setContentType("application/json;charset=utf-8");
        res.getWriter().write(json);
    }

}
