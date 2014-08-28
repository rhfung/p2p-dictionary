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

class DataMissing {
	public static  final DataMissing Singleton = new DataMissing();

	// / <summary>
	// / Tests to see if the object is Singleton
	// / </summary>
	// / <param name="test"></param>
	// / <returns></returns>
	public static boolean isSingleton(Object test) {
		if (test == null) {
			return false;
		} else if (Singleton.equals(test)) {
			return true;
		} else {
			return false;
		}
	}

}
