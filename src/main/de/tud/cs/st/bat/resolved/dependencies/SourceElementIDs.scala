/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.resolved
package dependencies

/**
 * Associates a source element with a unique id.
 *
 * Types are associated with ids larger than 0 and smaller than one billion.
 *
 * Fields are associated with ids > 1 000 000 000 and < 2 000 000 000
 *
 * Methods are associated with ids > 2 000 000 000
 *
 * @author Michael Eichberg
 */
object SourceElementIDs {

    import scala.collection.mutable.WeakHashMap

    private var nextTypeID = 0;

    private val typeIDs = WeakHashMap[Type, Int]()

    def sourceElementID(classFile: ClassFile): Int = sourceElementID(classFile.thisClass)

    def sourceElementID(t: Type): Int = typeIDs.getOrElseUpdate(t, { nextTypeID += 1; nextTypeID })

    //
    // Associates each method with a unique ID
    //

    private var nextMethodID = 1000000000;

    private val methodIDs = WeakHashMap[Int, WeakHashMap[(String, MethodDescriptor), Int]]()

    /**
     * '''Performance Hint'''
     * If possible try to use the more specific method id(Int,Method_Info).
     */
    def sourceElementID(classFile: ClassFile, method: Method_Info): Int = sourceElementID(sourceElementID(classFile), method)

    def sourceElementID(definingObjectTypeID: Int, method: Method_Info): Int = {
        val Method_Info(_, methodName, methodDescriptor, _) = method
        sourceElementID(definingObjectTypeID, methodName, methodDescriptor)
    }

    def sourceElementID(definingObjectTypeID: Int, methodName: String, methodDescriptor: MethodDescriptor): Int = {
        methodIDs.
            getOrElseUpdate(definingObjectTypeID, { WeakHashMap[(String, MethodDescriptor), Int]() }).
            getOrElseUpdate((methodName, methodDescriptor), { nextMethodID += 1; nextMethodID })
    }

}