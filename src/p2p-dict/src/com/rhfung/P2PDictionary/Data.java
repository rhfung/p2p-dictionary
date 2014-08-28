package com.rhfung.P2PDictionary;

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
import java.util.*;

import com.rhfung.Interop.NotImplementedException;
import com.rhfung.P2PDictionary.Encodings.*;


abstract class Data {
	// 3rd column: false = simple; true = complex
	//private static List<Tuple<ValueType, String, Boolean>> encodings;
	
	// first added encoding has higher priority
	private static List<IEncoding> newEncodings;
	private static Map<String,IEncoding> newEncodingLookupMime;
	private static Vector<IEncoding> newEncodingLookupType;
	

	public Object value;
	public int type;

	static {
		newEncodings = new Vector<IEncoding>();
		newEncodings.add(new BooleanEncoding());//1
		newEncodings.add(new AbstractNumberEncoding("number/int32", ValueType.Int32) { //2
			@Override
			public Number parse(String numberText) {
				// TODO Auto-generated method stub
				return new Integer(numberText);
			}
			@Override
			public boolean isInstanceOf(Object test) {
				// TODO Auto-generated method stub
				return test instanceof Integer;
			}
		});
		newEncodings.add(new AbstractNumberEncoding("number/int16", ValueType.Int16) { //3
			@Override
			public Number parse(String numberText) {
				// TODO Auto-generated method stub
				return new Short(numberText);
			}
			@Override
			public boolean isInstanceOf(Object test) {
				// TODO Auto-generated method stub
				return test instanceof Short;
			}
		});
		
		newEncodings.add(new AbstractNumberEncoding("number/int64", ValueType.Int64) { //4
			@Override
			public Number parse(String numberText) {
				// TODO Auto-generated method stub
				return new Long(numberText);
			}
			@Override
			public boolean isInstanceOf(Object test) {
				// TODO Auto-generated method stub
				return test instanceof Long;
			}
		});
		
		newEncodings.add(new AbstractNumberEncoding("number/single", ValueType.Single) { //5
			@Override
			public Number parse(String numberText) {
				// TODO Auto-generated method stub
				return new Float(numberText);
			}
			@Override
			public boolean isInstanceOf(Object test) {
				// TODO Auto-generated method stub
				return test instanceof Float;
			}
		});
		newEncodings.add(new AbstractNumberEncoding("number/double", ValueType.Double) { //6
			@Override
			public Number parse(String numberText) {
				// TODO Auto-generated method stub
				return new Double(numberText);
			}
			@Override
			public boolean isInstanceOf(Object test) {
				// TODO Auto-generated method stub
				return test instanceof Double;
			}
		});
	
		newEncodings.add(new BinaryEncoding()); //7
		newEncodings.add(new NullEncoding());//8
		newEncodings.add(new ObjectEncoding());
		newEncodings.add(new RemovedEncoding());//10
		newEncodings.add(new StringEncoding());
		newEncodings.add(new JSONEncoding());
		
		newEncodingLookupMime = new Hashtable<String,IEncoding>(newEncodings.size());
		newEncodingLookupType = new Vector<IEncoding>(newEncodings.size());
		newEncodingLookupType.setSize(newEncodings.size());
		for(IEncoding enc : newEncodings)
		{
			newEncodingLookupMime.put(enc.getMime(), enc);
			newEncodingLookupType.set(enc.getValueType(), enc);
		}
	}

	public void DetectTypeFromValue() {
		for (IEncoding enc : newEncodings)
		{
			if ( enc.isInstanceOf(this.value))
			{
				this.type = enc.getValueType();
				return;
			}
		}
		
		// not able to find encoding, let's check to see DataUnsupported 
		if (value instanceof MIMEByteObject)
		{
			String mimeType = ((MIMEByteObject) value).getMimeType();
			IEncoding enc = newEncodingLookupMime.get(mimeType);
			if (enc == null)
			{
				enc = addDataType(mimeType);
			}
			else
			{
				// auto-resolve the unsupported data immediately, so doesn't
				// keep the wrapper object in memory
				Object newVal = enc.readBytes(((MIMEByteObject) value).getPayload());
				this.value = newVal;
			}
			this.type = enc.getValueType();
		}
		else
		{
			throw new NotImplementedException("Cannot DetectTypeFromValue");
		}
	}


	public boolean isEmpty() {
		return type == ValueType.Removed;

	}

	public boolean isSimpleValue()
    {
		return newEncodingLookupType.get(this.type).isSimpleValue();
               //return encodings.Exists(x => x.Item1 == type && x.Item3 == false && x.Item1 != ValueType.Removed );
        
    }

	public static boolean isSimpleType(int type)
    {
            //return (type == ValueType.Booleanean || type == ValueType.Int16 || type == ValueType.Int32 || type == ValueType.Int64
            //    || type == ValueType.Single || type == ValueType.Double);
        // return encodings.Exists(x => x.Item1 == type && x.Item3 == false && x.Item1 != ValueType.Removed);
	return newEncodingLookupType.get(type).isSimpleValue();
    }

	public boolean isComplexValue()
    {
            // return (type == ValueType.String || type == ValueType.Binary || type == ValueType.Object);
        return newEncodingLookupType.get(this.type).isComplexValue();
    }

	@Override
	public String toString() {
		if (DataMissing.isSingleton(this.value)) {
			return getMime() + ";m=DataMissing";
		} else {
			return getMime() + GetMimeSimpleData();
		}
	}

	
	
	public String GetMimeSimpleData() {
		if (DataMissing.isSingleton(value)) {
			// no value to report
			return "";
		} else {
			IEncoding enc = newEncodingLookupType.get(this.type);
			if (enc.isSimpleValue() )
			{
				String s = enc.getMimeSimpleData(value);
				if (s.length() > 0)
					return ";d=" + s;
				else
					return s;
			}
			return "";

		}
	}

	public void ReadBytesUsingMime(String mimeType, byte[] data) {
		ReadMimeData(mimeType, "", data);
	}

	public void ReadMimeSimpleData(String mimeType) {
		String mime = mimeType.split("[;]", 2)[0];
		String nval = "";

		if ("number".equals(mime.split("/", 2)[0]) ) {
			if (mimeType.indexOf(";") > 0) {
				nval = mimeType.split("[;]", 2)[1].substring(2);
				ReadMimeData(mime, nval, null);
			} else {
				setMime(mime);
				this.value = DataMissing.Singleton;
			}
		} else {
			ReadMimeData(mime, nval, null);
		}

	}

	private void ReadMimeData(String mimeType, String nval, byte[] sourceBytes) {
		String mime = mimeType.split(";",2)[0];
		String rootMime = mime.split("/",2)[0];

		IEncoding enc = newEncodingLookupMime.get(mime);
		if (enc == null)
		{
			enc = addDataType(mime);
		}
		this.type = enc.getValueType();

		if (rootMime.equals( "number") && mimeType.contains(";")) {
			// reads the ;d=
			nval = mimeType.split(";")[1].substring(2);
		
			// byte payload provided in addition to mime payload
			if (sourceBytes != null ) {
				nval = new String(sourceBytes, Charset.forName("UTF-8")); 
			}
			
			enc.readMimeString(nval);
		}
		else
		{
			if (sourceBytes == null)
				this.value = DataMissing.Singleton;
			else
				this.value = enc.readBytes(sourceBytes);
		}
	}

	public String getMime()
    {
        // most mime types are not standard
        return newEncodingLookupType.get(this.type).getMime();
    }
	
	public byte[] writeEncodedBytes()
	{
		IEncoding enc = newEncodingLookupType.get(this.type);
		if (enc.isInstanceOf(this.value))
			return enc.writeBytes(this.value);
		else
			throw new NotImplementedException(enc.getMime() + " failed isInstanceOf during WriteMime");
	}

	// / <summary>
	// / Should use ReadBytesUsingMime or ReadMimeSimpleData. This is used for
	// HEAD.
	// / </summary>
	// / <returns></returns>
	public void setMime(String mimeName)
    {
		IEncoding enc = newEncodingLookupMime.get(mimeName);
		if (enc == null)
		{
			enc = addDataType(mimeName);
		}
        this.type = enc.getValueType();
    }
	
	private IEncoding addDataType(String mimeName)
	{
		synchronized (newEncodings) {
			IEncoding enc = new DynamicDataEncoding(newEncodingLookupType.size(), mimeName);
			 
			newEncodings.add(enc);
			newEncodingLookupMime.put(mimeName, enc);
			newEncodingLookupType.add(newEncodingLookupType.size(), enc); // add to end of list
						
			return enc;
		}
	}

	public boolean isDeleted() 
	{
		return this.type == ValueType.Removed;
	}

	public void Delete() 
	{
		this.type = ValueType.Removed;
		this.value = null;
	}
}
