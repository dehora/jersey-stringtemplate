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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * StringTemplate Provider for Jersey
 *
 * <p>You can configure the location of your templates with the
 * context param 'stringtemplate.template.path'. The default is set
 * to <tt>WEB-INF/templates</tt>. The StringTemplateGroup is called 'stgrp'.
 * The provider matches files with the extension ".st"
 * </p>
 * Example of configuring the template path:
 *
 * <pre>
 * <web-app ...
 *    <display-name>StringTemplateProvider/display-name>
 *    <context-param>
 *       <param-name>stringtemplate.template.path</param-name>
 *       <param-value>/WEB-INF/pages</param-value>
 *   </context-param>
 *   ...
 *</pre>
 *
 * <p>You'll also need to tell Jersey the package where this provider
 * is stored using the "com.sun.jersey.config.property.packages" property</p>
 *
 * <p>The provider puts the Viewable's model object the variable
 * "it" (as per Jersey's JSP provider), If the model is a Map. If so, the values
 * will be set directly into the template. The code assumes the map is of
 * a <tt>Map<String,Object></tt>.</p>
 *
 */

@Provider
public class StringTemplateProvider implements TemplateProcessor
{
    private static final String STRINGTEMPLATE_TEMPLATE_PATH = "stringtemplate.template.path";
    private static final String WEB_INF_TEMPLATES = "/WEB-INF/templates";
    private static final String EXTENSION = "st";
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
        final String filePath = path.endsWith(EXTENSION) ? path : path + "." + EXTENSION;
        try {
            final String fullPath = _templatesBasePath + filePath;
            final boolean templateFound = _servletContext.getResource(fullPath) != null;
            if (!templateFound) {
                _theLog.warn("Template not found [path: " + path + "] [context path: " + fullPath + "]");
            }
            return templateFound ? filePath : null;
        }
        catch (MalformedURLException e) {
            _theLog.warn("Malformed URL in finding template [" + filePath + "] from the servlet context: " + e.getMessage());
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
            templateModel = new HashMap<String, Object>()
            {{
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
        _templatesBasePath = context.getInitParameter(STRINGTEMPLATE_TEMPLATE_PATH);
        if (_templatesBasePath== null || "".equals(_templatesBasePath)) {
            _theLog.info("No '" + STRINGTEMPLATE_TEMPLATE_PATH + "' in context-param, defaulting to '" + WEB_INF_TEMPLATES + "'");
            _templatesBasePath = WEB_INF_TEMPLATES;
        }
        _templatesBasePath = _templatesBasePath.replaceAll("/$", "");
        _theStringTemplateGroup = new StringTemplateGroup("stgrp", _templatesBasePath, DefaultTemplateLexer.class);
    }
}
