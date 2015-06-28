/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package br

import org.opalj.br.analyses.SomeProject

/**
 * A `VirtualSourceElement` is the representation of some source element that is
 * always detached from the concrete source element that represents the implementation.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
sealed trait VirtualSourceElement
        extends SourceElement
        with scala.math.Ordered[VirtualSourceElement] {

    override def attributes = Nil

    final override def isVirtual = true

    /**
     * The "natural order" is VirtualClasses < VirtualFields < VirtualMethods.
     */
    override def compare(that: VirtualSourceElement): Int

    /**
     * Returns the class type of this `VirtualSourceElement`. If this `VirtualSourceElement`
     * is a [[VirtualClass]] the returned type is the declared class else it is the
     * declaring class.
     */
    def classType: ReferenceType

    def toJava: String

    /**
     * Returns the best line number information available.
     */
    def getLineNumber(project: SomeProject): Option[Int]

}

/**
 * Defines common helper functions related to [[VirtualSourceElement]]s.
 */
object VirtualSourceElement {

    def asVirtualSourceElements(
        classFiles: Traversable[ClassFile],
        includeMethods: Boolean = true,
        includeFields: Boolean = true): Iterable[VirtualSourceElement] = {
        var sourceElements: Iterable[VirtualSourceElement] = Iterable.empty

        classFiles foreach { classFile ⇒
            val classType = classFile.thisType
            sourceElements ++ Iterable(classFile.asVirtualClass)
            if (includeMethods)
                sourceElements ++= classFile.methods.view.map(_.asVirtualMethod(classType))
            if (includeFields)
                sourceElements ++= classFile.fields.view.map(_.asVirtualField(classType))
        }
        sourceElements
    }

}

/**
 * Represents a class for which we have found some references but have not analyzed
 * any class file or do not want to keep the reference to the underlying class file.
 *
 * @author Michael Eichberg
 */
final case class VirtualClass(thisType: ObjectType) extends VirtualSourceElement {

    override def isClass: Boolean = true

    override def classType: ObjectType = thisType

    override def toJava: String = thisType.toJava

    def getLineNumber(project: SomeProject): Option[Int] = Some(1)

    override def compare(that: VirtualSourceElement): Int = {
        //x < 0 when this < that; x == 0 when this == that; x > 0 when this > that
        that match {
            case VirtualClass(thatType) ⇒ thisType.compare(thatType)
            case _                      ⇒ -1
        }
    }

    override def hashCode: Int = thisType.id

    /**
     * Two objects of type `VirtualClass` are considered equal if they represent
     * the same type.
     */
    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualClass ⇒ this.thisType eq that.thisType
            case _                  ⇒ false
        }
    }
}

/**
 * @author Michael Eichberg
 */
sealed trait VirtualClassMember extends VirtualSourceElement

/**
 * Represents a field of a virtual class.
 *
 * @author Michael Eichberg
 */
final case class VirtualField(
        declaringClassType: ObjectType,
        name: String,
        fieldType: FieldType) extends VirtualClassMember {

    override def isField: Boolean = true

    override def classType: ObjectType = declaringClassType

    override def toJava: String =
        declaringClassType.toJava+"{ "+fieldType.toJava+" "+name+"; }"

    def getLineNumber(project: SomeProject): Option[Int] = None

    override def compare(that: VirtualSourceElement): Int = {
        // x < 0 when this < that; x == 0 when this == that; x > 0 when this > that
        that match {
            case _: VirtualClass ⇒
                1
            case that: VirtualField ⇒
                if (this.declaringClassType eq that.declaringClassType) {
                    this.name.compareTo(that.name) match {
                        case 0 ⇒ this.fieldType.compare(that.fieldType)
                        case x ⇒ x
                    }
                } else {
                    if (this.declaringClassType.id < that.declaringClassType.id)
                        -1
                    else
                        1
                }
            case _ /*VirtualMethod*/ ⇒
                -1
        }
    }

    override def hashCode: Int =
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + fieldType.id

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualField ⇒
                (this.declaringClassType eq that.declaringClassType) &&
                    (this.fieldType eq that.fieldType) &&
                    this.name == that.name
            case _ ⇒ false
        }
    }
}

/**
 * Represents a method of a virtual class.
 *
 * @author Michael Eichberg
 */
sealed class VirtualMethod(
    val declaringClassType: ReferenceType,
    val name: String,
    val descriptor: MethodDescriptor)
        extends VirtualClassMember {

    override def isMethod: Boolean = true

    override def classType: ReferenceType = declaringClassType

    override def toJava: String =
        declaringClassType.toJava+"{ "+descriptor.toJava(name)+"; }"

    def getLineNumber(project: SomeProject): Option[Int] = {
        if (declaringClassType.isArrayType)
            return None;

        project.classFile(declaringClassType.asObjectType).flatMap(cf ⇒
            cf.findMethod(name, descriptor).flatMap(m ⇒
                m.body.flatMap(b ⇒
                    b.firstLineNumber)))
    }

    override def compare(that: VirtualSourceElement): Int = {
        // x < 0 when this < that; x == 0 when this == that; x > 0 when this > that
        that match {
            case that: VirtualMethod ⇒
                if (this.declaringClassType eq that.declaringClassType) {
                    this.name.compareTo(that.name) match {
                        case 0 ⇒ this.descriptor.compare(that.descriptor)
                        case x ⇒ x
                    }
                } else {
                    if (this.declaringClassType.id < that.declaringClassType.id)
                        -1
                    else
                        1
                }
            case _ ⇒
                1
        }
    }

    override def hashCode: Int =
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + descriptor.hashCode()

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualMethod ⇒
                (this.declaringClassType eq that.declaringClassType) &&
                    (this.descriptor == that.descriptor) &&
                    this.name == that.name
            case _ ⇒ false
        }
    }
}
object VirtualMethod {

    def apply(
        declaringClassType: ReferenceType,
        name: String,
        descriptor: MethodDescriptor): VirtualMethod =
        new VirtualMethod(declaringClassType, name, descriptor)

    def unapply(virtualMethod: VirtualMethod): Option[(ReferenceType, String, MethodDescriptor)] = {
        Some((
            virtualMethod.declaringClassType,
            virtualMethod.name,
            virtualMethod.descriptor))
    }
}

final class VirtualForwardingMethod(
    declaringClassType: ReferenceType,
    name: String,
    descriptor: MethodDescriptor,
    val target: Method)
        extends VirtualMethod(declaringClassType, name, descriptor) {

    override def toJava: String =
        declaringClassType.toJava+"{ "+descriptor.toJava(name)+"; }"

}

object VirtualForwardingMethod {

    def apply(
        declaringClassType: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        target: Method): VirtualMethod =
        new VirtualForwardingMethod(declaringClassType, name, descriptor, target)

    def unapply(virtualMethod: VirtualForwardingMethod): Option[(ReferenceType, String, MethodDescriptor, Method)] = {
        Some((
            virtualMethod.declaringClassType,
            virtualMethod.name,
            virtualMethod.descriptor,
            virtualMethod.target))
    }
}
