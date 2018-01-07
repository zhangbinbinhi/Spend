package com.spend.util.com;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.Element;

public class XmlDom4j {

	private static Logger logger = LogManager.getLogger();

	public static Document parse(URL url) {
		SAXReader reader = new SAXReader();
		Document document = null;
		try {
			document = reader.read(url);
		} catch (DocumentException e) {
			logger.error("Parse return data to xml error! Detail: " + e);
			return null;
		}
		return document;
	}

	public static Document parse(File file) {
		SAXReader reader = new SAXReader();
		Document document = null;
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			try {
				document = reader.read(is);
			} catch (DocumentException e) {
				logger.error("File can not parse to xml document! Detail: " + e);
				is.close();
				return null;
			}
		} catch (IOException e) {
			logger.error("File can not parse to be xml document! Detail: " + e);
			try {
				is.close();
			} catch (IOException e1) {
				logger.error("Colse FileInputStream error! Detail: " + e);
			}
			return null;
		}
		return document;
	}

	public static Document parse(String data) {
		SAXReader reader = new SAXReader();
		Document document = null;
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(data.getBytes());
			document = reader.read(is);
		} catch (DocumentException e) {
			logger.error("Parse return data to xml error! Detail: " + e);
			try {
				is.close();
			} catch (IOException e1) {
				logger.error("Colse InputStream error! Detail: " + e);
			}
			return null;
		}
		return document;
	}

	public static Document parse(InputStream is) {
		SAXReader reader = new SAXReader();
		Document document = null;
		try {
			document = reader.read(is);
		} catch (DocumentException e) {
			logger.error("File can not parse to xml document! Detail: " + e);
			try {
				is.close();
			} catch (IOException e1) {
				logger.error("Colse InputStream error! Detail: " + e);
			}
			return null;
		}
		return document;
	}

	public static InputStream parse(Document doc) {
		String data = doc.asXML();
		InputStream is = new ByteArrayInputStream(data.getBytes());
		return is;
	}

	public static Document createDocument() {
		Document document = DocumentHelper.createDocument();
		return document;
	}

	public static Document createDocumentWithRoot() {
		return createDocumentWithRoot("root");
	}

	public static Document createDocumentWithRoot(String rootName) {
		Document document = DocumentHelper.createDocument();
		document.addElement(rootName);
		return document;
	}

	public static void writeXML(Document document, File file) {
		logger.info("将 XML Document写入文件: " + file);
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("UTF-8");
		try {
			XMLWriter writer = new XMLWriter(new FileWriter(file), format);
			writer.write(document);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, Map<String, List<String>>> parseGABIndexInfo(InputStream isIndex) {
		Document doc = XmlDom4j.parse(isIndex);
		try {
			isIndex.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return parseGABIndexInfo(doc);
	}

	public static Map<String, Map<String, List<String>>> parseGABIndexInfo(Document doc) {
		Map<String, Map<String, List<String>>> mapIndexInfo = new HashMap<String, Map<String, List<String>>>();
		Element root = doc.getRootElement();
		if (!root.getName().equals("MESSAGE")) {
			logger.error("Index Error! ");
			return mapIndexInfo;
		}
		Element element_DATASET_0 = root.element("DATASET");
		if (element_DATASET_0 == null) {
			logger.error("Index Error! ");
			return mapIndexInfo;
		}
		if (!("WA_COMMON_010017".equals(element_DATASET_0.attributeValue("name"))
				|| "JZ_COMMON_010017".equals(element_DATASET_0.attributeValue("name")))) {
			logger.error("Index Error! ");
			return mapIndexInfo;
		}
		Element element_DATASET_1 = element_DATASET_0.element("DATA").element("DATASET");
		if (element_DATASET_1 == null) {
			logger.error("Index Error! ");
			return mapIndexInfo;
		}
		if (!("WA_COMMON_010013".equals(element_DATASET_1.attributeValue("name"))
				|| "JZ_COMMON_010013".equals(element_DATASET_1.attributeValue("name")))) {
			logger.error("Index Error! ");
			return mapIndexInfo;
		}
		Iterator<?> itElemDATA = element_DATASET_1.elementIterator();
		while (itElemDATA.hasNext()) {
			// 每一个单独的DATA,抽象为每一类协议的数据
			String DATASOURCE = "";
			String PROTYPE = "";
			String DATA_SPLIT = "";
			String LINE_SPLIT = "";
			String COMPANY = "";
			String CITYCODE = "";
			String START_LINE = "";
			String CHARSET = "";
			List<String> listFiles = new LinkedList<String>();
			List<String> listDataField = new LinkedList<String>();
			List<String> listCHNField = new LinkedList<String>();
			List<String> listENGField = new LinkedList<String>();
			Element elementDATA = (Element) itElemDATA.next();
			Iterator<?> itElement = elementDATA.elementIterator();
			while (itElement.hasNext()) {
				Element element = (Element) itElement.next();
				String elmName = element.getName();
				if (elmName.equals("ITEM")) {
					String key = element.attributeValue("key");
					if (key.isEmpty()) {
						logger.warn("索引中数据信息的ITEM项有key值为有空值！");
					} else if (key.equals("J010024") || key.equals("I010032") || key.equals("10A0024")) {
						// 列分隔符
						String val = element.attributeValue("val");
						if (val.isEmpty()) {
							logger.debug("索引中的列分隔符写为了空，默认使用\\t");
							DATA_SPLIT = "\t";
						} else if (val.equals(" ")) {
							logger.warn("索引中的列分隔符写为了一个空格，请检查是否与数据相匹配！请检查索引是否正确！");
						} else if (val.equals("\\t")) {
							DATA_SPLIT = "\t";
						} else if (val.equals("\t")) {
							DATA_SPLIT = "\t";
						} else if (val.equals(",")) {
							logger.warn("索引中的列分隔符写为了一个逗号，请检查是否与数据相匹配！请检查索引是否正确！");
							DATA_SPLIT = val;
						} else {
							logger.warn("索引中的列分隔符比较奇特，请检查是否与数据相匹配！请检查索引是否正确！");
							DATA_SPLIT = val;
						}
					} else if (key.equals("J010025") || key.equals("I010033") || key.equals("10A0025")) {
						// 行分隔符
						String val = element.attributeValue("val");
						if (val.isEmpty()) {
							logger.warn("索引中的行分隔符写为了空，默认使用\\n");
							LINE_SPLIT = val;
						} else if (val.equals("\\n")) {
							LINE_SPLIT = "\n";
						} else if (val.equals("\\r\\n")) {
							LINE_SPLIT = "\r\n";
						} else {
							logger.error("索引中的行分隔符比较奇特，请检查索引是否正确！");
							LINE_SPLIT = val;
						}
					} else if (key.equals("A010004") || key.equals("01A0004")) {
						// 数据集代码，协议
						PROTYPE = element.attributeValue("val");
					} else if (key.equals("B050016") || key.equals("02E0016")) {
						// 数据源
						DATASOURCE = element.attributeValue("val");
					} else if (key.equals("G020013") || key.equals("07B0013")) {
						// 网安专用产品厂家组织机构代码(厂商代码)
						// 可以用来检验
						COMPANY = element.attributeValue("val");
					} else if (key.equals("F010008") || key.equals("06A0008")) {
						// 数据采集地
						// 可以用来检验
						CITYCODE = element.attributeValue("val");
					} else if (key.equals("I010038") || key.equals("10A0027")) {
						// 数据起始行
						START_LINE = element.attributeValue("val");
					} else if (key.equals("I010039") || key.equals("10A0028")) {
						// 数据编码格式
						String val = element.attributeValue("val");
						if (val.isEmpty()) {
							logger.debug("索引中的BCP文件编码格式为空，默认使用UTF-8格式");
							CHARSET = "UTF-8";
						} else if (val.toUpperCase().equals("UTF-8")) {
							CHARSET = element.attributeValue("val");
						} else if (val.equals(" ")) {
							logger.warn("索引中的BCP文件编码格式写为了一个空格！请检查索引是否正确！");
							CHARSET = "UTF-8";
						} else {
							logger.warn("索引中的BCP文件编码格非UTF-8！请检查索引是否正确！");
							CHARSET = element.attributeValue("val");
						}
					} else {
						logger.warn("索引中数据信息的ITEM项有问题，请检查索引！");
					}

				} else if (elmName.equals("DATASET")) {
					// 数据bcp文件信息
					String name = element.attributeValue("name");
					if (name.equals("WA_COMMON_010014") || name.equals("JZ_COMMON_010014")) {
						// BCP数据文件信息,可能有多个子节点“DATA”
						@SuppressWarnings("unchecked")
						Iterator<Element> itDataFiles = element.elementIterator();
						while (itDataFiles.hasNext()) {
							Element DATA = itDataFiles.next();
							@SuppressWarnings("unchecked")
							Iterator<Element> itITEM = DATA.elementIterator();
							while (itITEM.hasNext()) {
								Element elmItem = itITEM.next();
								String key = elmItem.attributeValue("key");
								if (key.equals("H010020") || key.equals("08A0020")) {
									// bcp文件名信息
									String fileName = elmItem.attributeValue("val");
									if (!fileName.isEmpty()) {
										listFiles.add(fileName);
									}
								}
							}
						}
					} else if (name.equals("WA_COMMON_010015") || name.equals("JZ_COMMON_010015")) {
						// BCP文件数据结构,应该只有一个子节点DATA吧
						Element DATA = element.element("DATA");
						
						//检查是否有重复的索引
						Set<String> setKey = new HashSet<String>();
						Set<String> setSame = new HashSet<String>();
						
						@SuppressWarnings("unchecked")
						Iterator<Element> itITEM = DATA.elementIterator();
						while (itITEM.hasNext()) {
							Element item = itITEM.next();
							String key = item.attributeValue("key");
							String chn = item.attributeValue("chn");
							String eng = item.attributeValue("eng");
							if (null == key) {
								logger.error("索引中的字段序列存在空指针，请检查数据索引");
								key = "";
							}
							listDataField.add(key);
							if (null == chn) {
								chn = "";
							}
							listCHNField.add(chn);
							if (null == eng) {
								eng = "";
							}
							listENGField.add(eng);
							// logger.info(key + " : " + eng + " : " + chn);
							
							if(setKey.contains(key)){
								setSame.add(key);
							}
							setKey.add(key);
						}
						
						if(setSame.size()>0){
							logger.error("The file's index GAB_ZIP_INDEX.xml  have same key !Detail: "+setSame.toString());
						}
						
					} else {
						logger.error("索引中数据信息的项有问题，请检查索引！");
					}
				} else {
					logger.error("Index Error! Please check GAB_ZIP_INDEX.xm file!");
				}
			}

			HashMap<String, List<String>> mapDataInfo = new HashMap<String, List<String>>();
			// 仅仅为了记录，我也是醉了

			List<String> listDatasource = new LinkedList<String>();
			listDatasource.add(DATASOURCE);
			mapDataInfo.put("DATASOURCE", listDatasource);

			List<String> listProtype = new LinkedList<String>();
			listProtype.add(PROTYPE);
			mapDataInfo.put("PROTYPE", listProtype);

			List<String> listFieldSplit = new LinkedList<String>();
			listFieldSplit.add(DATA_SPLIT);
			mapDataInfo.put("FIELD_SPLIT", listFieldSplit);

			List<String> listLineSplit = new LinkedList<String>();
			listLineSplit.add(LINE_SPLIT);
			mapDataInfo.put("LINE_SPLIT", listLineSplit);

			List<String> listCompany = new LinkedList<String>();
			listCompany.add(COMPANY);
			mapDataInfo.put("COMPANY", listCompany);

			List<String> listCityCodey = new LinkedList<String>();
			listCityCodey.add(CITYCODE);
			mapDataInfo.put("CITYCODE", listCityCodey);

			List<String> listStartLine = new LinkedList<String>();
			listStartLine.add(START_LINE);
			mapDataInfo.put("START_LINE", listStartLine);

			List<String> listCharset = new LinkedList<String>();
			listCharset.add(CHARSET);
			mapDataInfo.put("CHARSET", listCharset);

			mapDataInfo.put("FIELD", listDataField);
			mapDataInfo.put("CHN", listCHNField);
			mapDataInfo.put("ENG", listENGField);
			for (String fileName : listFiles) {
				mapIndexInfo.put(fileName, mapDataInfo);
			}
			// 补充
			String DATA_PROTYPE = DATASOURCE + "|" + PROTYPE;
			logger.debug(DATASOURCE + "|" + PROTYPE + ":" + listDataField);
			mapIndexInfo.put(DATA_PROTYPE, mapDataInfo);
		}
		return mapIndexInfo;
	}

}
