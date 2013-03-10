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

import com.rhfung.P2PDictionary.MIMEByteObject;

/**
 * Instances of this object are created with unique value types to handle
 * unsupported MIME types.
 * @author Richard
 *
 */
public class DynamicDataEncoding implements IEncoding {

	private int m_ValueType;
	private String m_Mime;
	
	public DynamicDataEncoding(int valueType, String mimeType)
	{
		m_ValueType = valueType;
		m_Mime = mimeType;
	}
	
	@Override
	public int getValueType() {
		// TODO Auto-generated method stub
		return m_ValueType;
	}

	@Override
	public String getMime() {
		// TODO Auto-generated method stub
		return m_Mime;
	}

	@Override
	public boolean isSimpleValue() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isComplexValue() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getMimeSimpleData(Object obj) {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public Object readBytes(byte[] sourceData) {
		return new com.rhfung.P2PDictionary.MIMEByteObject(getMime(), sourceData);
	}

	@Override
	public Object readMimeString(String nval) {
		return null;
	}

	@Override
	public boolean isInstanceOf(Object test) {
		// match the mime type
		return (test != null) && (test instanceof MIMEByteObject)
				&& ((MIMEByteObject) test).getMimeType().equals(getMime());
	}
	
	@Override
	public byte[] writeBytes(Object toSerialize)  {
		return ((MIMEByteObject) toSerialize).getPayload();
	}

}
