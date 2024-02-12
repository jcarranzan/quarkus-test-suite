package io.quarkus.ts.http.http2;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GreetingServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(GreetingServlet.class);

    private static final String MESSAGE = "From the Web Servlet man";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        LOG.info(req.getSession().getId());
        PrintWriter writer = resp.getWriter();
        writer.write(MESSAGE);
        writer.close();
    }

}
