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
 * <p>StringTemplate Provider for Jersey</p>
 * <p/>
 * <p>You can configure the location of your templates with the
 * context param 'stringtemplate.template.path'. The default is set
 * to <tt>WEB-INF/templates</tt>.
 * The provider matches files with the extension ".st"
 * </p>
 * <p>Example of configuring the template path:</p>
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
 * <p>The provider puts the Viewable's model object the variable
 * "it" (as per Jersey's JSP provider), If the model is a Map the values
 * will be set directly into the template. The code assumes the Map is
 * a <tt>Map<String,Object></tt>.</p>
 * <p/>
 * <p>You'll also need to tell Jersey the package where this provider
 * is stored using the "com.sun.jersey.config.property.packages" property</p>
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

        // StringTemplate doesn't want the file extension, so don't send it back 
        final String relativeTemplatePathNoExtension = path.endsWith(EXTENSION) ? path.substring(0, path.length() - 3) : path;
        final String fullTemplatePathNoExtension = getTemplatesBasePath() + relativeTemplatePathNoExtension;
        boolean templateFound = false;

        try {
            templateFound = _servletContext.getResource(fullTemplatePathNoExtension + EXTENSION) != null;
        } catch (MalformedURLException e) {
            _theLog.warn("Malformed URL finding template [" + relativeTemplatePathNoExtension + "] from the servlet context", e);
        }

        if (templateFound) {
            return fullTemplatePathNoExtension;
        } else {
            _theLog.info("Template not found, path to resolve [" + path + "] context check path [" + fullTemplatePathNoExtension + EXTENSION + "]");
            return null;
        }
    }


    public void writeTo(String resolvedPath, final Object model, OutputStream out) throws IOException {
        if (_theLog.isDebugEnabled()) {
            _theLog.debug("Processing template [" + resolvedPath + "] with model of type " + (model == null ? "null" : model.getClass().getSimpleName()));
        }
        out.flush();
        
        final StringTemplate template = getInstanceOf(resolvedPath);
        if (_theLog.isDebugEnabled()) {
            _theLog.debug("OK: Resolved template [" + resolvedPath + "]");
        }

        final OutputStreamWriter writer = new OutputStreamWriter(out);
        template.setAttributes(loadModel(model));
        try {
            writer.write(template.toString());
            writer.flush();
            if (_theLog.isDebugEnabled()) {
                _theLog.debug("OK: Processed template [" + resolvedPath + "]");
            }
        }
        catch (Throwable t) {
            _theLog.error("Error processing template [" + resolvedPath + "] ", t);
            out.write("<pre class='template-err'>".getBytes());
            t.printStackTrace(new PrintStream(out));
            out.write("</pre>".getBytes());
        }
    }

    @Context
    public void setServletContext(final ServletContext context) {
        _servletContext = context;
        setTemplateBasePath(context);
        _theStringTemplateGroup = new WebInfCompatibleStringTemplateGroup(_servletContext);
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

    private String getTemplatesBasePath() {
        return _templatesBasePath;
    }

    private void setTemplatesBasePath(String _templatesBasePath) {
        this._templatesBasePath = _templatesBasePath;
    }

    private StringTemplate getInstanceOf(String resolvedPath) throws IOException {
        return _theStringTemplateGroup.getInstanceOf(resolvedPath);
    }

    private void setTemplateBasePath(ServletContext context) {
        setTemplatesBasePath(context.getInitParameter(STRINGTEMPLATE_TEMPLATE_PATH));
        if (getTemplatesBasePath() == null || "".equals(getTemplatesBasePath())) {
            _theLog.info("No '" + STRINGTEMPLATE_TEMPLATE_PATH + "' in context-param, defaulting to '" + WEB_INF_TEMPLATES + "'");
            setTemplatesBasePath(WEB_INF_TEMPLATES);
        }
    }

    private class WebInfCompatibleStringTemplateGroup extends StringTemplateGroup {

        private ServletContext _context;

        WebInfCompatibleStringTemplateGroup(ServletContext ctx) {
            super("templates", null, DefaultTemplateLexer.class);
            _context = ctx;
        }

        @Override
        protected StringTemplate loadTemplateFromBeneathRootDirOrCLASSPATH(String templateResourcePath) {
            StringTemplate template = null;
            BufferedReader bufferedReader = null;
            try {
                URL target = _context.getResource(templateResourcePath);
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
                        error("Cannot close connection for template [" + templateResourcePath + "]", inner);
                    }
                }
            }
            return template;
        }
    }
}
