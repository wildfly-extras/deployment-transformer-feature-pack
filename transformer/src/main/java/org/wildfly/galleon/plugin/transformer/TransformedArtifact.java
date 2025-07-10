/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.galleon.plugin.transformer;

import java.nio.file.Path;
/**
 *
 * @author jdenise
 */
public class TransformedArtifact {
    private final Path src;
    private final Path target;
    private final boolean transformed;
    private final boolean srcSigned;
    private final boolean unsigned;

    TransformedArtifact(Path src, Path target, boolean transformed, boolean srcSigned, boolean unsigned) {
        this.src = src;
        this.target = target;
        this.transformed = transformed;
        this.srcSigned = srcSigned;
        this.unsigned = unsigned;
    }

    /**
     * @return the src
     */
    public Path getSrc() {
        return src;
    }

    /**
     * @return the target
     */
    public Path getTarget() {
        return target;
    }

    /**
     * @return the transformed
     */
    public boolean isTransformed() {
        return transformed;
    }

    /**
     * @return the unsigned
     */
    public boolean isUnsigned() {
        return unsigned;
    }

    /**
     * @return the srcSigned
     */
    public boolean isSrcSigned() {
        return srcSigned;
    }

}
