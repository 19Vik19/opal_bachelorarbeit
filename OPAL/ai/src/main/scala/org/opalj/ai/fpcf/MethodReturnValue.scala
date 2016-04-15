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
package ai
package fpcf

import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey

sealed trait MethodReturnValuePropertyMetaInformation extends PropertyMetaInformation {

    final type Self = MethodReturnValue
}

/**
 * Stores the information about the value (always) returned by a specific method. '''Overridden
 * methods are generally not taken into account.'''
 *
 * In the worst case the information about the return value is just the declared type.
 *
 * @param returnValue The value returned by the method when the method does not throw an exception.
 * 		If the method always throws an exception, then the returnValue is `None`.
 */
case class MethodReturnValue private (
        returnValue: Option[Domain#Value]
) extends Property with MethodReturnValuePropertyMetaInformation {

    assert(returnValue.isEmpty || (returnValue.get ne null), "returnValue must not be null")

    def this(returnValue: Domain#Value) { this(Some(returnValue)) }

    final def key = MethodReturnValue.key

    final def isRefineable = true

}

object MethodReturnValue extends MethodReturnValuePropertyMetaInformation {

    final val DeclaredReturnType = MethodReturnValue(None)

    /**
     * The key associated with every purity property.
     */
    final val key: PropertyKey[MethodReturnValue] = PropertyKey.create(
        "MethodReturnValue",
        /* Default / Fallback  */ DeclaredReturnType,
        /*  Cyclic Resolution  */ DeclaredReturnType
    )

}
