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

import java.nio.charset.Charset;

abstract public class AbstractNumberEncoding implements IEncoding {

	private String mime;
	private int type;
	
	public AbstractNumberEncoding(String mimeType, int valType)
	{
		this.mime = mimeType;
		this.type = valType;
	}
	
	@Override
	public int getValueType() {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public String getMime() {
		// TODO Auto-generated method stub
		return mime;
	}

	@Override
	public boolean isSimpleValue() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isComplexValue() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	/**
	 * Returns a completed string for isSimpleValue
	 */
	public String getMimeSimpleData(Object obj) 
	{
		// TODO Auto-generated method stub
		return obj.toString();
	}

	@Override
	public Object readBytes(byte[] sourceData) {
		// TODO Auto-generated method stub
		String intermediate = new String(sourceData, Charset.forName("UTF-8"));
		return parse(intermediate);
	}
	
	public abstract Number parse(String numberText);

	@Override
	public Object readMimeString(String nval) 
	{
		// TODO Auto-generated method stub
		return parse(nval);
	}

	@Override
	public byte[] writeBytes(Object toSerialize) {
		return ((Number) toSerialize).toString().getBytes(Charset.forName("UTF-8"));
	}
}
