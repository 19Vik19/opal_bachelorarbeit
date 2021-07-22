package org.opalj.fpcf.fixtures.cifi_benchmark.arrays.transitively_immutable;

import org.opalj.fpcf.fixtures.cifi_benchmark.common.CustomObject;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * This class encompasses different array-typed transitively immutable fields.
 */
@TransitivelyImmutableType("Class is final")
@TransitivelyImmutableClass("Class has only transitively immutable fields")
public final class ArrayTransitiveImmutability {

    @TransitivelyImmutableField("The elements of the array can not escape")
    @NonAssignableField("Field is final")
    private final Integer[] eagerAssignedIntegerArray = new Integer[]{1, 2, 3};

    @TransitivelyImmutableField("The elements of the array are never set")
    @NonAssignableField("The field is only assigned once")
    private static final Integer[] staticTransitivelyImmutableArray = new Integer[5];

    @TransitivelyImmutableField("The elements of the array can not escape")
    @NonAssignableField("Field is initialized in the constructor")
    private final Integer[] transitivelyImmutableArrayAssignedInTheConstructor;

    public ArrayTransitiveImmutability() {
        transitivelyImmutableArrayAssignedInTheConstructor = new Integer[]{5, 6, 7, 8};
    }

    @TransitivelyImmutableField("The elements of the array can not escape")
    @EffectivelyNonAssignableField("The array is not initialized.")
    private CustomObject[] notInitializedArray;

    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private CustomObject[] clonedArray = new CustomObject[]{new CustomObject(), new CustomObject()};

    public CustomObject[] getClonedArray(){ return  clonedArray.clone(); }
}
