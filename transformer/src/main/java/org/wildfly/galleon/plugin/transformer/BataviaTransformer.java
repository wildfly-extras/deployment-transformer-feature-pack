/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.galleon.plugin.transformer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.wildfly.extras.transformer.ArchiveTransformer;
import org.wildfly.extras.transformer.TransformerBuilder;
import org.wildfly.galleon.plugin.transformer.JakartaTransformer.LogHandler;
import org.wildfly.extras.transformer.TransformerFactory;

/**
 *
 * @author jdenise
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class BataviaTransformer {

    static TransformedArtifact transform(Path configsDir, Path src, Path target, boolean verbose, LogHandler log) throws IOException {
        boolean transformed;
        boolean signed = false;
        boolean unsigned = false;
        try {
            transformed = transform(configsDir != null ? configsDir.toString() : null, src.toString(), target.toString(), verbose);
            // TODO Only check for jar not for exploded for now
            if (src.getFileName().toString().endsWith(".jar")) {
                signed = JarUtils.isSignedJar(src);
            }
            if (transformed) {
                log.print("EE9: transformed %s", target.getFileName().toString());
            }
            if (signed && transformed) {
                log.print("WARNING: EE9: unsigning transformed %s ", target.getFileName().toString());
                JarUtils.unsign(target);
                unsigned = true;
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return new TransformedArtifact(src, target, transformed, signed, unsigned);
    }

    static boolean transform(final String configsDir, final String source, final String target, boolean verbose) throws Exception {
        final TransformerBuilder builder = TransformerFactory.getInstance().newTransformer();
        builder.setVerbose(verbose);
        if (configsDir != null) builder.setConfigsDir(configsDir);
        final ArchiveTransformer transformer = builder.build();
        return transformer.transform(new File(source), new File(target));
    }

}
