package com.kingdee.inte.teamworkclient.web.controller.maintest;

import com.kingdee.inte.teamworkclient.utils.ExcelReaderUtil;
import com.kingdee.inte.teamworkclient.utils.KSQLUtil;
import com.kingdee.inte.teamworkclient.utils.MyFileReader;
import com.kingdee.inte.teamworkclient.utils.MyFileWriter;
import com.kingdee.inte.teamworkclient.web.controller.maintest.consts.CADFCConst;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * description:
 *
 * @author Administrator
 * @date 2020/9/25 9:31
 */
@RestController
@RequestMapping(value = "/KSQL/v1")
public class KSQLController {
	private static final Logger LOGGER = LoggerFactory.getLogger(KSQLController.class);

	private int index = 0;

	@GetMapping("/fields")
	public String getFieldFromShowTable(String str) {
		return KSQLUtil.getFieldsFromShowTable(str);
	}

	@GetMapping("/gen/sql")
	public String generateSql() {
		final String sqlFilePath = "E:\\行政区划\\T_BD_ADMINDIVISION_20200927173549.sql";
		StringBuilder result = new StringBuilder();
		File sqlFile = new File(sqlFilePath);
		LOGGER.info("开始读取sql文件");
		List<String> allInsertSqlLineList = MyFileReader.getWordContainsLinesIgnoreCase(sqlFile, "INSERT INTO");
		List<Map<String, String>> allInsertNameToValueList = new ArrayList<>();
		List<Map<String, String>> allInsertFidToSqlList = new ArrayList<>();
		double total = allInsertSqlLineList.size();
		AtomicInteger i = new AtomicInteger(1);
		allInsertSqlLineList.forEach(line -> {
			Map<String, String> insertSqlMap = KSQLUtil.getInsertMap(line);
			allInsertNameToValueList.add(insertSqlMap);
			Map<String, String> map = new HashMap<>();
			map.put(insertSqlMap.get("FID"), line);
			allInsertFidToSqlList.add(map);
			LOGGER.info("已完成 {}%", i.getAndIncrement() / total * 100);
		});
		// 如果不存在即 进行insert语句
		ifNotExistInsert(result, allInsertFidToSqlList);

		// 如果存在即 进行update操作
		Map<String, String> idToSqlMap = new HashMap<>();
		allInsertFidToSqlList.forEach(idToSqlMap::putAll);
		ifExistUpdate(result, allInsertNameToValueList, idToSqlMap);
		List<String> list = new ArrayList<>();
		list.add(result.toString());
		MyFileWriter.write("E:\\行政区划\\", "T_BD_ADMINDIVISION_FIXED.sql", list);
		LOGGER.info("程序运行结束》》》");
		return result.toString();
	}

	@GetMapping("/admindivisioncode/init")
	public String initAdminDivisionCode() {
		StringBuilder sb = new StringBuilder();
		String updateDataPath = "E:\\行政区划\\中国行政区划数据更新_V1.0_2020.08-2.xlsx";
		List<Map<String, String>> updateDataList = getCellValueList(updateDataPath, 1, 1);
		Map<String, String> numToCode = new LinkedHashMap<>();
		updateDataList.forEach(item -> {
			numToCode.put(item.get("A"), item.get("L"));
		});
		/// ddd
		final String sqlFilePath = "E:\\行政区划\\T_BD_ADMINDIVISION_20200927173549.sql";
		StringBuilder result = new StringBuilder();
		File sqlFile = new File(sqlFilePath);
		LOGGER.info("开始读取sql文件");
		List<String> allInsertSqlLineList = MyFileReader.getWordContainsLinesIgnoreCase(sqlFile, "INSERT INTO");
		Map<String, String> numberToAreaCode = new LinkedHashMap<>();
		double total = allInsertSqlLineList.size();
		AtomicInteger i = new AtomicInteger(1);
		allInsertSqlLineList.forEach(line -> {
			Map<String, String> insertSqlMap = KSQLUtil.getInsertMap(line);
			String fid = insertSqlMap.get("FID");
			String fnumber = insertSqlMap.get("FNUMBER");
			String nfid = CADFCConst.id2id.get(fnumber.substring(1, fnumber.length() - 1));
			if (StringUtils.isNotBlank(nfid)) {
				fid = nfid;
			}
			numberToAreaCode.put(fnumber, fid);
			Map<String, String> map = new HashMap<>();
			LOGGER.info("已完成 {}%", i.getAndIncrement() / total * 100);
		});

		//dddd
		Iterator<Map.Entry<String, String>> it = numToCode.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			String fid = numberToAreaCode.get("'" + entry.getKey() + "'");
			String ifExist = String.format("IF EXISTS (SELECT 1 FROM T_BD_ADMINDIVISION WHERE FID = %s)", fid);
			String updateSql = String.format("UPDATE T_BD_ADMINDIVISION SET FAREACODE = '%s' WHERE FID = %s;", entry.getValue(), fid);
			sb.append(ifExist).append("\n");
			sb.append(updateSql).append("\n");
		}
		List<String> list = new ArrayList<>();
		list.add(sb.toString());
		MyFileWriter.write("E:\\行政区划\\", "T_BD_ADMINDIVISION_AREACODE_INIT.sql", list);
		LOGGER.info("程序运行结束》》》");
		return sb.toString();

	}


	@GetMapping("/all-ml")
	public String generateMultiLanSql() {
		LOGGER.info("开始解析 T_BD_ADMINDIVISION_FIXED_ALL_WITHOUT_L.sql ");
		String filePath = "E:\\行政区划\\T_BD_ADMINDIVISION_FIXED_ALL_WITHOUT_L.sql";
		Stack<String> sqlStack = new Stack<>();
		List<String> allLine = MyFileReader.getAllLines(new File(filePath));
		allLine.forEach(sqlStack::push);

		Stack<String> resultStack = new Stack<>();
		while (sqlStack != null && !sqlStack.empty() && sqlStack.peek() != null) {
			String item = sqlStack.pop();
			String sqlItemGroup = "";
			if (item.contains("UPDATE") && item.contains("SET")) {
				sqlItemGroup = handleUpdate(item, sqlStack.pop());
			}

			if (item.contains("INSERT") && item.contains("INTO") && item.contains("VALUES")) {
				sqlItemGroup = handleInsert(item, sqlStack.pop());
			}
			if (StringUtils.isNotBlank(sqlItemGroup)) {
				resultStack.push(sqlItemGroup);
			} else {
				throw new RuntimeException("sql 中有异常数据， 请查看！");
			}
		}

		StringBuilder resultsb = new StringBuilder();

		while (resultStack != null && !resultStack.empty() && resultStack.peek() != null) {
			resultsb.append(resultStack.pop());
		}
		List<String> list = new ArrayList<>();
		list.add(resultsb.toString());
		MyFileWriter.write("E:\\行政区划", "T_BD_ADMINDIVISION_FIXED_ALL_WITH_L.sql", list);
		LOGGER.info("程序运行结束》》》");
		return resultsb.toString();
	}

	private String handleInsert(String insertSql, String prevSql) {
		String result = prevSql + "\n";
		result += insertSql + "\n";
		result += prevSql.replace("IF NOT EXISTS", "IF EXISTS") + "\n";
		Map<String, String> map = KSQLUtil.getInsertMap(insertSql);
		String insertLSql = String.format("INSERT INTO T_BD_ADMINDIVISION_L " +
						"(FPKID, FID, FLOCALEID, FNAME, FFULLNAME, FDESCRIPTION, FSIMPLENAME) VALUES ('%s', %s, 'zh_CN', %s, %s, %s, %s);",
				CADFCConst.PKIDS120[index], map.get("FID"), map.get("FNAME"), map.get("FFULLNAME"), map.get("FDESCRIPTION"), "' '");
		index++;
		result += insertLSql + "\n";
		return result;
	}

	private String handleUpdate(String updateSql, String prevSql) {
		String result = prevSql + "\n";
		result += updateSql + "\n";
		result += prevSql + "\n";
		Map<String, String> upSqlMap = getUpdateMap(updateSql);
		String updateLSql = String.format("UPDATE SET T_BD_ADMINDIVISION_L FNAME = %s, FFULLNAME = %s, " +
						"FDESCRIPTION = %s WHERE FLOCALEID = 'zh_CN' AND FID = %s;", upSqlMap.get("FNAME"), upSqlMap.get("FFULLNAME"),
				upSqlMap.get("FDESCRIPTION"), upSqlMap.get("FID"));
		result += updateLSql + "\n";
		return result;
	}

	private Map<String, String> getUpdateMap(String sql) {
		int nameIndex = sql.indexOf("FNAME");
		int fullNameIndex = sql.indexOf("FFULLNAME");
		int descIndex = sql.indexOf("FDESCRIPTION");
		int idIndex = sql.indexOf("FID");
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("FNAME", getVal(nameIndex, sql, '\''));
		resultMap.put("FFULLNAME", getVal(fullNameIndex, sql, '\''));
		resultMap.put("FDESCRIPTION", getVal(descIndex, sql, '\''));
		resultMap.put("FID", getVal(idIndex, sql, ';'));
		return resultMap;
	}

	private String getVal(int index, String sql, char ch) {
		StringBuilder result = new StringBuilder("");
		int flag = 0;
		for (int i = index; i < sql.length(); i++) {
			if (ch == ';') {
				if (sql.charAt(i) >= 48 && sql.charAt(i) <= 57) {
					result.append(sql.charAt(i));
				}
			} else {
				if (sql.charAt(i) == ch) {
					flag++;
				}
				if (flag == 1) {
					result.append(sql.charAt(i));
				}
			}
		}
		if (ch == '\'') {
			result.append("'");
		}
		return result.toString();
	}

	private void ifExistUpdate(StringBuilder result, List<Map<String, String>> allInsertNameToValueList,
	                           Map<String, String> idToSqlMap) {
		String baselinePath = "E:\\行政区划\\引出数据_行政区划_0902引入模板-基线.xlsx";
		String numberToIdFilePath = "E:\\行政区划\\引出数据_带内码_0925.xlsx";
		List<Map<String, String>> numberToIdFileList = getCellValueList(numberToIdFilePath, 0, 4);
		List<Map<String, String>> baselineList = getCellValueList(baselinePath, 0, 3);

		Map<String, String> numberToIdMap = new HashMap<>();
		numberToIdFileList.forEach(item -> {
			numberToIdMap.put(item.get("B"), item.get("A"));
		});

		baselineList.forEach(item -> {
			Map<String, String> map = new LinkedHashMap<>();
			String fid = numberToIdMap.get(item.get("A"));
			map.put("FID", fid);
			map.put("FNUMBER", '\'' + item.get("A") + '\'');
			map.put("FNAME", '\'' + item.get("B") + '\'');
			String fstatus = "";
			if (StringUtils.equals(item.get("E"), "已审核")) {
				fstatus = "'C'";
			} else if (StringUtils.equals(item.get("E"), "已审核")) {
				fstatus = "'B'";
			} else {
				fstatus = "'A'";
			}
			map.put("FSTATUS", fstatus);
			String fenable;
			if (StringUtils.equals(item.get("H"), "可用")) {
				fenable = "'1'";
			} else {
				fenable = "'0'";
			}
			map.put("FENABLE", fenable);
			map.put("FLONGNUMBER", '\'' + item.get("J") + '\'');
			map.put("FLEVEL", item.get("K"));
			map.put("FFULLNAME", '\'' + (StringUtils.isBlank(item.get("L")) ? " " : item.get("L")) + '\'');
			String fisleaf;
			if (StringUtils.equals(item.get("O"), "是")) {
				fisleaf = "'1'";
			} else {
				fisleaf = "'0'";
			}
			map.put("FISLEAF", fisleaf);
			map.put("FDESCRIPTION", '\'' + (StringUtils.isBlank(item.get("U")) ? " " : item.get("U")) + '\'');
			map.put("FFULLSPELL", '\'' + (StringUtils.isBlank(item.get("X")) ? " " : item.get("X")) + '\'');
			map.put("FSIMPLESPELL", '\'' + (StringUtils.isBlank(item.get("Y")) ? " " : item.get("Y")) + '\'');
			map.put("FCITYNUMBER", '\'' + (StringUtils.isBlank(item.get("Z")) ? " " : item.get("Z")) + '\'');
			String fisCity;
			if (StringUtils.equals("是", item.get("AA"))) {
				fisCity = "'1'";
			} else {
				fisCity = "'0'";
			}
			map.put("FISCITY", fisCity);

			String ifExistsSql = KSQLUtil.getIfExistsStatement("T_BD_ADMINDIVISION", map);
			result.append(ifExistsSql);
			String updateSql = KSQLUtil.convertToUpdateSql("T_BD_ADMINDIVISION", idToSqlMap.get(fid));
			result.append(updateSql);
		});


	}

	private List<Map<String, String>> getCellValueList(String path, int sheetIndex, int headRowSize) {
		List<Map<String, Cell>> rowList = ExcelReaderUtil.readXSheetAllCell(path, sheetIndex, headRowSize);
		List<Map<String, String>> rowStrList = new ArrayList<>();
		rowList.forEach(row -> {
			Map<String, String> map = new HashMap<>();
			row.forEach((cellLabel, cellVal) -> {
				map.put(cellLabel, ExcelReaderUtil.getCellValue(cellVal));
			});
			rowStrList.add(map);
		});
		return rowStrList;
	}

	private void ifNotExistInsert(StringBuilder result, List<Map<String, String>> allInsertFidToSqlList) {
		String numberToIdFilePath = "E:\\行政区划\\引出数据_带内码_0925.xlsx";
		List<Map<String, String>> numberToIdFileList = getCellValueList(numberToIdFilePath, 0, 4);
		Map<String, String> numberToIdMap = new HashMap<>();
		numberToIdFileList.forEach(item -> {
			numberToIdMap.put(item.get("A"), item.get("B"));
		});

		allInsertFidToSqlList.forEach(item -> {
			Iterator<Map.Entry<String, String>> it = item.entrySet().iterator();
			Map.Entry<String, String> entry = null;
			if (it.hasNext()) {
				entry = it.next();
				Map<String, String> map = new HashMap<>();
				map.put("FID", entry.getKey());
				if (numberToIdMap.get(map.get("FID")) == null) {
					Map<String, String> insertSqlMap = KSQLUtil.getInsertMap(entry.getValue());
					String fnumber = insertSqlMap.get("FNUMBER");
					String nfid = CADFCConst.id2id.get(fnumber.substring(1, fnumber.length() - 1));
					if (StringUtils.isNotBlank(nfid)) {
						map.put("FID", nfid);
						insertSqlMap.put("FID", nfid);
						insertSqlMap.put("FMASTERID", nfid);
					}
					result.append(KSQLUtil.getIfNotExistsStatement("T_BD_ADMINDIVISION", map));
					result.append(KSQLUtil.convertToInsertFromInsertSql(entry.getValue(), "T_BD_ADMINDIVISION", insertSqlMap)).append("\n");
				}
			} else {
				throw new RuntimeException("有数据异常请检查！！！");
			}
		});
	}


}
