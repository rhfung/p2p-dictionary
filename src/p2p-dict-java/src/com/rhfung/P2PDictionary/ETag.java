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

class ETag {
	public int UID;
	public int Revision;

	public ETag(int owner, int rev) {
		this.UID = owner;
		this.Revision = rev;
	}

	private String TrimStart(String original, String charToTrim)
	{
		if (original.startsWith(charToTrim))
			return original.substring(charToTrim.length());
		else
			return original;
	}
	
	private String TrimEnd(String original, String charToTrim)
	{
		if (original.endsWith(charToTrim))
			return original.substring(0, original.length() - charToTrim.length());
		else
			return original;
	}
	
	// / <summary>
	// / Takes an ETag in the format "42.576" and extracts the version number
	// / </summary>
	// / <param name="s"></param>
	public ETag(String eTag)
    {
        eTag = TrimStart(eTag, "\"");
        eTag = TrimEnd(eTag, "\"");

        
        String[] parts = eTag.split("[.]", 2);
        this.UID = Integer.parseInt(parts[0]);
        this.Revision = Integer.parseInt(parts[1]);
    }

	public static ETagCompare CompareETags(ETag first, ETag second) {
		if (first.Revision > second.Revision) {
			return ETagCompare.FirstIsNewer;
		} else if (first.Revision < second.Revision) {
			return ETagCompare.SecondIsNewer;
		} else if (first.UID == second.UID) {
			return ETagCompare.Same;
		} else {
			return ETagCompare.Conflict;
		}
	}

}
