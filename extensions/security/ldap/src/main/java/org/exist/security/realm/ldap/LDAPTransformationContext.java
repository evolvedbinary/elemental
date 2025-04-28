/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.security.realm.ldap;

import java.util.ArrayList;
import java.util.List;

import org.exist.config.Configurable;
import org.exist.config.Configuration;
import org.exist.config.Configurator;
import org.exist.security.realm.TransformationContext;
import org.exist.config.annotation.ConfigurationClass;
import org.exist.config.annotation.ConfigurationFieldAsElement;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam retter</a>
 */
@ConfigurationClass("transformation")
public class LDAPTransformationContext implements TransformationContext, Configurable {

    @ConfigurationFieldAsElement("add-group")
    //protected List<String> addGroup = new ArrayList<String>();
    protected String addGroup; //TODO convert to list

    private final Configuration configuration;

    public LDAPTransformationContext(final Configuration config) {
        this.configuration = Configurator.configure(this, config);
    }

    @Override
    public List<String> getAdditionalGroups() {
        final List<String> additionalGroups = new ArrayList<>();
        additionalGroups.add(addGroup);
        return additionalGroups;
    }

    @Override
    public boolean isConfigured() {
        return (configuration != null);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}