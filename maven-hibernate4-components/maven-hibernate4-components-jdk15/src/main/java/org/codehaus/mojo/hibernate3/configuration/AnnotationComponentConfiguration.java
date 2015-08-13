package org.codehaus.mojo.hibernate3.configuration;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.ReflectHelper;
import org.jboss.util.file.ArchiveBrowser;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class AnnotationComponentConfiguration extends AbstractComponentConfiguration {
	// ------------------------ INTERFACE METHODS ------------------------

	// --------------------- Interface ComponentConfiguration
	// ---------------------

	public String getName() {
		return "annotationconfiguration";
	}

	// -------------------------- OTHER METHODS --------------------------

	@SuppressWarnings("unchecked")
	protected Configuration createConfiguration() {
		// retrievethe Build object
		Build build = getExporterMojo().getProject().getBuild();
		Log log = getExporterMojo().getLog();

		// now create an empty arraylist that is going to hold our entities
		List<String> entities = new ArrayList<String>();
		try {
			if (getExporterMojo().getComponentProperty("scan-classes", false)) {
				File outputDirectory = new File(build.getOutputDirectory());
				entities.addAll(new PersistenceClassScanner(log).scanDir(outputDirectory));
				File testOutputDirectory = new File(build.getTestOutputDirectory());
				entities.addAll(new PersistenceClassScanner(log).scanDir(testOutputDirectory));
			}

			if (getExporterMojo().getComponentProperty("scan-jars", false)) {
				for (File jarFile : getJarFiles()) {
					entities.addAll(new PersistenceClassScanner(log).scanJar(jarFile));
				}
			}
		} catch (Exception e) {
			getExporterMojo().getLog().error(e.getMessage(), e);
			return null;
		}

		// now create the configuration object
		Configuration configuration = new Configuration();
		addNamedAnnotatedClasses(configuration, entities);
		return configuration;
	}

	private List<File> getJarFiles() {
		List<File> results = new ArrayList<File>();
		for (Artifact artifact : getExporterMojo().getProject().getArtifacts()) {
			results.add(artifact.getFile());
		}
		return results;
	}

	private void addNamedAnnotatedClasses(Configuration cfg, Collection<String> classNames) {
		for (String name : classNames) {
			try {
				Class<?> clazz = classForName(name);
				cfg.addAnnotatedClass(clazz);
			} catch (ClassNotFoundException cnfe) {
				Package pkg;
				try {
					pkg = classForName(name + ".package-info").getPackage();
				} catch (ClassNotFoundException e) {
					pkg = null;
				}
				if (pkg == null) {
					throw new PersistenceException(name + " class or package not found", cnfe);
				} else {
					cfg.addPackage(name);
				}
			}
		}
	}

	private Class<?> classForName(String className) throws ClassNotFoundException {
		return ReflectHelper.classForName(className, this.getClass());
	}

	protected void validateParameters() throws MojoExecutionException {
		super.validateParameters();
		if (getConfigurationFile() == null && !getExporterMojo().getComponentProperty("scan-classes", false)
				&& !getExporterMojo().getComponentProperty("scan-jars", false)) {
			throw new MojoExecutionException("No hibernate.cfg.xml configuration provided. "
					+ "Annotated classes/packages is only configurable via hibernate.cfg.xml");
		}
	}
}
