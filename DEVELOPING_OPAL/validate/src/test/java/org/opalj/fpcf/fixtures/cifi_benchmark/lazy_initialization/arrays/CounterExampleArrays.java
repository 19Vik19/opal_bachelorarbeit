package org.opalj.fpcf.fixtures.cifi_benchmark.lazy_initialization.arrays;

import org.opalj.fpcf.fixtures.cifi_benchmark.common.CustomObject;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;

/**
 * This class encompasses different counter examples of lazy initialized arrays.
 */
public class CounterExampleArrays {

    @MutableField("Field is assignable")
    @AssignableField("Multiple assignments possible")
    private CustomObject[] array;

    public CustomObject[] getArray(int n) {
        if (array == null || array.length < n) {
            this.array = new CustomObject[n];
        }
        return array;
    }

    @MutableField("Field is assignable")
    @AssignableField("Field can be read with multiple values.")
    private CustomObject[] b;

    public CustomObject[] getB(boolean flag) throws Exception {
        if(b!=null)
            return b;
        else if(flag)
            return b;
        else {
            this.b = new CustomObject[5];
            return b;
        }
    }
}
