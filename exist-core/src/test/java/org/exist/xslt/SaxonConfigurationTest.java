/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.xslt;

import net.sf.saxon.s9api.Processor;
import org.exist.storage.BrokerPool;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.Configuration;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:alan@evolvedbinary.com">Alan Paxton</a>
 */
public class SaxonConfigurationTest {

  @ClassRule
  public static final ExistEmbeddedServer EXIST_EMBEDDED_SERVER = new ExistEmbeddedServer(true, true);

  @Test
  public void configFromBroker() {
    final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();

    final Configuration elementalConfiguration = brokerPool.getConfiguration();
    assertThat(elementalConfiguration.getProperty("saxon.configuration")).isEqualTo("saxon-config.xml");

    final SaxonConfiguration saxonConfiguration = SaxonConfiguration.getConfiguration(elementalConfiguration, null);

    // There is no way to install EE at the test/build phase.
    // Sanity check is to confirm this does indeed return "HE" (Home Edition).
    final Processor saxonProcessor = saxonConfiguration.getProcessor();
    assertThat(saxonProcessor.getSaxonEdition()).isEqualTo("HE");

    final SaxonConfiguration saxonConfiguration2 = SaxonConfiguration.getConfiguration(elementalConfiguration, null);
    assertThat(saxonConfiguration2).isSameAs(saxonConfiguration);
  }
}
