package com.sri.yices;

/**
 * Little class to act like Pair<YVal[], YVal>
 *
 */

public class VectorValue {
    public final YVal[] vector;
    public final YVal value;

    public VectorValue(YVal[] vector, YVal value){
        this.vector = vector;
        this.value = value;
    }

}
