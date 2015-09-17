/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.swagger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.jaxrs.config.BeanConfig;
import org.apache.camel.model.rest.RestDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RestSwaggerApiDeclarationServlet extends HttpServlet {

    private Logger LOG = LoggerFactory.getLogger(RestSwaggerApiDeclarationServlet.class);

    // TODO: implement me
    //private RestSwaggerReader reader = new RestSwaggerReader();
    private BeanConfig swaggerConfig = new BeanConfig();
    private boolean cors;
    private volatile boolean initDone;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // configure swagger options
        String s = config.getInitParameter("swagger.version");
        if (s != null) {
            swaggerConfig.setVersion(s);
        }
        s = config.getInitParameter("base.path");
        if (s != null) {
            swaggerConfig.setBasePath(s);
        }
        s = config.getInitParameter("cors");
        if (s != null) {
            cors = "true".equalsIgnoreCase(s);
        }

        String title = config.getInitParameter("api.title");
        String description = config.getInitParameter("api.description");
        String termsOfServiceUrl = config.getInitParameter("api.termsOfServiceUrl");
        String contact = config.getInitParameter("api.contact");
        String license = config.getInitParameter("api.license");
        String licenseUrl = config.getInitParameter("api.licenseUrl");

        swaggerConfig.setTitle(title);
        swaggerConfig.setDescription(description);
        swaggerConfig.setTermsOfServiceUrl(termsOfServiceUrl);
        swaggerConfig.setContact(contact);
        swaggerConfig.setLicense(license);
        swaggerConfig.setLicenseUrl(licenseUrl);
    }

    public abstract List<RestDefinition> getRestDefinitions(String camelId) throws Exception;

    public abstract List<String> findCamelContexts() throws Exception;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (!initDone) {
            initBaseAndApiPaths(request);
        }

        String contextId;
        String route = request.getPathInfo();

        try {

            // render list of camel contexts as root
            if (route == null || route.equals("") || route.equals("/")) {
                renderCamelContexts(request, response);
            } else {
                // first part is the camel context
                if (route.startsWith("/")) {
                    route = route.substring(1);
                }
                // the remainder is the route part
                contextId = route.split("/")[0];
                if (route.startsWith(contextId)) {
                    route = route.substring(contextId.length());
                }


                // TODO: implement these
                if (!route.equals("") && !route.equals("/")) {
                    // render overview if the route is empty or is the root path
                    // renderApiDeclaration(request, response, contextId, route);
                } else {
                    // renderResourceListing(request, response, contextId);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error rendering swagger due " + e.getMessage());
        }
    }

    private void initBaseAndApiPaths(HttpServletRequest request) throws MalformedURLException {
        String base = swaggerConfig.getBasePath();
        if (base == null || !base.startsWith("http")) {
            // base path is configured using relative, so lets calculate the absolute url now we have the http request
            URL url = new URL(request.getRequestURL().toString());
            if (base == null) {
                base = "";
            }
            String path = translateContextPath(request);
            if (url.getPort() != 80 && url.getPort() != -1) {
                base = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + path + "/" + base;
            } else {
                base = url.getProtocol() + "://" + url.getHost() + request.getContextPath() + "/" + base;
            }
            swaggerConfig.setBasePath(base);
        }

        // TODO: api path?
        /*
        base = swaggerConfig.getApiPath
        if (base == null || !base.startsWith("http")) {
            // api path is configured using relative, so lets calculate the absolute url now we have the http request
            val url = new URL(request.getRequestURL.toString)
            if (base == null) {
                base = ""
            }
            val path = translateContextPath(request)
            if (url.getPort != 80 && url.getPort != -1) {
                base = url.getProtocol + "://" + url.getHost + ":" + url.getPort + path + "/" + base
            } else {
                base = url.getProtocol + "://" + url.getHost + request.getContextPath + "/" + base
            }
            swaggerConfig.setApiPath(base)
        } */
        initDone = true;
    }

    /**
     * We do only want the base context-path and not sub paths
     */
    private String translateContextPath(HttpServletRequest request) {
        String path = request.getContextPath();
        if (path.isEmpty() || path.equals("/")) {
            return "";
        } else {
            int idx = path.lastIndexOf("/");
            if (idx > 0) {
                return path.substring(0, idx);
            }
        }
        return path;
    }

    /**
     * Renders a list of available CamelContexts in the JVM
     */
    private void renderCamelContexts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOG.trace("renderCamelContexts");

        if (cors) {
            response.addHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
            response.addHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH");
            response.addHeader("Access-Control-Allow-Origin", "*");
        }

        List<String> contexts = findCamelContexts();
        response.getWriter().print("[\n");
        for (int i = 0; i < contexts.size(); i++) {
            String name = contexts.get(i);
            response.getWriter().print("{\"name\": \"" + name + "\"}");
            if (i < contexts.size() - 1) {
                response.getWriter().print(",\n");
            }
        }
        response.getWriter().print("\n]");
    }

}