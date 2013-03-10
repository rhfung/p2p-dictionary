package com.rhfung.P2PDictionary.Encodings;

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

public interface IEncoding
{
	int getValueType();
	String getMime();
	
	boolean isSimpleValue();
	boolean isComplexValue(); // arg! should only have one test
	boolean isInstanceOf(Object test);
	
	/**
	 * Method called if isSimpleValue
	 * @param obj
	 * @return
	 */
	String getMimeSimpleData(Object obj); // caution - can be long
	
	/**
	 * Method called if the mime type does not begin with number
	 * @param sourceData
	 * @return
	 */
	Object readBytes(byte[] sourceData);
	
	/**
	 * Method called to create a set of bytes from Object.
	 * Can be assured that data type is compatible as isInstanceOf
	 * called beforehand.
	 */
	byte[] writeBytes(Object toSerialize);
	
	/**
	 * Method called if the mime type begins with number
	 * @param nval
	 * @return
	 */
	Object readMimeString(String nval);
}
