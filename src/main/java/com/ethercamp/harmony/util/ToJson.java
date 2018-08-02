/**
 * Copyright (c) 2011-2013 iTel Technology Inc,All Rights Reserved.
 */
	
package com.ethercamp.harmony.util;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @ClassName: ToJson
 * @Description: TODO
 * @author Administrator
 * @date 2013年11月20日 下午2:29:43
 */
public final class ToJson {
	private static final Logger LOG = Logger.getLogger(ToJson.class);
	public static String toJson(Object o){
	
		try {
			return new ObjectMapper().writeValueAsString(o);
		} catch (Exception e) {
			LOG.error("convert object to json string fail:"+o.toString(),e);
		}
		return null;
	}
	
	public static <T> T convertJsonToObject(String json,Class<T> aclass){
		try {
			return new ObjectMapper().readValue(json, aclass);
		} catch (IOException e) {
			LOG.error("convert json to object fail:"+json,e);
		}
		return null;
	}
	public static <T> List<T> convertJsonArrayToObject(String jsonArray,Class<T> aclass){
		
		ObjectMapper mapper=new ObjectMapper();
		try {
			return mapper.readValue(jsonArray, mapper.getTypeFactory().constructCollectionType(List.class, aclass));
		} catch (IOException e) {
			LOG.error("convert json array to List fail:"+jsonArray,e);
		}
		return null;
	}
	
}
