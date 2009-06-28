package net.dehora.jst

import com.sun.jersey.spi.template.TemplateProcessor;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

@Provider
public class StringTemplateProvider implements TemplateProcessor
{
    private static final Logger _theLog = Logger.getLogger(StringTemplateProvider.class);
    private static StringTemplateGroup _theStringTemplateGroup;
    private ServletContext _servletContext;
    private String _templatesBasePath;
    private static final String EXTENSION = "st";

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
            _theLog.warn("MalformedURLException finding template [" + filePath + "] from the servlet context: " + e.getMessage());
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
            out.write("<pre>".getBytes());
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
        _templatesBasePath = context.getInitParameter("stringtemplate.template.path");
        if (StringUtils.isBlank(_templatesBasePath)) {
            _theLog.info("No 'stringtemplate.template.path' in context-param, defaulting to '/WEB-INF/templates'");
            _templatesBasePath = "/WEB-INF/templates";
        }
        _templatesBasePath = _templatesBasePath.replaceAll("/$", "");
        _theStringTemplateGroup = new StringTemplateGroup("stgrp", _templatesBasePath, DefaultTemplateLexer.class);
    }
}
