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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * JSON object serialization uses Jackson 2.0.2.
 * @author Richard
 *
 */
public class JSONObject {
	private String jsonString;
	private ObjectMapper m_mapper;


	/**
	 * Constructs using JSON notation.
	 * @param jsonStr
	 */
	public JSONObject(String jsonStr)
	{
		this.jsonString = jsonStr;
		this.m_mapper = new ObjectMapper();
	}
	
	/**
	 * Constructs and creates JSON from the given Object.
	 * @param serializeObject
	 */
	public JSONObject(Object serializeObject) throws JsonMappingException, JsonGenerationException, IOException
	{
		this.m_mapper = new ObjectMapper();
		this.jsonString = m_mapper.writeValueAsString(serializeObject);
	}
	
	public void put(Object serializeObject) throws JsonMappingException, JsonGenerationException, IOException
	{
		this.jsonString = m_mapper.writeValueAsString(serializeObject);
	}
	
	public <T> T get(Class<T> valueType) throws JsonParseException, JsonMappingException, IOException
	{
		return m_mapper.readValue(this.jsonString, valueType);
	}
	
	public boolean isException()
	{
		try
		{
			JsonNode node= m_mapper.readTree(jsonString);
			return node.has("class") && node.has("exception");
		}
		catch(JsonProcessingException ex)
		{
			return false;	
		}
		catch( IOException ex)
		{
			return false;
		}
	
	}
	
	public String getJsonString() {
		return jsonString;
	}

	public void setJsonString(String jsonString)
	{
		this.jsonString = jsonString;
	}
	
	@Override
	public String toString() {
		return jsonString;
	}
}
