/*
 * Copyright 2011-2013, by Vladimir Kostyukov and Contributors.
 * 
 * This file is part of la4j project (http://la4j.org)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributor(s): -
 * 
 */

package org.la4j.factory;

import org.la4j.linear.LinearSystem;
import org.la4j.matrix.Matrix;
import org.la4j.vector.Vector;

public abstract class AbstractFactory implements Factory {

    private static final long serialVersionUID = 4071505L;

    @Override
    public LinearSystem createLinearSystem(Matrix a, Vector b) {
        return new LinearSystem(a, b, this);
    }

    @Override
    public Factory safe() {
        return new SafeFactory(this);
    }

    @Override
    public Factory unsafe() {
        return this;
    }
}
