/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package properties

import scala.collection.Set
import org.opalj.br.Method
import org.opalj.fpcf.Property

/**
 * Determines for each interface based call site those methods that are potentially called by
 * call-by-signature means '''only'''.
 */
sealed trait CallBySignature extends Property {

    final type Self = CallBySignature

    final def isRefineable = false

    /**
     * Returns the key used by all `CallBySignature` properties.
     */
    final def key = CallBySignature.Key
}

object CallBySignature {

    final val Key = {
        PropertyKey.create[CallBySignature](
            // The unique name of the property.
            "CallBySignatureTargets",
            // The default property that will be used if no analysis is able
            // to (directly) compute the respective property.
            // TODO
            (ps: PropertyStore, e: Entity) ⇒ throw new RuntimeException("Preliminary Version"),
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ throw new UnknownError("internal error")
        )
    }
}

case class CBSTargets(cbsTargets: Set[Method]) extends CallBySignature

case object NoCBSTargets extends CallBySignature

