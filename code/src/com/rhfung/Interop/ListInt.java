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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

/**
 * List of integers.
 * @author Richard
 *
 */
public class ListInt implements  List<Integer> {
	
	private Vector<Integer> m_list;
	
	public ListInt()
	{
		m_list = new Vector<Integer>();
	}
	
	/*
	 * Deep copies list. Does not copy Integer objects.
	 */
	public ListInt(ListInt copyList)
	{
		m_list = new Vector<Integer>();
		
		for(Integer i : copyList)
		{
			this.add(i);
		}
	}
	
	public ListInt(int initialCapacity) {
		m_list = new Vector<Integer>(initialCapacity);
	}

	public ListInt(Collection<Integer> collection) {
		m_list = new Vector<Integer>(collection);
	}

	public ListInt(Enumeration<Integer> values) {
		m_list = new Vector<Integer>();
		while (values.hasMoreElements())
		{
				m_list.add(	values.nextElement());
		}
	}

	public static ListInt createList(int item1)
	{
		ListInt list = new ListInt();
		list.add(item1);
		return list;
	}
	
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		
		Iterator<Integer> values = m_list.iterator();
		while (values.hasNext())
		{
				if (bld.length() > 0)
					bld.append(",");
				bld.append(values.next().toString());
		}
		
		return bld.toString();
	}

	@Override
	public int size() {
		return m_list.size();
	}

	@Override
	public boolean isEmpty() {
		return m_list.isEmpty();
	}

	/**
	 * Compares element by integer value.
	 */
	@Override
	public boolean contains(Object o) {
		if (o instanceof Number)
		{
			for(Integer i : m_list)
			{
				if (i.equals(o))
					return true;
			}
			
			return false;
		}
		else
			return false;
	}

	@Override
	public Iterator<Integer> iterator() {
		return m_list.iterator();
	}

	@Override
	public Object[] toArray() {
		return m_list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return m_list.toArray(a);
	}

	public void add(int item)
	{
		m_list.add(new Integer(item));
	}
	
	
	@Override
	public boolean add(Integer e) {
		return m_list.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return m_list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return m_list.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		return m_list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Integer> c) {
		return m_list.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return m_list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return m_list.retainAll(c);
	}

	@Override
	public void clear() {
		m_list.clear();
		
	}

	@Override
	public Integer get(int index) {
		return m_list.get(index);
	}
	
	public int getLastItem()
	{
		return m_list.get(m_list.size() - 1).intValue();
	}

	@Override
	public Integer set(int index, Integer element) {
		return m_list.set(index, element);
	}

	@Override
	public void add(int index, Integer element) {		
		m_list.add(index, element); 
	}

	/**
	 * Removes the integer element, not the index.
	 */
	@Override
	public Integer remove(int index) {
		m_list.remove(new Integer(index));
		return null;
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return m_list.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return m_list.lastIndexOf(o);
	}

	@Override
	public ListIterator<Integer> listIterator() {
		// TODO Auto-generated method stub
		return m_list.listIterator();
	}

	@Override
	public ListIterator<Integer> listIterator(int index) {
		// TODO Auto-generated method stub
		return m_list.listIterator(index);
	}

	@Override
	public List<Integer> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return m_list.subList(fromIndex, toIndex);
	}

	/**
	 * @return 0 if no id found
	 */
	public int getNextIntegerGreaterThan(int baseInteger)
	{
		if (m_list.size() < 2)
			return 0;
		
		SortedSet<Integer> keySort = new TreeSet<Integer>(m_list);
		Iterator<Integer> iter = keySort.iterator();
		int theInt = iter.next();
		while(iter.hasNext())
		{
			if (theInt <= baseInteger )
				theInt = iter.next();
			else
				return theInt;
		}
		if (theInt > baseInteger)
			return theInt;
		else
			return 0;
	}
}
