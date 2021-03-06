/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.co.tuple;

import sorcer.service.Arg;
import sorcer.service.Fidelity;

public class FidelityOutputEntry<T, F extends Arg> extends OutputEntry<T> {

    private static final long serialVersionUID = 1L;

    private Fidelity<F> fidelity;

    public FidelityOutputEntry(String path, T value, Fidelity<F> fidelity) {
        super(path, value, 0);
        this.index = index;
        this.fidelity= fidelity;
    }

    public FidelityOutputEntry(String path, T value, int index) {
        super(path, value);
        this.index = index;
    }

    public FidelityOutputEntry(String path, T value, boolean isPersistant, int index) {
        this(path, value, index);
        this.isPersistent = isPersistant;
    }

    public Fidelity<F> getFidelity() {
        return fidelity;
    }

    public void setFidelity(Fidelity<F> fidelity) {
        this.fidelity = fidelity;
    }

}