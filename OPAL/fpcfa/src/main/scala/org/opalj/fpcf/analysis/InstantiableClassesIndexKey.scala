/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analysis

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.SourceElementsPropertyStoreKey

/**
 * The ''key'' object to get information about the classes that can be instantiated
 * (either directly or indirectly).
 *
 * @example
 *      To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and pass in
 *      `this` object.
 *
 * @author Michael Reif
 */
object InstantiableClassesIndexKey extends ProjectInformationKey[InstantiableClassesIndex] {

    /**
     * The [[InstantiableClassesIndex]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements = Seq(FPCFAnalysesManagerKey, SourceElementsPropertyStoreKey)

    /**
     * Computes the information which classes are (not) instantiable.
     */
    override protected def compute(project: SomeProject): InstantiableClassesIndex = {
        InstantiableClassesIndex(project)
    }
}