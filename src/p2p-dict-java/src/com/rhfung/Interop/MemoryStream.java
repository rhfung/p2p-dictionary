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

/**
 * Emulation of .NET memory stream for P2P Dictionary.
 * @author Richard
 *
 */
public class MemoryStream {
	private byte[] buffer;

	public MemoryStream()
	{
		this.buffer = new byte[0];
	}
	
	public MemoryStream(byte[] copyBuffer)
	{
		this.buffer = copyBuffer;
	}
	
	public MemoryStream(ByteArrayOutputStream memBuffer)
	{
		this.buffer = memBuffer.toByteArray();
	}
	
	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}
	
	public StreamWriter createStreamWriter()
	{
		return new StreamWriter(this);
	}
	
	public int getLength()
	{
		return buffer.length;
	}
	
	public void dispose()
	{
		this.buffer = null;
	}
}
