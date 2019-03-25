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
    private String fixtureName;
    private String fixtureBase;
    private String fixtureChild;

    @Nullable
    private TreeElement rootAttributes;

    public IndexedFixtureIdentifier(String fixtureName, String fixtureBase, String fixtureChild, @Nullable TreeElement rootAttributes) {
        this.fixtureName = fixtureName;
        this.fixtureBase = fixtureBase;
        this.fixtureChild = fixtureChild;
        this.rootAttributes = rootAttributes;
    }

    public String getFixtureName() {
        return fixtureName;
    }

    public void setFixtureName(String name) {
        this.fixtureName = name;
    }

    public String getFixtureBase() {
        return fixtureBase;
    }

    public void setFixtureBase(String fixtureBase) {
        this.fixtureBase = fixtureBase;
    }

    public String getFixtureChild() {
        return fixtureChild;
    }

    public void setChild(String child) {
        this.fixtureChild = child;
    }

    @Nullable
    public TreeElement getRootAttributes() {
        return rootAttributes;
    }

    public void setRootAttributes(@Nullable TreeElement rootAttributes) {
        this.rootAttributes = rootAttributes;
    }
}



