package com.spend.engine.props;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SProperties {
	
	/**
	 * @此类注意负责对配置文件的解析
	 */
	private final static String propsFilePath = "./etc/props.properties";

	private final Map<String, String> mapProperties;

	private static Logger logger = LogManager.getLogger(SProperties.class
			.getName());

	private static final class Holder {
		static final SProperties instance = new SProperties(propsFilePath);
	}

	public static SProperties getInstance() {
		return Holder.instance;
	}

	public SProperties(String propsFilePath) {
		
		logger.info("Read SProperties Properties !");

		this.mapProperties = new HashMap<String, String>();

		Properties props = new Properties();
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(
					propsFilePath));
			props.load(in);
			Enumeration<?> en = props.propertyNames();
			while (en.hasMoreElements()) {
				String key = (String) en.nextElement();
				String Property = props.getProperty(key);
				logger.info(key + ":" + Property);
				mapProperties.put(key, Property);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Map<String, String> getMapProperties() {
		return mapProperties;
	}
}