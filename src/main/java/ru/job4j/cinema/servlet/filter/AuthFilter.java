package ru.job4j.cinema.servlet.filter;

import ru.job4j.cinema.store.Store;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) sreq;
        HttpServletResponse resp = (HttpServletResponse) sresp;
        if(req.getMethod().equalsIgnoreCase("POST")) {
            Store store = (Store) req.getServletContext().getAttribute("store");
            if (req.getSession().getAttribute("customerId") == null) {
                int customerId = store.addCustomer();
                if (customerId != -1) {
                    req.getSession().setAttribute("customerId", customerId);
                } else {
                    resp.sendRedirect(req.getContextPath() + "/index.html");
                    return;
                }
            } else if (req.getParameter("customerName") != null && req.getParameter("phone") != null) {
                store.updateCustomer((int) req.getSession().getAttribute("customerId"),
                        req.getParameter("customerName"), req.getParameter("phone"));
            }
        }
        chain.doFilter(sreq, sresp);
    }

    @Override
    public void destroy() {
    }
}
