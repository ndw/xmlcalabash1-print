package com.xmlcalabash.util;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 6:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class FoFOP implements FoProcessor {
    XProcRuntime runtime = null;
    Properties options = null;
    XStep step = null;
    URIResolver resolver = null;

    private Class klass = null;
    private Method method = null;
    private String fopVersion = null;
    private Object fopFactory = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        this.options = options;

        // What version of FOP are we using?
        // Only 1.x and 2.x are supported!
        String className = "org.apache.fop.apps.FopFactory";
        try {
            klass = Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            klass = null;
        }

        if (klass != null) {
            try {
                method = klass.getMethod("newInstance");
                fopVersion = "1.x";
            } catch (NoSuchMethodException nsme) {
                // nop;
            }
        }

        if (fopVersion == null) {
            className = "org.apache.fop.apps.FopFactoryBuilder";
            try {
                klass = Class.forName(className);
                fopVersion = "2.x";
            } catch (ClassNotFoundException cnfe) {
                // nop
            }
        }

        if ("1.x".equals(fopVersion)) {
            initializeFop1x(runtime, step, options);
        } else if ("2.x".equals(fopVersion)) {
            initializeFop2x(runtime, step, options);
        } else {
            throw new XProcException("Failed to instantiate FOP 1.x or FOP 2.x");
        }
    }

    private void initializeFop1x(XProcRuntime runtime, XStep step, Properties options) {
        try {
            fopFactory = method.invoke(klass, (Object[]) null);
            Class fclass = fopFactory.getClass();

            resolver = runtime.getResolver();
            if (resolver != null) {
                method = fclass.getMethod("setURIResolver", URIResolver.class);
                method.invoke(fopFactory, resolver);
            }

            String s = getStringProp("BaseURL");
            if (s != null) {
                method = fclass.getMethod("setBaseURL", String.class);
                method.invoke(fopFactory, s);
            }

            Boolean b = getBooleanProp("BreakIndentInheritanceOnReferenceAreaBoundary");
            if (b != null) {
                method = fclass.getMethod("setBreakIndentInheritanceOnReferenceAreaBoundary", Boolean.class);
                method.invoke(fopFactory, b);
            }

            s = getStringProp("FontBaseURL");
            b = getBooleanProp("Base14KerningEnabled");
            if (s != null || b != null) {
                Method getFontManager = fclass.getMethod("getFontManager");
                Object fontManager = getFontManager.invoke(fopFactory);

                if (s != null) {
                    method = fontManager.getClass().getMethod("setFontBaseURL", String.class);
                    method.invoke(fontManager, s);
                }

                if (b != null) {
                    method = fontManager.getClass().getMethod("setBase14KerningEnabled", Boolean.class);
                    method.invoke(fontManager, b);
                }
            }

            s = getStringProp("HyphenBaseURL");
            if (s != null) {
                method = fclass.getMethod("setHyphenBaseURL", String.class);
                method.invoke(fopFactory, s);
            }

            s = getStringProp("PageHeight");
            if (s != null) {
                method = fclass.getMethod("setPageHeight", String.class);
                method.invoke(fopFactory, s);
            }

            s = getStringProp("PageWidth");
            if (s != null) {
                method = fclass.getMethod("setPageWidth", String.class);
                method.invoke(fopFactory, s);
            }

            Float f = getFloatProp("SourceResolution");
            if (f != null) {
                method = fclass.getMethod("setSourceResolution", Float.class);
                method.invoke(fopFactory, f);
            }

            f = getFloatProp("TargetResolution");
            if (f != null) {
                method = fclass.getMethod("setTargetResolution", Float.class);
                method.invoke(fopFactory, f);
            }

            b = getBooleanProp("StrictUserConfigValidation");
            if (b != null) {
                method = fclass.getMethod("setStrictUserConfigValidation", Boolean.class);
                method.invoke(fopFactory, b);
            }

            b = getBooleanProp("StrictValidation");
            if (b != null) {
                method = fclass.getMethod("setStrictUserConfigValidation", Boolean.class);
                method.invoke(fopFactory, b);
            }

            b = getBooleanProp("UseCache");
            if (b != null) {
                method = fclass.getMethod("getFontManager().setUseCache", Boolean.class);
                method.invoke(fopFactory, b);
            }

            s = getStringProp("UserConfig");
            if (s != null) {
                method = fclass.getMethod("setUserConfig", String.class);
                method.invoke(fopFactory, s);
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    private void initializeFop2x(XProcRuntime runtime, XStep step, Properties options) {
        Object fopFactoryBuilder = null;
        Constructor factBuilderConstructor = null;
        try {
            factBuilderConstructor = klass.getConstructor(URI.class);
        } catch (NoSuchMethodException nsme) {
            // nop;
        }

        resolver = runtime.getResolver();
        URI baseURI = step.getStep().getNode().getBaseURI();
        String s = getStringProp("BaseURL");
        if (s != null) {
            baseURI = baseURI.resolve(s);
        }

        try {
            if (resolver == null) {
                fopFactoryBuilder = factBuilderConstructor.newInstance(baseURI);
            } else {
                // FIXME: make an org.apache.xmlgraphics.io.ResourceResolver resolver!?
                fopFactoryBuilder = factBuilderConstructor.newInstance(baseURI);
            }
            Class fclass = fopFactoryBuilder.getClass();

            // FIXME: make this configurable
            Boolean b = false;
            /* Why doesn't this call work with reflection?
            method = fclass.getMethod("setStrictFOValidation", Boolean.class);
            method.invoke(fopFactoryBuilder, b);
            */

            b = getBooleanProp("BreakIndentInheritanceOnReferenceAreaBoundary");
            if (b != null) {
                method = fclass.getMethod("setBreakIndentInheritanceOnReferenceAreaBoundary", Boolean.class);
                method.invoke(fopFactoryBuilder, b);
            }

            Float f = getFloatProp("SourceResolution");
            if (f != null) {
                method = fclass.getMethod("setSourceResolution", Float.class);
                method.invoke(fopFactoryBuilder, f);
            }

            /* FIXME:
            s = getStringProp("FontBaseURL");
            if (s != null) {
                fopFactory.getFontManager().setFontBaseURL(s);
            }
            */

            b = getBooleanProp("Base14KerningEnabled");
            if (b != null) {
                Method getFontManager = fclass.getMethod("getFontManager");
                Object fontManager = getFontManager.invoke(fopFactoryBuilder);

                method = fontManager.getClass().getMethod("setBase14KerningEnabled", Boolean.class);
                method.invoke(fontManager, b);
            }

            /* FIXME:
            s = getStringProp("HyphenBaseURL");
            if (s != null) {
                fopFactory.setHyphenBaseURL(s);
            }
            */

            s = getStringProp("PageHeight");
            if (s != null) {
                method = fclass.getMethod("setPageHeight", String.class);
                method.invoke(fopFactoryBuilder, s);
            }

            s = getStringProp("PageWidth");
            if (s != null) {
                method = fclass.getMethod("setPageWidth", String.class);
                method.invoke(fopFactoryBuilder, s);
            }

            f = getFloatProp("TargetResolution");
            if (f != null) {
                method = fclass.getMethod("setTargetResolution", Float.class);
                method.invoke(fopFactoryBuilder, f);
            }

            b = getBooleanProp("StrictUserConfigValidation");
            if (b != null) {
                method = fclass.getMethod("setStrictUserConfigValidation", Boolean.class);
                method.invoke(fopFactoryBuilder, b);
            }

            b = getBooleanProp("StrictValidation");
            if (b != null) {
                method = fclass.getMethod("setStrictUserConfigValidation", Boolean.class);
                method.invoke(fopFactoryBuilder, b);
            }

            b = getBooleanProp("UseCache");
            if (b != null && !b) {
                Method getFontManager = fclass.getMethod("getFontManager");
                Object fontManager = getFontManager.invoke(fopFactoryBuilder);

                method = fontManager.getClass().getMethod("disableFontCache");
                method.invoke(fontManager);
            }

            /* FIXME:
            s = getStringProp("UserConfig");
            if (s != null) {
                fopFactory.setUserConfig(s);
            }
            */

            method = fclass.getMethod("build");
            fopFactory = method.invoke(fopFactoryBuilder);
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    public void format(XdmNode doc, OutputStream out, String contentType) {
        String outputFormat = null;
        if (contentType == null || "application/pdf".equalsIgnoreCase(contentType)) {
            outputFormat = "application/pdf"; // "PDF";
        } else if ("application/PostScript".equalsIgnoreCase(contentType)) {
            outputFormat = "application/postscript"; //"PostScript";
        } else if ("application/afp".equalsIgnoreCase(contentType)) {
            outputFormat =  "application/x-afp";  //"AFP";
        } else if ("application/rtf".equalsIgnoreCase(contentType)) {
            outputFormat = "application/rtf";
        } else if ("text/plain".equalsIgnoreCase(contentType)) {
           outputFormat = "text/plain";
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

        if (! ("1.x".equals(fopVersion) || "2.x".equals(fopVersion))) {
            throw new XProcException("Unexpected FOP version: " + fopVersion);
        }

        try {
            InputSource fodoc = S9apiUtils.xdmToInputSource(runtime, doc);
            SAXSource source = new SAXSource(fodoc);

            Object userAgent = null;
            Object fop = null;

            if ("1.x".equals(fopVersion)) {
                method = fopFactory.getClass().getMethod("newFop", String.class, OutputStream.class);
                fop = method.invoke(fopFactory, outputFormat, out);

                method = fop.getClass().getMethod("getUserAgent");
                userAgent = method.invoke(fop);
            } else {
                method = fopFactory.getClass().getMethod("newFOUserAgent");
                userAgent = method.invoke(fopFactory);
            }

            Class uaClass = userAgent.getClass();

            Boolean b = getBooleanProp("Accessibility");
            if (b != null) {
                method = uaClass.getMethod("setAccessibility", Boolean.class);
                method.invoke(userAgent, b);
            }

            String s = getStringProp("Author");
            if (s != null) {
                method = uaClass.getMethod("setAuthor", String.class);
                method.invoke(userAgent, s);
            }

            if ("1.x".equals(fopVersion)) {
                method = uaClass.getMethod("setBaseURL", String.class);
                method.invoke(userAgent, step.getNode().getBaseURI().toString());
                s = getStringProp("BaseURL");
                if (s != null) {
                    method.invoke(userAgent, s);
                }
            } else {
                // FIXME: how do I do this in 2.x?
            }

            b = getBooleanProp("ConserveMemoryPolicy");
            if (b != null) {
                method = uaClass.getMethod("setConserveMemoryPolicy", Boolean.class);
                method.invoke(userAgent, b);
            }

            s = getStringProp("CreationDate");
            if (s != null) {
                DateFormat df = DateFormat.getDateInstance();
                Date d = df.parse(s);
                method = uaClass.getMethod("setCreationDate", Date.class);
                method.invoke(userAgent, d);
            }

            s = getStringProp("Creator");
            if (s != null) {
                method = uaClass.getMethod("setCreator", String.class);
                method.invoke(userAgent, s);
            }

            s = getStringProp("Keywords");
            if (s != null) {
                method = uaClass.getMethod("setKeywords", String.class);
                method.invoke(userAgent, s);
            }

            b = getBooleanProp("LocatorEnabled");
            if (b != null) {
                method = uaClass.getMethod("setLocatorEnabled", Boolean.class);
                method.invoke(userAgent, b);
            }

            s = getStringProp("Producer");
            if (s != null) {
                method = uaClass.getMethod("setProducer", String.class);
                method.invoke(userAgent, s);
            }

            s = getStringProp("Subject");
            if (s != null) {
                method = uaClass.getMethod("setSubject", String.class);
                method.invoke(userAgent, s);
            }

            Float f = getFloatProp("TargetResolution");
            if (f != null) {
                method = uaClass.getMethod("setTargetResolution", Float.class);
                method.invoke(userAgent, f);
            }

            s = getStringProp("Title");
            if (s != null) {
                method = uaClass.getMethod("setTitle", String.class);
                method.invoke(userAgent, s);
            }

            if ("2.x".equals(fopVersion)) {
                method = uaClass.getMethod("newFop", String.class, OutputStream.class);
                fop = method.invoke(userAgent, outputFormat, out);
            }

            method = fop.getClass().getMethod("getDefaultHandler");
            Object defHandler = method.invoke(fop);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, new SAXResult((ContentHandler) defHandler));
        } catch (Exception e) {
            throw new XProcException(step.getNode(), "Failed to process FO document with FOP", e);
        }
    }

    private String getStringProp(String name) {
        return options.getProperty(name);
    }

    private Float getFloatProp(String name) {
        String s = getStringProp(name);
        if (s != null) {
            try {
                return Float.parseFloat(s);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBooleanProp(String name) {
        String s = options.getProperty(name);
        if (s != null) {
            return "true".equals(s);
        }
        return null;
    }
}
