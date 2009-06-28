/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dehora.jst.provider;

import com.sun.jersey.spi.template.TemplateProcessor;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import javax.servlet.ServletContext;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * StringTemplate Provider for Jersey
 * <p/>
 * <p>You can configure the location of your templates with the
 * context param 'stringtemplate.template.path'. The default is set
 * to <tt>WEB-INF/templates</tt>. The StringTemplateGroup is called 'stgrp'.
 * The provider matches files with the extension ".st"
 * </p>
 * Example of configuring the template path:
 * <p/>
 * <pre>
 * <web-app ...
 *    <display-name>StringTemplateProvider/display-name>
 *    <context-param>
 *       <param-name>stringtemplate.template.path</param-name>
 *       <param-value>/WEB-INF/pages</param-value>
 *   </context-param>
 *   ...
 * </pre>
 * <p/>
 * <p>You'll also need to tell Jersey the package where this provider
 * is stored using the "com.sun.jersey.config.property.packages" property</p>
 * <p/>
 * <p>The provider puts the Viewable's model object the variable
 * "it" (as per Jersey's JSP provider), If the model is a Map the values
 * will be set directly into the template. The code assumes the Map is
 * a <tt>Map<String,Object></tt>.</p>
 */

@Provider
public class StringTemplateProvider implements TemplateProcessor {

    private static final String STRINGTEMPLATE_TEMPLATE_PATH = "stringtemplate.template.path";
    private static final String WEB_INF_TEMPLATES = "/WEB-INF/templates";
    private static final String EXTENSION = ".st";
    private static final Logger _theLog = Logger.getLogger(StringTemplateProvider.class);
    private static StringTemplateGroup _theStringTemplateGroup;

    private ServletContext _servletContext;
    private String _templatesBasePath;

    public StringTemplateProvider() {
    }

    public String resolve(final String path) {
        if (_theLog.isDebugEnabled()) {
            _theLog.debug("Resolving template path [" + path + "]");
        }
        // ST doesn't want the file extension
        final String relativeTemplatePath = path.endsWith(EXTENSION) ? path.substring(0, path.length() - 3) : path;
        try {
            final String fullTemplatePath = getTemplatesBasePath() + relativeTemplatePath;
            final boolean templateFound = _servletContext.getResource(fullTemplatePath + EXTENSION) != null;
            if (!templateFound) {
                _theLog.info("Template not found [path: " + path + "] [context path: " + fullTemplatePath + "]");
                return null;
            } else {
                return fullTemplatePath;
            }
        }
        catch (MalformedURLException e) {
            _theLog.warn("Malformed URL in finding template [" + relativeTemplatePath + "] from the servlet context: " + e.getMessage());
            return null;
        }
    }


    public void writeTo(String resolvedPath, final Object model, OutputStream out) throws IOException {
        if (_theLog.isDebugEnabled()) {
            _theLog.debug("Processing template [" + resolvedPath + "] with model of type " + (model == null ? "null" : model.getClass().getSimpleName()));
        }
        out.flush();
        final StringTemplate template = getTemplateFor(resolvedPath);
        if (_theLog.isDebugEnabled()) {
            _theLog.debug("OK: Resolved template [" + resolvedPath + "]");
        }
        final OutputStreamWriter writer = new OutputStreamWriter(out);
        final Map<String, Object> templateModel = loadModel(model);
        template.setAttributes(templateModel);
        try {
            writer.write(template.toString());
            writer.flush();
            if (_theLog.isDebugEnabled()) {
                _theLog.debug("OK: Processed template [" + resolvedPath + "]");
            }
        }
        catch (Throwable t) {
            _theLog.error("Error processing template [" + resolvedPath + "] " + t.getMessage(), t);
            out.write("<pre class='template-err'>".getBytes());
            t.printStackTrace(new PrintStream(out));
            out.write("</pre>".getBytes());
        }
    }

    @SuppressWarnings({"unchecked"})
    private Map<String, Object> loadModel(final Object model) {
        final Map<String, Object> templateModel;
        if (model instanceof Map) {
            templateModel = new HashMap<String, Object>((Map<String, Object>) model);
        } else {
            templateModel = new HashMap<String, Object>() {{
                put("it", model);
            }};
        }
        return templateModel;
    }

    private StringTemplate getTemplateFor(String resolvedPath) throws IOException {
        return _theStringTemplateGroup.getInstanceOf(resolvedPath);
    }

    @Context
    public void setServletContext(final ServletContext context) {
        this._servletContext = context;
        setTemplateBasePath(context);
        _theStringTemplateGroup = new WebInfCompatibleStringTemplateGroup(_servletContext);
    }

    private void setTemplateBasePath(ServletContext context) {
        setTemplatesBasePath(context.getInitParameter(STRINGTEMPLATE_TEMPLATE_PATH));
        if (getTemplatesBasePath() == null || "".equals(getTemplatesBasePath())) {
            _theLog.info("No '" + STRINGTEMPLATE_TEMPLATE_PATH + "' in context-param, defaulting to '" + WEB_INF_TEMPLATES + "'");
            setTemplatesBasePath(WEB_INF_TEMPLATES);
        }
    }

    public String getTemplatesBasePath() {
        return _templatesBasePath;
    }

    public void setTemplatesBasePath(String _templatesBasePath) {
        this._templatesBasePath = _templatesBasePath;
    }


    class WebInfCompatibleStringTemplateGroup extends StringTemplateGroup {

        private ServletContext _context;

        public WebInfCompatibleStringTemplateGroup(ServletContext ctx) {
            super("templates", null, DefaultTemplateLexer.class);
            _context = ctx;
        }

        @Override
        protected StringTemplate loadTemplateFromBeneathRootDirOrCLASSPATH(String templateResourcePath) {
            StringTemplate template = null;
            BufferedReader bufferedReader = null;
            try {
                URL target  = _context.getResource(templateResourcePath);
                InputStream inputStream = target.openStream();
                InputStreamReader inputStreamReader = getInputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                template = loadTemplate(name, bufferedReader);
                bufferedReader.close();
                bufferedReader = null;
            } catch (MalformedURLException e) {
                error("Malformed URL in finding template[" + templateResourcePath + "] from the servlet context" + e.getMessage(), e);
            } catch (IOException e) {
                error("Can't load [" + templateResourcePath + "] from the servlet context", e);
            }
            finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException inner) {
                        error("Cannot close template connection: " + templateResourcePath);
                    }
                }
            }
            return template;
        }
    }
}
