package com.rhfung.Interop;

//P2PDictionary
//Copyright (C) 2013, Richard H Fung (www.richardhfung.com)
//
//Permission is hereby granted to any person obtaining a copy of this software 
//and associated documentation files (the "Software"), to deal in the Software 
//for the sole purposes of PERSONAL USE. This software cannot be used in 
//products where commercial interests exist (i.e., license, profit from, or
//otherwise seek monetary value). The person DOES NOT HAVE the right to
//redistribute, copy, modify, merge, publish, sublicense, or sell this Software
//without explicit prior permission from the author, Richard H Fung.
//
//The above copyright notice and this permission notice shall be included 
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.

import java.util.Collection;
import java.util.Iterator;


/**
 * <p>
 * A tuple of three elements.
 * </p> 
 * 
 * @since 1.0
 * 
 * @author Daniel Fern&aacute;ndez
 *
 */
public final class Tuple<A,B,C> 
         {

    private static final long serialVersionUID = -1877265551599483740L;

    private static final int SIZE = 3;

    private final A val0;
    private final B val1;
    private final C val2;
    
    
    
    public static <A,B,C> Tuple<A,B,C> with(final A value0, final B value1, final C value2) {
        return new Tuple<A,B,C>(value0,value1,value2);
    }

    
    /**
     * <p>
     * Create tuple from array. Array has to have exactly three elements.
     * </p>
     * 
     * @param <X> the array component type 
     * @param array the array to be converted to a tuple
     * @return the tuple
     */
    public static <X> Tuple<X,X,X> fromArray(final X[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        if (array.length != 3) {
            throw new IllegalArgumentException("Array must have exactly 3 elements in order to create a Triplet. Size is " + array.length);
        }
        return new Tuple<X,X,X>(array[0],array[1],array[2]);
    }

    
    /**
     * <p>
     * Create tuple from collection. Collection has to have exactly three elements.
     * </p>
     * 
     * @param <X> the collection component type 
     * @param collection the collection to be converted to a tuple
     * @return the tuple
     */
    public static <X> Tuple<X,X,X> fromCollection(final Collection<X> collection) {
        return fromIterable(collection);
    }


    
    /**
     * <p>
     * Create tuple from iterable. Iterable has to have exactly three elements.
     * </p>
     * 
     * @param <X> the iterable component type 
     * @param iterable the iterable to be converted to a tuple
     * @return the tuple
     */
    public static <X> Tuple<X,X,X> fromIterable(final Iterable<X> iterable) {
        return fromIterable(iterable, 0, true);
    }

    
    
    /**
     * <p>
     * Create tuple from iterable, starting from the specified index. Iterable
     * can have more (or less) elements than the tuple to be created.
     * </p>
     * 
     * @param <X> the iterable component type 
     * @param iterable the iterable to be converted to a tuple
     * @return the tuple
     */
    public static <X> Tuple<X,X,X> fromIterable(final Iterable<X> iterable, int index) {
        return fromIterable(iterable, index, false);
    }

    


    private static <X> Tuple<X,X,X> fromIterable(final Iterable<X> iterable, int index, final boolean exactSize) {
        
        if (iterable == null) {
            throw new IllegalArgumentException("Iterable cannot be null");
        }

        boolean tooFewElements = false; 
        
        X element0 = null;
        X element1 = null;
        X element2 = null;
        
        final Iterator<X> iter = iterable.iterator();
        
        int i = 0;
        while (i < index) {
            if (iter.hasNext()) {
                iter.next();
            } else {
                tooFewElements = true;
            }
            i++;
        }
        
        if (iter.hasNext()) {
            element0 = iter.next();
        } else {
            tooFewElements = true;
        }
        
        if (iter.hasNext()) {
            element1 = iter.next();
        } else {
            tooFewElements = true;
        }
        
        if (iter.hasNext()) {
            element2 = iter.next();
        } else {
            tooFewElements = true;
        }
        
        if (tooFewElements && exactSize) {
            throw new IllegalArgumentException("Not enough elements for creating a Triplet (3 needed)");
        }
        
        if (iter.hasNext() && exactSize) {
            throw new IllegalArgumentException("Iterable must have exactly 3 available elements in order to create a Triplet.");
        }
        
        return new Tuple<X,X,X>(element0, element1, element2);
        
    }
    

    
    
    
    
    public Tuple(
            final A value0,
            final B value1,
            final C value2) {
        this.val0 = value0;
        this.val1 = value1;
        this.val2 = value2;
    }


    public A getValue0() {
        return this.val0;
    }


    public B getValue1() {
        return this.val1;
    }


    public C getValue2() {
        return this.val2;
    }


    public int getSize() {
        return 3;
    }
    
}
