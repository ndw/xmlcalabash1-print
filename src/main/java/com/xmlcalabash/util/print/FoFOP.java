package com.xmlcalabash.util.print;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.XdmNode;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.OutputStream;
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

    private FopFactory fopFactory = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        this.options = options;

        // Only FOP 2.x is supported...

        resolver = runtime.getResolver();
        URI baseURI = step.getStep().getNode().getBaseURI();
        String s = getStringProp("BaseURL");
        if (s != null) {
            baseURI = baseURI.resolve(s);
        }

        FopFactoryBuilder fopBuilder = null;

        try {
            s = getStringProp("UserConfig");
            if (s != null) {
                DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
                Configuration cfg = cfgBuilder.buildFromFile(new File(s));
                fopBuilder = new FopFactoryBuilder(baseURI).setConfiguration(cfg);
            } else {
                fopBuilder = new FopFactoryBuilder(baseURI);
            }

            Boolean b = getBooleanProp("StrictFOValidation");
            if (b != null) {
                fopBuilder.setStrictFOValidation(b);
            }

            b = getBooleanProp("BreakIndentInheritanceOnReferenceAreaBoundary");
            if (b != null) {
                fopBuilder.setBreakIndentInheritanceOnReferenceAreaBoundary(b);
            }

            Float f = getFloatProp("SourceResolution");
            if (f != null) {
                fopBuilder.setSourceResolution(f);
            }

            b = getBooleanProp("Base14KerningEnabled");
            if (b != null) {
                fopBuilder.getFontManager().setBase14KerningEnabled(b);
            }

            s = getStringProp("PageHeight");
            if (s != null) {
                fopBuilder.setPageHeight(s);
            }

            s = getStringProp("PageWidth");
            if (s != null) {
                fopBuilder.setPageWidth(s);
            }

            f = getFloatProp("TargetResolution");
            if (f != null) {
                fopBuilder.setTargetResolution(f);
            }

            b = getBooleanProp("StrictUserConfigValidation");
            if (b != null) {
                fopBuilder.setStrictUserConfigValidation(b);
            }

            // Backwards compatability with StrictUserConfigValidation?
            b = getBooleanProp("StrictValidation");
            if (b != null) {
                fopBuilder.setStrictUserConfigValidation(b);
            }

            b = getBooleanProp("UseCache");
            if (b != null && !b) {
                fopBuilder.getFontManager().disableFontCache();
            }

            fopFactory = fopBuilder.build();
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

        try {
            InputSource fodoc = S9apiUtils.xdmToInputSource(runtime, doc);
            SAXSource source = new SAXSource(fodoc);

            FOUserAgent userAgent = fopFactory.newFOUserAgent();

            Class uaClass = userAgent.getClass();

            Boolean b = getBooleanProp("Accessibility");
            if (b != null) {
                userAgent.setAccessibility(b);
            }

            String s = getStringProp("Author");
            if (s != null) {
                userAgent.setAuthor(s);
            }

            b = getBooleanProp("ConserveMemoryPolicy");
            if (b != null) {
                userAgent.setConserveMemoryPolicy(b);
            }

            s = getStringProp("CreationDate");
            if (s != null) {
                DateFormat df = DateFormat.getDateInstance();
                Date d = df.parse(s);
                userAgent.setCreationDate(d);
            }

            s = getStringProp("Creator");
            if (s != null) {
                userAgent.setCreator(s);
            }

            s = getStringProp("Keywords");
            if (s != null) {
                userAgent.setKeywords(s);
            }

            b = getBooleanProp("LocatorEnabled");
            if (b != null) {
                userAgent.setLocatorEnabled(b);
            }

            s = getStringProp("Producer");
            if (s != null) {
                userAgent.setProducer(s);
            }

            s = getStringProp("Subject");
            if (s != null) {
                userAgent.setSubject(s);
            }

            Float f = getFloatProp("TargetResolution");
            if (f != null) {
                userAgent.setTargetResolution(f);
            }

            s = getStringProp("Title");
            if (s != null) {
                userAgent.setTitle(s);
            }

            Fop fop = userAgent.newFop(outputFormat, out);
            DefaultHandler defHandler = fop.getDefaultHandler();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, new SAXResult(defHandler));
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
