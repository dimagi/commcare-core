package org.javarosa.core.model;

import org.javarosa.core.model.instance.TreeElement;

import javax.annotation.Nullable;

/**
 * Model representation for a row of "IndexedFixtureIndex" table
 *
 * Represents a IndexedFixture root level properties like the fixture's
 * base name and child name along with the root level attributes.
 */

public class IndexedFixtureIdentifier {
    private String fixtureBase;
    private String fixtureChild;

    @Nullable
    private byte[] rootAttributes;

    public IndexedFixtureIdentifier(String fixtureBase, String fixtureChild, @Nullable byte[] rootAttributes) {
        this.fixtureBase = fixtureBase;
        this.fixtureChild = fixtureChild;
        this.rootAttributes = rootAttributes;
    }

    public String getFixtureBase() {
        return fixtureBase;
    }

    public String getFixtureChild() {
        return fixtureChild;
    }

    @Nullable
    public byte[] getRootAttributes() {
        return rootAttributes;
    }
}



