package com.kingdee.inte.teamworkclient.utils;

import com.kingdee.inte.teamworkclient.common.GLConst;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * description:
 *
 * @author Administrator
 * @date 2020/9/25 9:29
 */
public class KSQLUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(KSQLUtil.class);

	private static final String IF_NOT_EXISTS = "IF NOT EXISTS (";
	private static final String IF_EXISTS = "IF EXISTS (";
	private static final String RIGHT_BRACKET = ")\n";


	/**
	 * Method Description: Created by whx
	 * 〈获取 所有的数据库字段〉
	 *
	 * @param str show table sql 语句
	 * @return java.lang.String 数据库字段以 ，  分割
	 * @throws
	 * @date 2020/9/25 9:36
	 */
	public static String getFieldsFromShowTable(String str) {
		final String REGEX = "\"[A-Z]+\"";
		Pattern p = Pattern.compile(REGEX);
		Matcher m = p.matcher(str);
		List<String> list = new ArrayList<>();
		while (m.find()) {
			String temp = m.group();
			list.add(temp);
		}
		StringBuilder sb = new StringBuilder();
		Iterator<String> iterator = list.iterator();
		while (iterator.hasNext()) {
			String t = iterator.next();
			sb.append(t.substring(1, t.length() - 1));
			if (iterator.hasNext()) {
				sb.append(",");
			}
		}
		LOGGER.info(sb.toString());
		return sb.toString();
	}

	/**
	 * Method Description: Created by whx
	 * 〈IF_NOT_EXISTS 语句〉
	 *
	 * @param tableName   表名
	 * @param nameToValue 字段及值
	 * @return java.lang.String 拼接语句
	 * @date 2020/9/27 18:10
	 */
	public static String getIfNotExistsStatement(String tableName, Map<String, String> nameToValue) {
		return getString(tableName, nameToValue, IF_NOT_EXISTS);
	}

	@NotNull
	private static String getString(String tableName, Map<String, String> nameToValue, String ifNotExists) {
		StringBuilder selectSql = new StringBuilder("SELECT 1 FROM ");
		selectSql.append(tableName);
		Iterator<Map.Entry<String, String>> it = nameToValue.entrySet().iterator();
		if (it.hasNext()) {
			selectSql.append(" WHERE ");
		}
		while (it.hasNext()) {
			Map.Entry entry = it.next();
			selectSql.append(entry.getKey()).append(" = ").append(entry.getValue());
			if (it.hasNext()) {
				selectSql.append(" AND ");
			}
		}
		return ifNotExists + selectSql + RIGHT_BRACKET;
	}

	/**
	 * Method Description: Created by whx
	 * 〈IF_NOT_EXISTS 语句〉
	 *
	 * @param tableName   表名
	 * @param nameToValue 字段及值
	 * @return java.lang.String 拼接语句
	 * @date 2020/9/27 18:10
	 */
	public static String getIfExistsStatement(String tableName, Map<String, String> nameToValue) {
		return getString(tableName, nameToValue, IF_EXISTS);
	}


	/**
	 * Method Description: Created by whx
	 * 〈获取 insert 语句中的 field -> value 键值对〉
	 *
	 * @param sql insert 语句
	 * @return java.util.Map<java.lang.String, java.lang.String> insert 语句中的 field -> value 键值对
	 * @throws
	 * @date 2020/9/27 17:56
	 */
	public static Map<String, String> getInsertMap(String sql) {
		// 替换  值中的  VALUES
		sql = encodeConflictString(sql, "VALUES", "v");
		sql = encodeConflictString(sql, ",", "#");

		String[] namesAndValues = sql.split("VALUES");
		String namesStr = filterNoUsed(namesAndValues[0]);
		namesStr = namesStr.substring(1, namesStr.length() - 1);
		String valuesStr = filterNoUsed(namesAndValues[1]);
		valuesStr = valuesStr.substring(1, valuesStr.length() - 1);
		String[] names = namesStr.split(",");
		String[] values = valuesStr.split(",");
		if (names.length != values.length) {
			throw new RuntimeException("这不是一个合法的 INSERT  sql!!");
		}
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < names.length; i++) {
			String tmp = values[i].trim();
			tmp = decodeConflictString(tmp, "VALUES", "v");
			tmp = decodeConflictString(tmp, ",", "#");
			map.put(names[i].trim(), tmp);
		}
		return map;
	}

	public static String convertToInsertFromInsertSql(String srcSql, String tableName, Map<String, String> paramMap) {
		paramMap.put("FDISABLERID", "0");
		paramMap.put("FMODIFIERID", "0");
		paramMap.put("FCREATORID", "0");
		paramMap.put("FENABLE", "'0'");
		StringBuilder sb = new StringBuilder("INSERT INTO ");
		sb.append(tableName).append(" (");
		List<String> fieldList = new ArrayList<>();
		List<String> valueList = new ArrayList<>();
		paramMap.forEach((k, v) -> {
			fieldList.add(k);
			valueList.add(v);
		});
		Iterator<String> itf = fieldList.iterator();
		while (itf.hasNext()) {
			sb.append(itf.next());
			if (itf.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(") VALUES (");
		Iterator<String> itv = valueList.iterator();
		while (itv.hasNext()) {
			sb.append(itv.next());
			if (itv.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(");");
		return sb.toString();

	}

	private static String filterNoUsed(String str) {
		final String REGEX = "\\([\\s\\S]+\\)";
		Pattern p = Pattern.compile(REGEX);
		Matcher m = p.matcher(str);
		List<String> list = new ArrayList<>();
		if (m.find()) {
			return m.group();
		} else {
			throw new RuntimeException("这不是一个合法的 INSERT  sql!!");
		}

	}

	private static String encodeConflictString(String fullStr, String sourceChars, String replaceChar) {
		final String rpl = GLConst.REPLACE_STR + replaceChar;

		List<String> fullPartList = new ArrayList<>();
		boolean isSingleQuot = false;
		StringBuilder fullPartBuilder = new StringBuilder();
		for (int i = 0; i < fullStr.length(); i++) {
			char ch = fullStr.charAt(i);
			if (ch == '\'') {
				if (!isSingleQuot) {
					fullPartBuilder.append(ch);
					fullPartList.add(fullPartBuilder.toString());
					isSingleQuot = true;
					fullPartBuilder = new StringBuilder(ch);
				} else {
					String tmp = fullPartBuilder.append(ch).toString();
					if (tmp.contains(sourceChars)) {
						tmp = tmp.replace(sourceChars, rpl);
					}
					fullPartList.add(tmp);
					fullPartBuilder = new StringBuilder();
					isSingleQuot = false;
				}
			} else {
				if (ch == ',' && !isSingleQuot) {
					fullPartBuilder.append(ch);
					fullPartList.add(fullPartBuilder.toString());
					fullPartBuilder = new StringBuilder();
				} else {
					fullPartBuilder.append(ch);
				}
			}
		}
		fullPartList.add(fullPartBuilder.toString());
		StringBuilder sb = new StringBuilder();
		fullPartList.forEach(sb::append);
		return sb.toString();
	}

	private static String decodeConflictString(String fullStr, String sourceChars, String replaceChar) {
		final String rpl = GLConst.REPLACE_STR + replaceChar;
		return fullStr.replace(rpl, sourceChars);
	}

	public static String convertToUpdateSql(String table, String insertSql) {
		Map<String, String> insertSqlMap = KSQLUtil.getInsertMap(insertSql);
		insertSqlMap.put("FDISABLERID", "0");
		insertSqlMap.put("FMODIFIERID", "0");
		insertSqlMap.put("FCREATORID", "0");
		String ffullname = insertSqlMap.get("FFULLNAME");
		if (StringUtils.equals(insertSqlMap.get("FLEVEL"), "1") && ffullname.contains("_")) {
			ffullname = ffullname.substring(0, ffullname.length() - 2) + "'";
			insertSqlMap.put("FFULLNAME", ffullname);
			insertSqlMap.put("FNAME", ffullname);
		}

		StringBuilder updateSqlSb = new StringBuilder("UPDATE ");
		updateSqlSb.append(table).append(" SET ");
		Iterator<Map.Entry<String, String>> it = insertSqlMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			String name = entry.getKey();
			String value = entry.getValue();
			if (!StringUtils.equals(name, "FID")) {
				updateSqlSb.append(String.format("%s = %s", name, value));
				if (it.hasNext()) {
					updateSqlSb.append(", ");
				}
			}
		}
		updateSqlSb.append(String.format(" WHERE FID = %s", insertSqlMap.get("FID"))).append(";\n");
		return updateSqlSb.toString();
	}

}
