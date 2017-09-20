package uk.ac.ic.wlgitbridge.server;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.Request;
import uk.ac.ic.wlgitbridge.application.config.Oauth2;
import uk.ac.ic.wlgitbridge.snapshot.base.ForbiddenException;
import uk.ac.ic.wlgitbridge.snapshot.getdoc.GetDocRequest;
import uk.ac.ic.wlgitbridge.util.Instance;
import uk.ac.ic.wlgitbridge.util.Log;
import uk.ac.ic.wlgitbridge.util.Util;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

/**
 * Created by winston on 25/10/15.
 */
public class Oauth2Filter implements Filter {

    public static final String ATTRIBUTE_KEY = "oauth2";

    private final Oauth2 oauth2;

    public Oauth2Filter(Oauth2 oauth2) {
        this.oauth2 = oauth2;
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    /**
     * The original request from git will not contain the Authorization header.
     *
     * So, for projects that need auth, we return 401. Git will swallow this
     * and prompt the user for user/pass, and then make a brand new request.
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain
    ) throws IOException, ServletException {
        String project = Util.removeAllSuffixes(
                ((Request) servletRequest).getRequestURI().split("/")[1],
                ".git"
        );
        Log.info("[{}] Checking if auth needed", project);
        GetDocRequest doc = new GetDocRequest(project);
        doc.request();
        try {
            doc.getResult();
        } catch (ForbiddenException e) {
            Log.info("[{}] Auth needed", project);
            getAndInjectCredentials(
                    project,
                    servletRequest,
                    servletResponse,
                    filterChain
            );
            return;
        }
        Log.info("[{}] Auth not needed", project);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    // TODO: this is ridiculous. Check for error cases first, then return/throw
    // TODO: also, use an Optional credential, since we treat it as optional
    private void getAndInjectCredentials(
            String projectName,
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain
    ) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String capturedUsername = "(unknown)";

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            Log.info("[{}] Authorization header present");
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();
                if (basic.equalsIgnoreCase("Basic")) {
                    try {
                        String credentials = new String(
                                Base64.decodeBase64(st.nextToken()),
                                "UTF-8"
                        );
                        String[] split = credentials.split(":");
                        if (split.length == 2) {
                            String username = split[0];
                            String password = split[1];
                            String accessToken = null;
                            if (username.length() > 0) {
                                capturedUsername = username;
                            }
                            try {
                                accessToken = new PasswordTokenRequest(
                                        Instance.httpTransport,
                                        Instance.jsonFactory,
                                        new GenericUrl(
                                                oauth2.getOauth2Server()
                                                        + "/oauth/token"
                                        ),
                                        username,
                                        password
                                ).setClientAuthentication(
                                        new ClientParametersAuthentication(
                                                oauth2.getOauth2ClientID(),
                                                oauth2.getOauth2ClientSecret()
                                        )
                                ).execute().getAccessToken();
                            } catch (TokenResponseException e) {
                                unauthorized(projectName, capturedUsername, e.getStatusCode(), request, response);
                                return;
                            }
                            final Credential cred = new Credential.Builder(
                                    BearerToken.authorizationHeaderAccessMethod(
                                    )
                            ).build();
                            cred.setAccessToken(accessToken);
                            servletRequest.setAttribute(ATTRIBUTE_KEY, cred);

                            filterChain.doFilter(
                                    servletRequest,
                                    servletResponse
                            );
                        } else {
                            unauthorized(projectName, capturedUsername, 0, request, response);
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new Error("Couldn't retrieve authentication", e);
                    }
                }
            }
        } else {
            unauthorized(projectName, capturedUsername, 0, request, response);
        }
    }

    @Override
    public void destroy() {}

    private void unauthorized(
            String projectName,
            String userName,
            int statusCode,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) throws IOException {
        Log.info(
            "[{}] Unauthorized, User '{}' status={} ip={}",
            projectName,
            userName,
            statusCode,
            servletRequest.getRemoteAddr()
        );
        HttpServletResponse response = servletResponse;
        response.setContentType("text/plain");
        response.setHeader("WWW-Authenticate", "Basic realm=\"Git Bridge\"");
        response.setStatus(401);

        PrintWriter w = response.getWriter();
        w.println(
                "Please sign in using your email address and Overleaf password."
        );
        w.println();
        w.println(
                "*Note*: if you sign in to Overleaf using another provider, "
                        + "such "
        );
        w.println(
                "as Google or Twitter, you need to set a password "
                        + "on your Overleaf "
        );
        w.println(
                "account first. "
                        + "Please see https://www.overleaf.com/blog/195 for "
        );
        w.println("more information.");
        w.close();
    }

}
