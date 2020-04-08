package com.sri.yices;

/**
 *
 * YVal objects are used to explore values in a Model using the API. A
 * value in a model is representaed as a DAG.  Yices provides
 * functions to access values by exploring the model DAG. Function
 * yices_get_value evaluates a term and returns a node descriptor from
 * which the term value can be constructed.
 * Within a model, each node has an integer identifier and a tag that
 * specifies the node’s type. All DAG-exploration functions store this
 * information in records of type YVal.
 *
 * I see no reason to make it anything more than a glorified C-struct.
 *
 */
public class YVal {

    public final YValTag tag;

    public final int id;

    public YVal(int tag, int id){
        this.tag = YValTag.idToTag(tag);
        this.id = id;
    }

    public String toString(){
        return String.format("<%s: %d>", tag, id);
    }

}
