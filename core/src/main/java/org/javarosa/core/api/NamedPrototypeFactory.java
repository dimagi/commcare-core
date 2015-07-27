/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.api;

import org.javarosa.core.util.PrefixTree;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * ProtoType factory for serializing and deserializing persisted classes using
 * their hash codes. To use a non-default hasher, use one of the overriding constructors
 * or call setStaticHasher().
 */

public class NamedPrototypeFactory extends PrototypeFactory{

    public NamedPrototypeFactory() {
        super();
        setStaticHasher(new NameHasher());
    }

    public NamedPrototypeFactory(PrefixTree classNames) {
        super(classNames);
        setStaticHasher(new NameHasher());
    }
}
