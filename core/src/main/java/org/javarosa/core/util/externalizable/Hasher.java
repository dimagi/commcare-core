/**
 *
 */
package org.javarosa.core.util.externalizable;

/**
 * A class capable of producing hash values. All implementations
 * must provide the same value as the reference implementation
 * (in PrototypeFactory), but can be capable of doing so in
 * an improved manner.
 *
 * This is a workaround for the fact that the serialization engine
 * doesn't provide a PrototypeFactory on serialization, only
 * deserialziation
 *
 * @author ctsims
 */
public interface Hasher {
    public byte[] getClassHashValue(Class type);
}
