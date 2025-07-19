/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.xquery.functions.array;

import com.github.krukow.clj_lang.*;
import com.ibm.icu.text.Collator;
import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Implements the array type (XQuery 3.1). An array is also a function. This class thus extends
 * {@link FunctionReference} to allow the item to be called in a dynamic function
 * call.
 *
 * Based on immutable, persistent vectors. Operations like append, head, tail, reverse should be fast.
 * Remove and insert-before require copying the array.
 *
 * @author Wolf
 */
public class ArrayType extends FunctionReference implements Lookup.LookupSupport {

    // the signature of the function which is evaluated if the map is called as a function item
    private static final FunctionSignature ACCESSOR =

        new FunctionSignature(
            new QName("get", ArrayModule.NAMESPACE_URI, ArrayModule.PREFIX),
            "Internal accessor function for arrays.",
            new SequenceType[] {
                    new FunctionParameterSequenceType("index", Type.INTEGER, Cardinality.EXACTLY_ONE, "The index")
            },
            new SequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE));

    private InternalFunctionCall accessorFunc;

    private IPersistentVector<Sequence> vector;

    private XQueryContext context;

    /**
     * The common super-type of items in the array.
     * Initialised to {@link Type#ANY_TYPE} when the type is unknown.
     */
    private int itemType = Type.ANY_TYPE;

    public ArrayType(final XQueryContext context, final List<Sequence> items) {
        this(null, context, items);
    }

    public ArrayType(final Expression expression, final XQueryContext context, final List<Sequence> items) {
        this(expression, context);
        this.vector = PersistentVector.create(items);
        for (int i = 0; i < items.size(); i++) {
            final Sequence sequence = items.get(i);
            if (i == 0) {
                this.itemType = sequence.getItemType();
            } else {
                this.itemType = Type.getCommonSuperType(sequence.getItemType(), itemType);
            }
        }
    }

    public ArrayType(final XQueryContext context, final Sequence items) throws XPathException {
        this(null, context, items);
    }

    public ArrayType(final Expression expression, final XQueryContext context, final Sequence items) throws XPathException {
        this(expression, context);
        final int itemCount = items.getItemCount();
        final Sequence[] itemList = new Sequence[itemCount];
        for (int i = 0; i < itemCount;  i++) {
            final Item item = items.itemAt(i);
            if (i == 0) {
                this.itemType = item.getType();
            } else {
                this.itemType = Type.getCommonSuperType(item.getType(), itemType);
            }
            final Sequence sequence = item.toSequence();
            itemList[i] = sequence;
        }
        this.vector = PersistentVector.create(itemList);
    }

    public ArrayType(final XQueryContext context, final IPersistentVector<Sequence> vector) {
        this(null, context, vector);
    }

    public ArrayType(final Expression expression, final XQueryContext context, final IPersistentVector<Sequence> vector) {
        this(expression, context);
        this.vector = vector;
        for (int i = 0; i < vector.count(); i++) {
            final Sequence sequence = vector.nth(i);
            if (i == 0) {
                this.itemType = sequence.getItemType();
            } else{
                this.itemType = Type.getCommonSuperType(sequence.getItemType(), itemType);
            }
        }
    }

    private ArrayType(final Expression expression, final XQueryContext context) {
        super(expression, null);
        this.context = context;
        final Function fn = new AccessorFunc(context);
        this.accessorFunc = new InternalFunctionCall(fn);
    }

    public Sequence get(int n) {
        return vector.nth(n);
    }

    @Override
    public Sequence get(final AtomicValue key) throws XPathException {
        if (!Type.subTypeOf(key.getType(), Type.INTEGER)) {
            throw new XPathException(getExpression(), ErrorCodes.XPTY0004, "Position argument for array lookup must be a positive integer");
        }
        final int pos = ((IntegerValue)key).getInt();
        if (pos <= 0 || pos > getSize()) {
            final String startIdx = vector.length() == 0 ? "0" : "1";
            final String endIdx = String.valueOf(vector.length());
            throw new XPathException(getExpression(), ErrorCodes.FOAY0001, "Array index " + pos + " out of bounds (" + startIdx + ".." + endIdx + ")");
        }
        return get(pos - 1);
    }

    @Override
    public Sequence keys() throws XPathException {
        return asSequence();
    }

    public Sequence tail() throws XPathException {
        if (vector.length() == 2) {
            final Sequence tail = vector.nth(1);
            return new ArrayType(getExpression(), context, tail);
        }
        return new ArrayType(getExpression(), context, RT.subvec(vector, 1, vector.length()));
    }

    public ArrayType subarray(int start, int end) throws XPathException {
        return new ArrayType(getExpression(), context, RT.subvec(vector, start, end));
    }

    public ArrayType remove(int position) throws XPathException {
        ITransientCollection<Sequence> ret = PersistentVector.emptyVector().asTransient();

        for(int i = 0; i < vector.length(); i++) {
            if (position != i) {
                ret = ret.conj(vector.nth(i));
            }
        }

        return new ArrayType(getExpression(), context, (IPersistentVector<Sequence>)ret.persistent());
    }

    public ArrayType insertBefore(final int position, final Sequence member) throws XPathException {
        ITransientCollection<Sequence> ret = PersistentVector.emptyVector().asTransient();

        for(int i = 0; i < vector.length(); i++) {
            if (position == i) {
                ret = ret.conj(member);
            }
            ret = ret.conj(vector.nth(i));
        }
        if (position == vector.length()) {
            ret = ret.conj(member);
        }

        return new ArrayType(getExpression(), context, (IPersistentVector<Sequence>)ret.persistent());
    }

    public ArrayType put(final int position, final Sequence member) throws XPathException {
        return new ArrayType(getExpression(), context, vector.assocN(position,member));
    }

    public static ArrayType join(final XQueryContext context, final List<ArrayType> arrays) {
        final ITransientCollection<Sequence> ret = PersistentVector.emptyVector().asTransient();
        for (final ArrayType type: arrays) {
            for (ISeq<Sequence> seq = type.vector.seq(); seq != null; seq = seq.next()) {
                ret.conj(seq.first());
            }
        }
        return new ArrayType(null, context, (IPersistentVector<Sequence>)ret.persistent());
    }

    /**
     * Add member. Modifies the array! Don't use unless you're constructing a new array.
     *
     * @param sequence the member sequence to add
     */
    public void add(final Sequence sequence) {
        this.vector = vector.cons(sequence);
        this.itemType = itemType == Type.ANY_TYPE ? sequence.getItemType() : Type.getCommonSuperType(sequence.getItemType(), itemType);
    }

    /**
     * Return a new array with a member appended.
     *
     * @param seq the member sequence to append
     * @return new array
     */
    public ArrayType append(final Sequence seq) {
        return new ArrayType(getExpression(), this.context, vector.cons(seq));
    }

    public ArrayType reverse() {
        final IPersistentVector<Sequence> rvec = PersistentVector.create(vector.rseq());
        return new ArrayType(getExpression(), this.context, rvec);
    }

    public Sequence asSequence() throws XPathException {
        final ValueSequence result = new ValueSequence(vector.length());
        for (int i = 0; i < vector.length(); i++) {
            result.addAll(vector.nth(i));
        }
        return result;
    }

    public Sequence[] toArray() {
        final Sequence[] array = new Sequence[vector.length()];
        return (Sequence[]) RT.seqToPassedArray(vector.seq(), array);
    }

    public int getSize() {
        return vector.length();
    }

    @Override
    public void analyze(final AnalyzeContextInfo contextInfo) throws XPathException {
        accessorFunc.analyze(contextInfo);
    }

    @Override
    public Sequence eval(final Sequence contextSequence) throws XPathException {
        return accessorFunc.eval(contextSequence, null);
    }

    @Override
    public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
        return accessorFunc.eval(contextSequence, contextItem);
    }

    @Override
    public Sequence evalFunction(final Sequence contextSequence, final Item contextItem, final Sequence[] seq) throws XPathException {
        final AccessorFunc af =  (AccessorFunc) accessorFunc.getFunction();
        return af.eval(seq, contextSequence);
    }

    @Override
    public void setArguments(final List<Expression> arguments) throws XPathException {
        accessorFunc.setArguments(arguments);
    }

    @Override
    public void resetState(final boolean postOptimization) {
        accessorFunc.resetState(postOptimization);
    }

    @Override
    public int getType() {
        return Type.ARRAY;
    }

    @Override
    public int getItemType() {
        // TODO(AR) investigate why returning the type of items in the array causes tests to fail
        return Type.ARRAY;
//        return itemType == Type.ANY_TYPE ? Type.ITEM : itemType;
    }

    @Override
    public AtomicValue atomize() throws XPathException {
        if (vector.length() == 0) {
            return null;
        } else if (vector.length() > 1) {
            throw new XPathException(getExpression(), ErrorCodes.XPTY0004, "Expected single atomic value but found array with length " + vector.length());
        }
        final Sequence member = vector.nth(0);
        if (member.hasMany()) {
            throw new XPathException(getExpression(), ErrorCodes.XPTY0004, "Expected single atomic value but found sequence of length " + member.getItemCount());
        }
        return member.itemAt(0).atomize();
    }

    public ArrayType sort(@Nullable final Collator collator, final FunctionReference keyFunRef) throws XPathException {
        final Map<Sequence, List<Sequence>> sortedMap = new TreeMap<>(new SequenceComparator(collator));

        final Sequence fargs[] = new Sequence[1];
        for (ISeq<Sequence> seq = vector.seq(); seq != null; seq = seq.next()) {
            fargs[0] = seq.first();

            final Sequence sortKey = keyFunRef.evalFunction(null, null, fargs);
            sortedMap.compute(sortKey, (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                v.add(fargs[0]);
                return v;
            });
        }

        final List<Sequence> sorted = sortedMap
            .values()
            .stream()
            .reduce(new ArrayList<>(), (a,b) -> {
                a.addAll(b);
                return a;
            });

        return new ArrayType(getExpression(), context, (IPersistentVector<Sequence>)PersistentVector.create(sorted));
    }

    public ArrayType forEach(final FunctionReference ref) throws XPathException {
        final ITransientCollection<Sequence> ret = PersistentVector.emptyVector().asTransient();
        final Sequence[] fargs = new Sequence[1];
        for (ISeq<Sequence> seq = vector.seq(); seq != null; seq = seq.next()) {
            fargs[0] = seq.first();
            ret.conj(ref.evalFunction(null, null, fargs));
        }
        return new ArrayType(getExpression(), context, (IPersistentVector<Sequence>)ret.persistent());
    }

    public ArrayType forEachPair(final ArrayType other, final FunctionReference ref) throws XPathException {
        final ITransientCollection<Sequence> ret = PersistentVector.emptyVector().asTransient();
        for (ISeq<Sequence> i1 = vector.seq(), i2 = other.vector.seq(); i1 != null && i2 != null; i1 = i1.next(), i2 = i2.next()) {
            ret.conj(ref.evalFunction(null, null, new Sequence[]{ i1.first(), i2.first() }));
        }
        return new ArrayType(getExpression(), context, (IPersistentVector<Sequence>)ret.persistent());
    }

    public ArrayType filter(final FunctionReference ref) throws XPathException {
        final ITransientCollection<Sequence> ret = PersistentVector.emptyVector().asTransient();
        final Sequence[] fargs = new Sequence[1];
        for (ISeq<Sequence> seq = vector.seq(); seq != null; seq = seq.next()) {
            fargs[0] = seq.first();
            final Sequence fret = ref.evalFunction(null, null, fargs);
            if (fret.effectiveBooleanValue()) {
                ret.conj(fargs[0]);
            }
        }
        return new ArrayType(getExpression(), context, (IPersistentVector<Sequence>)ret.persistent());
    }

    public Sequence foldLeft(final FunctionReference ref, Sequence zero) throws XPathException {
        for (ISeq<Sequence> seq = vector.seq(); seq != null; seq = seq.next()) {
            zero = ref.evalFunction(null, null, new Sequence[] { zero, seq.first() });
        }
        return zero;
    }

    public Sequence foldRight(final FunctionReference ref, final Sequence zero) throws XPathException {
        final ISeq<Sequence> seq = vector.seq();
        return foldRight(ref, zero, seq);
    }

    private Sequence foldRight(final FunctionReference ref, final Sequence zero, final ISeq<Sequence> seq) throws XPathException {
        if (seq == null) {
            return zero;
        }
        final Sequence head = seq.first();
        final Sequence tailResult = foldRight(ref, zero, seq.next());
        return ref.evalFunction(null, null, new Sequence[] { head, tailResult });
    }

    protected static Sequence flatten(final Sequence input, final ValueSequence result) throws XPathException {
        for (final SequenceIterator i = input.iterate(); i.hasNext(); ) {
            final Item item = i.nextItem();
            if (item.getType() == Type.ARRAY) {
                final Sequence members = ((ArrayType)item).asSequence();
                flatten(members, result);
            } else {
                result.add(item);
            }
        }
        return result;
    }

    public static Sequence flatten(final Item item) throws XPathException {
        if (item.getType() == Type.ARRAY) {
            final Sequence members = ((ArrayType)item).asSequence();
            return flatten(members, new ValueSequence(members.getItemCount()));
        }
        return item.toSequence();
    }

    /**
     * Flatten the given sequence by recursively replacing arrays with their member sequence.
     *
     * @param input the sequence to flatten
     * @return flattened sequence
     * @throws XPathException in case of dynamic error
     */
    public static Sequence flatten(final Sequence input) throws XPathException {
        if (input.hasOne()) {
            return flatten(input.itemAt(0));
        }
        boolean flatten = false;
        final int itemType = input.getItemType();
        if (itemType == Type.ARRAY) {
            flatten = true;
        } else if (itemType == Type.ITEM) {
            // may contain arrays - check
            for (final SequenceIterator i = input.iterate(); i.hasNext(); ) {
                if (i.nextItem().getType() == Type.ARRAY) {
                    flatten = true;
                    break;
                }
            }
        }
        return flatten ? flatten(input, new ValueSequence(input.getItemCount() * 2)) : input;
    }

    @Override
    public boolean containsReference(final Item item) {
        for (int i = 0; i < vector.length(); i++) {
            final Sequence value = vector.nth(i);
            if (value == item || value.containsReference(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(final Item item) {
        for (int i = 0; i < vector.length(); i++) {
            final Sequence value = vector.nth(i);
            if (value.equals(item) || value.contains(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (vector.length() > 0) {
            builder.append(' ');
        }
        for (int i = 0; i < vector.length(); i++) {

            if (i > 0) {
                builder.append(", ");
            }

            final Sequence sequence = vector.nth(i);

            if (!sequence.hasOne()) {
                builder.append('(');
            }

            for (int j = 0; j < sequence.getItemCount(); j++) {
                if (j > 0) {
                    builder.append(", ");
                }

                final Item item = sequence.itemAt(j);
                if (Type.subTypeOf(item.getType(), Type.STRING)) {
                    builder.append('"');
                }

                builder.append(item.toString());

                if (Type.subTypeOf(item.getType(), Type.STRING)) {
                    builder.append('"');
                }
            }

            if (!sequence.hasOne()) {
                builder.append(')');
            }
        }
        if (vector.length() > 0) {
            builder.append(' ');
        }
        builder.append(']');
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T toJavaObject(final Class<T> target) throws XPathException {
        if (byte[].class.isAssignableFrom(target)) {
            final byte[] javaArray = new byte[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(byte.class);
            }
            return (T) javaArray;

        } else if (Byte[].class.isAssignableFrom(target)) {
            final Byte[] javaArray = new Byte[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Byte.class);
            }
            return (T) javaArray;

        } else if (short[].class.isAssignableFrom(target)) {
            final short[] javaArray = new short[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(short.class);
            }
            return (T) javaArray;

        } else if (Short[].class.isAssignableFrom(target)) {
            final Short[] javaArray = new Short[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Short.class);
            }
            return (T) javaArray;

        } else if (int[].class.isAssignableFrom(target)) {
            final int[] javaArray = new int[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(int.class);
            }
            return (T) javaArray;

        } else if (Integer[].class.isAssignableFrom(target)) {
            final Integer[] javaArray = new Integer[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Integer.class);
            }
            return (T) javaArray;

        } else if (long[].class.isAssignableFrom(target)) {
            final long[] javaArray = new long[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(long.class);
            }
            return (T) javaArray;

        } else if (Long[].class.isAssignableFrom(target)) {
            final Long[] javaArray = new Long[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Long.class);
            }
            return (T) javaArray;

        } else if (float[].class.isAssignableFrom(target)) {
            final float[] javaArray = new float[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(float.class);
            }
            return (T) javaArray;

        } else if (Float[].class.isAssignableFrom(target)) {
            final Float[] javaArray = new Float[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Float.class);
            }
            return (T) javaArray;

        } else if (double[].class.isAssignableFrom(target)) {
            final double[] javaArray = new double[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(double.class);
            }
            return (T) javaArray;

        } else if (Double[].class.isAssignableFrom(target)) {
            final Double[] javaArray = new Double[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Double.class);
            }
            return (T) javaArray;

        } else if (boolean[].class.isAssignableFrom(target)) {
            final boolean[] javaArray = new boolean[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(boolean.class);
            }
            return (T) javaArray;

        } else if (Boolean[].class.isAssignableFrom(target)) {
            final Boolean[] javaArray = new Boolean[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Boolean.class);
            }
            return (T) javaArray;

        } else if (char[].class.isAssignableFrom(target)) {
            final char[] javaArray = new char[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(char.class);
            }
            return (T) javaArray;

        } else if (Character[].class.isAssignableFrom(target)) {
            final Character[] javaArray = new Character[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Character.class);
            }
            return (T) javaArray;

        } else if (String[].class.isAssignableFrom(target)) {
            final String[] javaArray = new String[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(String.class);
            }
            return (T) javaArray;

        } else if (Object[].class.isAssignableFrom(target)) {
            final Object[] javaArray = new Object[vector.length()];
            for (int i = 0; i < vector.length(); i++) {
                javaArray[i] = vector.nth(i).toJavaObject(Object.class);
            }
            return (T) javaArray;
        }

        return super.toJavaObject(target);
    }

    /**
     * The accessor function which will be evaluated if the Array is called
     * as a function item.
     */
    private class AccessorFunc extends BasicFunction {

        public AccessorFunc(final XQueryContext context) {
            super(context, ACCESSOR);
        }

        @Override
        public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
            final IntegerValue v = (IntegerValue) args[0].itemAt(0);
            final int n = v.getInt();
            if (n <= 0 || n > ArrayType.this.getSize()) {
                throw new XPathException(this, ErrorCodes.FOAY0001, "Position " + n + " does not exist in this array. Length is " + ArrayType.this.getSize());
            }
            return ArrayType.this.get(n - 1);
        }
    }
}
