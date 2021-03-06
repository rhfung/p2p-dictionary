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
import com.rhfung.P2PDictionary.MIMEByteObject;
import com.rhfung.P2PDictionary.JSONObject;


/**
 * Any Java object is automatically converted to JSON for serialization.
 * @author Richard
 *
 */
public class JSONEncoding implements IEncoding {

	
	@Override
	public int getValueType() {
		// TODO Auto-generated method stub
		return ValueType.JSON;
	}

	@Override
	public String getMime() {
		// TODO Auto-generated method stub
		return "application/json";
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

	/**
	 * Instantiates a JSONObject.
	 */
	@Override
	public Object readBytes(byte[] sourceData) {
		JSONObject object = new JSONObject(new String(sourceData, Charset.forName("UTF-8")));
		return object;
	}

	/**
	 * Instantiates a JSONObject.
	 */
	@Override
	public Object readMimeString(String nval) {
		// TODO Auto-generated method stub
		JSONObject object = new JSONObject(nval);
		return object; 
	}

	/**
	 * Returns true for JSONObject or Java Object.
	 */
	@Override
	public boolean isInstanceOf(Object test) {
		// TODO Auto-generated method stub
		return 	(test instanceof JSONObject ||
				test instanceof Object) &&
				!(test instanceof MIMEByteObject);
	}
	
	/**
	 * Converts either JSONObject or Java Object to JSON. Automatically creates a JSON
	 * object notation
	 * {
	 * 		class: "class_name",
	 * 		exception: "exception_class"
	 * }
	 * if cannot create the object automatically.
	 */
	@Override
	public byte[] writeBytes(Object toSerialize)  {
		if (toSerialize instanceof JSONObject)
			return ((JSONObject) toSerialize).getJsonString().getBytes(Charset.forName("UTF-8"));
		else
		{
			try
			{
				// create JSON object for serialization
				JSONObject obj = new JSONObject(toSerialize);
				return obj.getJsonString().getBytes(Charset.forName("UTF-8"));
			}
			catch(Exception ex)
			{
				JSONObject obj = new JSONObject("{ \"class\": \"" + toSerialize.getClass().getName() + "\", \"exception\": \"" + ex.getClass().getName() + "\" }") ;
				return obj.getJsonString().getBytes(Charset.forName("UTF-8"));
			}
		}
	}
}
