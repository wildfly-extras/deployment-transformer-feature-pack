/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ee8to9.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.jboss.logging.Logger;
import org.wildfly.galleon.plugin.transformer.JakartaTransformer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * WildFly Core DeploymentTransformer implementation that uses the WF Galleon integration with Eclipse Transformer to
 * transform.
 */
public final class DeploymentTransformer implements org.jboss.as.server.deployment.transformation.DeploymentTransformer {

    private static final Logger logger = Logger.getLogger(DeploymentTransformer.class.getPackage().getName());

    public InputStream transform(InputStream in, String name) throws IOException {
        final boolean verbose = logger.isTraceEnabled();
        if (isEnabled()) {
            // first parameter represents external configs directory - null indicates use provided transformation defaults
            // The name captures the type of file.
            return JakartaTransformer.transform(null, in, name, verbose, new JakartaTransformer.LogHandler() {
                @Override
                public void print(String format, Object... args) {
                    logger.tracef(format, args);
                }
            });
        }
        logger.tracef("Skipping processing of %s", name);
        return in;
    }

    public void transform(Path src, Path target) throws IOException {
        // no-op initially
        /*
        final boolean verbose = logger.isTraceEnabled();
        JakartaTransformer.transform(src, target, verbose, new JakartaTransformer.LogHandler() {
            @Override
            public void print(String format, Object... args) {
                logger.tracef(format, args);
            }
        });
        */
    }

    private boolean isEnabled() {
        final String value = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.unsupported.skip.jakarta.transformer", "false");
        return !Boolean.parseBoolean(value);
    }
}
