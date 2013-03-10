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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;


/**
 * Emulation of .NET StreamWriter. 
 * @author Richard
 *
 */
public class StreamWriter  {

	static final String NEWLINE = "\r\n";
	
	private ByteArrayOutputStream m_stream;
	private MemoryStream m_memory;
	private java.io.StringWriter m_writer;
	
	public StreamWriter( ByteArrayOutputStream stream)
	{
		m_stream = stream;
		m_memory = null;
		m_writer = new StringWriter();
	}
	
	public StreamWriter( MemoryStream stream)
	{
		m_stream = null;
		m_memory = stream;
		m_writer = new StringWriter();
	}
	
	/**
	 * Does not flush the buffer.
	 * @param str
	 */
	public void WriteLine(String str)
	{
		m_writer.write(str + NEWLINE);
	}
	
	/**
	 * Does not flush the buffer.
	 */
	public void WriteLine()
	{
		m_writer.write(NEWLINE);
	}
	
	/**
	 * Writes strings. Does not flush the buffer.
	 * @param str
	 */
	public void Write(String str)
	{
		m_writer.write(str);
	}
	
	/**
	 * Flushes string that are written.
	 */
	public void Flush() {
		flush(null);
	}
	
	/**
	 * Automatically flush the current buffer and then adds appendBytes
	 */
	private void flush(byte[] appendBytes)
	{
		m_writer.flush();
		if (m_stream != null)
		{
		try {
			m_stream.write(ReadBytes(m_writer, m_writer.getBuffer().length()));
			if(appendBytes != null)
				m_stream.write(appendBytes);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}
		if (m_memory != null)
		{
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(m_memory.getLength() + m_writer.getBuffer().length() );
			try
			{
				byteStream.write(m_memory.getBuffer());
				byteStream.write(ReadBytes(m_writer, m_writer.getBuffer().length()));
				if (appendBytes != null)
					byteStream.write(appendBytes);
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
			m_memory.setBuffer(byteStream.toByteArray());
		}
		m_writer = new StringWriter();
	}
	
	private static byte[]  ReadBytes(StringWriter reader, int length)
    {
		String str = reader.toString();
    	byte[] arrayToFill = new byte[length];
    	for (int i = 0; i < length; i++)
    	{
    		arrayToFill[i] = (byte) str.charAt(i); // ENSURE this is 8-bit non-UTF reading!!!!
    	}
    	return arrayToFill;
    }

	/**
	 * Each call to Write with a byte array will deep copy the byte buffer, so call this method infrequently.
	 * Flushes the buffer immediately.
	 * @param byteBuffer
	 */
	public void Write(byte[] byteBuffer) {
		flush(byteBuffer);
	}

	/**
	 * Each call to Write with a byte array will deep copy the byte buffer, so call this method infrequently.
	 * Flushes the buffer immediately.
	 * @param memStream
	 */
	public void Write(MemoryStream memStream)
	{
		if (memStream != null)
		{
			if (memStream != m_memory)
			{
				Write(memStream.getBuffer());
			}
			else
			{
				throw new RuntimeException("Cannot write buffer to itself");
			}
		}
	}
}
