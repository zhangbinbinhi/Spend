package com.spend.engine.props;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NProperties {

	private static Logger logger = LogManager.getLogger(NProperties.class
			.getName());

	public static Map<String, String> getTheProperties(String propsFilePath) {

		logger.debug("Read SProperties Properties !");

		Map<String, String> mapProperties = new HashMap<String, String>();

		mapProperties = new HashMap<String, String>();

		Properties props = new Properties();
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(
					propsFilePath));
			props.load(in);
			Enumeration<?> en = props.propertyNames();
			while (en.hasMoreElements()) {
				String key = (String) en.nextElement();
				String Property = props.getProperty(key);
				logger.debug(key + ":" + Property);
				
				mapProperties.put(key, Property);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return mapProperties;
	}

	public static Map<String, String> getTheProperties(File propsFile) {
		if(!propsFile.exists()){
			return null;
		}
		logger.debug("Read NProperties Properties !");
		Map<String, String> mapProperties = new HashMap<String, String>();
		mapProperties = new HashMap<String, String>();

		Properties props = new Properties();
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(
					propsFile));
			props.load(in);
			Enumeration<?> en = props.propertyNames();
			while (en.hasMoreElements()) {
				String key = (String) en.nextElement();
				String Property = props.getProperty(key);
				logger.debug(key + ":" + Property);
				mapProperties.put(key, Property);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return mapProperties;
	}
}