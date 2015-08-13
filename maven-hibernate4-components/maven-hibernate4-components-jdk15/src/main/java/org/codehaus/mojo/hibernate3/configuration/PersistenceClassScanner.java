package org.codehaus.mojo.hibernate3.configuration;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import org.apache.maven.plugin.logging.Log;
import org.jboss.util.file.ArchiveBrowser;
import org.jboss.util.file.DirectoryArchiveBrowser;
import org.jboss.util.file.JarArchiveBrowser;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by yyang on 15/8/13.
 */
public class PersistenceClassScanner {

    private Log log;

    private ArchiveBrowser.Filter filter = new ArchiveBrowser.Filter() {
        public boolean accept(String filename) {
            return filename.endsWith(".class");
        }
    };

    public PersistenceClassScanner(Log log) {
        this.log = log;
    }

    public List<String> scanDir(String dir) {
        return scanDir(new File(dir));
    }

    public List<String> scanDir(File dir) {
        if (!dir.exists()) {
            throw new IllegalArgumentException("Path '" + dir + "' not exists!");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Path '" + dir + "' is not a directory!");
        }
        if (!(dir.canRead() && dir.canExecute())) {
            throw new SecurityException("Path '" + dir + "' not need excute permission!");
        }
        log.debug("[scanDirForClasses] " + dir);
        return getClasses(new DirectoryArchiveBrowser(dir, filter));
    }

    public List<String> scanJar(File jarFile) {
        if (jarFile == null) {
            throw new IllegalArgumentException("Jar is null!");
        }
        log.debug("[scanJarForClasses] " + jarFile);
        return getClasses(new JarArchiveBrowser(jarFile, filter));
    }

    private List<String> getClasses(Iterator<InputStream> files) {
        List<String> entities = new ArrayList<String>();
        while (files.hasNext()) {
            InputStream stream = files.next();
            DataInputStream dstream = new DataInputStream(stream);
            ClassFile classFile = null;
            try {
                try {
                    classFile = new ClassFile(dstream);
                } finally {
                    dstream.close();
                    stream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            AnnotationsAttribute visible = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.visibleTag);
            if (visible != null) {
                boolean isEntity = visible.getAnnotation(Entity.class.getName()) != null;
                if (isEntity) {
                    log.info("found EJB3 Entity bean: " + classFile.getName());
                    entities.add(classFile.getName());
                }
                boolean isEmbeddable = visible.getAnnotation(Embeddable.class.getName()) != null;
                if (isEmbeddable) {
                    log.info("found EJB3 @Embeddable: " + classFile.getName());
                    entities.add(classFile.getName());
                }
                boolean isEmbeddableSuperclass = visible.getAnnotation(MappedSuperclass.class.getName()) != null;
                if (isEmbeddableSuperclass) {
                    log.info("found EJB3 @MappedSuperclass: " + classFile.getName());
                    entities.add(classFile.getName());
                }
            }
        }
        return entities;
    }
}
