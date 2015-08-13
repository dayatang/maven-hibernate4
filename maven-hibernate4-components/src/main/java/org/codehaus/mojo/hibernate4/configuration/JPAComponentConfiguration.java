package org.codehaus.mojo.hibernate4.configuration;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;

public class JPAComponentConfiguration
        extends AbstractComponentConfiguration {
// --------------------- Interface ComponentConfiguration ---------------------

    public String getName() {
        return "jpaconfiguration";
    }

    protected Configuration createConfiguration() {
        String persistenceUnit = getExporterMojo().getComponentProperty("persistenceunit");

        try {
            Configuration ejb3cfg = new Configuration();
            return ejb3cfg.configure();
        } catch (HibernateException he) {
            getExporterMojo().getLog().error(he.getMessage());
            return null;
        }
    }
}
