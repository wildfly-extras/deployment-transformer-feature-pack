/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.quickstarts.kitchensink_ear.util;

import static javax.faces.annotation.FacesConfig.Version;

import javax.faces.annotation.FacesConfig;

/**
 * This class uses CDI to alias Jakarta EE resources, such as the persistence context, to CDI beans
 *
 * <p>
 * Example injection on a managed bean field:
 * </p>
 *
 * <pre>
 * &#064;Inject
 * private EntityManager em;
 * </pre>
 */
@FacesConfig(
    // Activates CDI built-in beans that provide the injection this project uses
    version = Version.JSF_2_3
)
public class WebResources {

}
