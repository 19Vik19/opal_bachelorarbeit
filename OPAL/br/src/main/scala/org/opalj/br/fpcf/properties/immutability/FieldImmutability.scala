/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package immutability

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait FieldImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    type Self = FieldImmutability
}

/**
 * Describes the field immutability of org.opalj.br.Field
 *
 * [[MutableField]] A field with a mutable field reference
 *
 * [[NonTransitivelyImmutableField]] A field with an immutable field reference and a shallow immutable or mutable data type
 *
 * [[DependentlyImmutableField]] A field with an immutable field reference and a generic type and parts of it are no
 * substantiated in an shallow or mutable way.
 *
 * [[TransitivelyImmutableField]] A field with an immutable field reference and a deep immutable field type or with an
 * immutable field reference and a referenced object that can not escape or its state be mutated.
 *
 * @author Tobias Roth
 */
sealed trait FieldImmutability extends OrderedProperty with FieldImmutabilityPropertyMetaInformation {
    final def key: PropertyKey[FieldImmutability] = FieldImmutability.key
}

object FieldImmutability extends FieldImmutabilityPropertyMetaInformation {
    final val PropertyKeyName = "opalj.FieldImmutability"

    final val key: PropertyKey[FieldImmutability] = {
        PropertyKey.create(
            PropertyKeyName,
            MutableField
        )
    }
}

case object TransitivelyImmutableField extends FieldImmutability {

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: FieldImmutability): FieldImmutability = that
}

case class DependentlyImmutableField(parameter: Set[String]) extends FieldImmutability {

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == TransitivelyImmutableField) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this");
        }
    }

    def meet(that: FieldImmutability): FieldImmutability = {
        if (that == MutableField || that == NonTransitivelyImmutableField)
            that
        else {
            if (that.isInstanceOf[DependentlyImmutableField])
                DependentlyImmutableField(parameter.union(that.asInstanceOf[DependentlyImmutableField].parameter))
            else this
        }
    }
}

case object NonTransitivelyImmutableField extends FieldImmutability {

    def meet(that: FieldImmutability): FieldImmutability = {
        if (that == MutableField)
            that
        else
            this
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == TransitivelyImmutableField || other.isInstanceOf[DependentlyImmutableField]) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this");
        }
    }
}

case object MutableField extends FieldImmutability {

    def meet(other: FieldImmutability): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableField) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}
