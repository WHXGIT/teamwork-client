package com.kingdee.inte.teamworkclient.web.controller.maintest;

import com.kingdee.inte.teamworkclient.common.Builder;
import com.kingdee.inte.teamworkclient.pojo.ExcelDO;
import com.kingdee.inte.teamworkclient.utils.Columns;
import com.kingdee.inte.teamworkclient.utils.ExcelReaderUtil;
import com.kingdee.inte.teamworkclient.utils.ExcelWriterUtil;
import com.kingdee.inte.teamworkclient.web.controller.maintest.consts.CADFCConst;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * description: 用于测试的Controller
 *
 * @author Administrator
 * @date 2020/9/23 14:35
 */
@RestController
@RequestMapping("/china-adf/v1")
public class ChinaAdminDivisionFixedController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChinaAdminDivisionFixedController.class);

	private Map<String, Map<String, String>> baselineResMap;

	private static final List<ExcelDO> excelDOList = new ArrayList<>();

	private static List<Integer> numberList = null;

	@GetMapping("/enter")
	public void main() {

		String baselinePath = "E:\\行政区划\\引出数据_行政区划_0902引入模板-基线.xlsx";
		String handledPath = "E:\\行政区划\\中国行政区划数据更新_V1.0_2020.08-1.xlsx";
		try {
			List<Map<String, String>> baselineList = getCellValueList(baselinePath, 0, 3);

			List<Map<String, String>> handledCellList = getCellValueList(handledPath, 0, 1);

			Map<String, Map<String, String>> standerDataMap = convertToMap(handledCellList);
			baselineResMap = convertToMap(baselineList);

			LOGGER.info("程序读取Excel执行结束！");
			//依次执行以下内容
			fixDataNameAndFullname(baselineList, standerDataMap, "1");
			fixDataNameAndFullname(baselineList, standerDataMap, "2");
			fixDataNameAndFullname(baselineList, standerDataMap, "3");

			LOGGER.info("文件写入到 目标文件");
			String targetPath = "E:\\行政区划\\引出数据_行政区划_0902引入模板-基线-v2.xlsx";
			ExcelWriterUtil.updateWrite(baselinePath, targetPath, 0, 3, baselineList);
			//************************************为每条数据添加 省/市/区/县/镇 结束***********************************
			LOGGER.info("文件，{}创建成功", targetPath);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		LOGGER.info("程序执行结束！");
		//ExcelReaderUtil.readWriteExcelSheetData(path, 3);
	}

	@GetMapping("/enter2")
	public void main2() {
		String sourcePath = "E:\\行政区划\\引出数据_行政区划_0902引入模板-基线-v2.xlsx";
		LOGGER.info("处理数据缺失的问题开始...");
		Workbook workbook = null;
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourcePath))) {
			// 直接传入输入流即可，此时excel就已经解析了
			workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheetAt(0);
			List<Row> rowList = ExcelReaderUtil.getRowList(sheet, 3);
			rowList.forEach(row -> {
				Map<String, Cell> cellMap = ExcelReaderUtil.getCellMap(row);
				ExcelDO excelDO = buildExcelDO(cellMap);
				excelDOList.add(excelDO);
			});
			List<List<String>> writeNewList = fixLost(excelDOList);
			for (List<String> newCell : writeNewList) {
				Row row = sheet.createRow(sheet.getLastRowNum() + 1);
				for (int i = 0; i < 31; i++) {
					Cell cell = row.createCell(i);
					cell.setCellValue(newCell.get(i));
				}
			}
			String filePath = "E:\\行政区划\\引出数据_行政区划_0902引入模板-基线-v3.xlsx";
			FileOutputStream fos = new FileOutputStream(filePath);
			workbook.write(fos);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
		LOGGER.info("缺失数据已经添加");
	}

	@GetMapping("/enter3")
	public void main3() {
		String sourcePath = "E:\\行政区划\\引出数据_行政区划_0902引入模板-基线-v3.xlsx";
		LOGGER.info("处理剩余的其他问题开始...");

		// 指定excel文件，创建缓存输入流
		Workbook workbook = null;
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourcePath));) {
			// 直接传入输入流即可，此时excel就已经解析了
			workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheetAt(0);
			List<Row> rowList = ExcelReaderUtil.getRowList(sheet, 3);
			rowList.forEach(row -> {
				Map<String, Cell> cellMap = ExcelReaderUtil.getCellMap(row);
				doOptions(cellMap);
			});
			String filePath = "E:\\行政区划\\引出数据_行政区划_0902引入模板-基线-v4.xlsx";
			FileOutputStream fos = new FileOutputStream(filePath);
			workbook.write(fos);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		LOGGER.info("处理所有问题结束， 加油！！！");
	}



	public void doOptions(Map<String, Cell> cellMap) {
		fixNameToCorrect(cellMap);
		fixLevelError(cellMap);
		fixErrorName(cellMap);
		fixMunicipality(cellMap);
	}

	/**
	 * @param cellMap 一行cell
	 * @return void
	 * @throws
	 * @description 〈 名称不规范 的问题〉
	 * @date 2020/9/4 13:49
	 * @author RD_haoxin_wang
	 */
	public static void fixNameToCorrect(Map<String, Cell> cellMap) {
		ExcelDO excelDO = buildExcelDO(cellMap);
		if (StringUtils.isNotEmpty(CADFCConst.number2Value.get(excelDO.getNumber()))) {
			cellMap.get(Columns.getIndexLabel(1)).setCellValue(CADFCConst.number2Value.get(excelDO.getNumber()));
			String[] nameLevels = excelDO.getLongName().split("_");
			String newLongName = "";
			for (int i = 0; i < nameLevels.length - 1; i++) {
				newLongName += nameLevels[i] + "_";
			}
			newLongName += CADFCConst.number2Value.get(excelDO.getNumber());
			cellMap.get(Columns.getIndexLabel(11)).setCellValue(newLongName);
			cellMap.get(Columns.getIndexLabel(20)).setCellValue(CADFCConst.number2Value.get(excelDO.getNumber()));
		}
	}

	/**
	 * @param cellMap
	 * @return void
	 * @throws
	 * @description 〈更新所属父级〉
	 * @date 2020/9/4 14:24
	 * @author RD_haoxin_wang
	 */
	public void fixLevelError(Map<String, Cell> cellMap) {

		ExcelDO excelDO = buildExcelDO(cellMap);
		if (StringUtils.isNotEmpty(CADFCConst.number2CityDisValue.get(excelDO.getNumber()))) {
			cellMap.get(Columns.getIndexLabel(1)).setCellValue(CADFCConst.number2CityDisValue.get(excelDO.getNumber()));
			String[] nameLevels = excelDO.getLongName().split("_");
			String newLongName = nameLevels[0] + "_" + CADFCConst.number2CityDisValue.get(excelDO.getNumber());
			String[] longNumbers = excelDO.getLongNumber().split("\\.");
			String newLongNumber = longNumbers[0] + "." + longNumbers[2];
			cellMap.get(Columns.getIndexLabel(10)).setCellValue("2");
			cellMap.get(Columns.getIndexLabel(11)).setCellValue(newLongName);
			String longNumber = ExcelReaderUtil.getCellValue(cellMap.get("J"));
			Map<String, String> map = findNo2Name(longNumber, ".");
			List<String> numberList = new ArrayList<>(map.keySet());
			List<String> nameList = new ArrayList<>(map.values());
			cellMap.get("J").setCellValue(numberList.get(0) + "." + numberList.get(2));
			cellMap.get(Columns.getIndexLabel(15)).setCellValue(numberList.get(0));
			cellMap.get(Columns.getIndexLabel(16)).setCellValue(nameList.get(0));
			cellMap.get(Columns.getIndexLabel(20)).setCellValue(CADFCConst.number2CityDisValue.get(excelDO.getNumber()));
			cellMap.get(Columns.getIndexLabel(26)).setCellValue("是");
			cellMap.get(Columns.getIndexLabel(27)).setCellValue("002");
			cellMap.get(Columns.getIndexLabel(28)).setCellValue("市");
		}
	}

	/**
	 * @param cellMap
	 * @return void
	 * @throws
	 * @description 〈错别字更正〉
	 * @date 2020/9/4 14:29
	 * @author RD_haoxin_wang
	 */
	public static void fixErrorName(Map<String, Cell> cellMap) {
		ExcelDO excelDO = buildExcelDO(cellMap);
		if (StringUtils.isNotEmpty(CADFCConst.number2NameError.get(excelDO.getNumber()))) {
			cellMap.get(Columns.getIndexLabel(1)).setCellValue(CADFCConst.number2NameError.get(excelDO.getNumber()));
			String[] nameLevels = excelDO.getLongName().split("_");
			String newLongName = "";
			for (int i = 0; i < nameLevels.length - 1; i++) {
				newLongName += nameLevels[i] + "_";
			}
			newLongName += CADFCConst.number2NameError.get(excelDO.getNumber());
			cellMap.get(Columns.getIndexLabel(11)).setCellValue(newLongName);
			cellMap.get(Columns.getIndexLabel(20)).setCellValue(CADFCConst.number2NameError.get(excelDO.getNumber()));
		}
	}

	/**
	 * @param cellMap
	 * @return void
	 * @throws
	 * @description 〈直辖市问题〉
	 * @date 2020/9/4 14:40
	 * @author RD_haoxin_wang
	 */
	public void fixMunicipality(Map<String, Cell> cellMap) {
		ExcelDO excelDO = buildExcelDO(cellMap);
		// 二级禁用
		if (StringUtils.isNotEmpty(CADFCConst.number2SecondDCity.get(excelDO.getNumber()))) {
			cellMap.get(Columns.getIndexLabel(7)).setCellValue("禁用");
		}
		// 三级变二级
		if (StringUtils.isNotEmpty(CADFCConst.number2SecondDCity.get(excelDO.getParentNumber()))) {
			String[] levelNumbers = excelDO.getLongNumber().split("\\.");
			String newLongNumber = levelNumbers[0] + "." + levelNumbers[2];
			cellMap.get(Columns.getIndexLabel(9)).setCellValue(newLongNumber);
			cellMap.get(Columns.getIndexLabel(10)).setCellValue("2");
			String[] longNames = excelDO.getLongName().split("_");
			cellMap.get(Columns.getIndexLabel(11)).setCellValue(longNames[0] + "_" + longNames[2]);
			cellMap.get(Columns.getIndexLabel(15)).setCellValue(levelNumbers[0]);
			cellMap.get(Columns.getIndexLabel(26)).setCellValue("是");
			cellMap.get(Columns.getIndexLabel(27)).setCellValue("002");
			cellMap.get(Columns.getIndexLabel(28)).setCellValue("市");
		}
	}

	/**
	 * @param excelDOList
	 * @return void
	 * @throws
	 * @description 〈缺失数据〉
	 * @date 2020/9/4 15:08
	 * @author RD_haoxin_wang
	 */
	public static List<List<String>> fixLost(List<ExcelDO> excelDOList) {
		Map<String, List<String>> number2NameIdMap = new HashMap<>();
		List<List<String>> newCellList = new ArrayList<>();

		CADFCConst.number2LostNumber.forEach((number2Name, number) -> {
			List<ExcelDO> list = listBrother(excelDOList, number);
			String newNumber = increaseOne(number2NameIdMap, number, list);
			String parentLongNumber = list.get(0).getLongNumber().split("\\.")[0] + "." + list.get(0).getLongNumber().split("\\.")[1];
			String parentLongName = list.get(0).getLongName().split("_")[0] + "_" + list.get(0).getLongName().split("_")[1];
			List<String> cellValueList = new ArrayList<>();
			cellValueList.add(newNumber);
			cellValueList.add(number2Name.split("_")[1]);
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add("已审核");
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add("可用");
			cellValueList.add("2020-09-08 00:00:00");
			cellValueList.add(parentLongNumber + "." + newNumber);
			cellValueList.add("3");
			cellValueList.add(parentLongName + "_" + number2Name.split("_")[1]);
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add(list.get(0).getIsLeafNode());
			cellValueList.add(list.get(0).getParentNumber());
			cellValueList.add(list.get(0).getParentName());
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add(number2Name.split("_")[1]);
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add("");
			cellValueList.add(list.get(0).getPhoneAreaNumber());
			cellValueList.add(list.get(0).getIsCity());
			cellValueList.add(list.get(0).getLevelNumber());
			cellValueList.add(list.get(0).getLevelName());
			cellValueList.add(list.get(0).getCountryNumber());
			cellValueList.add(list.get(0).getCountryName());
			newCellList.add(cellValueList);
		});
		return newCellList;
	}

	public static String increaseOne(Map<String, List<String>> number2NameIdMap, String number, List<ExcelDO> list) {

		if (numberList == null) {
			numberList = excelDOList.stream()
					.map(item -> Integer.valueOf(item.getNumber()))
					.sorted().collect(Collectors.toList());
		} else {
			numberList = numberList.stream()
					.sorted().collect(Collectors.toList());
		}

		int newNumber = numberList.get(numberList.size() - 1) + 1;
		String newNumberStr = String.valueOf(newNumber);
		int newNumberLen = newNumberStr.length();
		for (int i = 0; i < list.get(0).getNumber().length() - newNumberLen; i++) {
			newNumberStr = "0" + newNumberStr;
		}
		numberList.add(newNumber);
		return newNumberStr;
	}


	public static List<ExcelDO> listBrother(List<ExcelDO> excelDOList, String parentNumber) {
		List<ExcelDO> list = excelDOList.stream()
				.filter(excelDO -> parentNumber.equals(excelDO.getParentNumber()))
				.sorted(Comparator.comparing(ExcelDO::getNumber))
				.collect(Collectors.toList());
		return list;
	}

	private Map<String, Map<String, String>> convertToMap(List<Map<String, String>> handledCellList) {
		Map<String, Map<String, String>> standerDataMap = new LinkedHashMap<>();
		handledCellList.forEach(cellVal -> {
			standerDataMap.put(cellVal.get("A"), cellVal);
		});
		return standerDataMap;
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


	/**
	 * Method Description: Created by whx
	 * 〈 为市级数据 添加 市、 区 等值〉
	 *
	 * @param baselineList   源数据
	 * @param standerDataMap 标准数据
	 * @date 2020/9/23 18:29
	 */
	private void fixDataNameAndFullname(List<Map<String, String>> baselineList, Map<String, Map<String, String>> standerDataMap, String level) {
		baselineList.forEach(rowMap -> {
			if (StringUtils.equals(rowMap.get("K"), level)) {
				String no = rowMap.get("A");
				String stdName = standerDataMap.get(no).get("K");
				if (StringUtils.isNotBlank(stdName)) {
					rowMap.put("B", stdName);
				}
				List<String> nameList = new ArrayList<>(findNo2Name(rowMap.get("J"), ".").values());
				Iterator<String> iterator = nameList.iterator();
				StringBuilder sb = new StringBuilder();
				while (iterator.hasNext()) {
					String name = iterator.next();
					sb.append(name);
					if (iterator.hasNext()) {
						sb.append('_');
					}
				}
				rowMap.put("L", sb.toString());
			}
		});
	}

	/**
	 * Method Description: Created by whx
	 * 〈  根据长名称 寻找 毎一级的  编码和名称  〉
	 *
	 * @param longNumber 长编码
	 * @return java.util.Map<java.lang.String, java.lang.String>
	 * @date 2020/9/23 18:36
	 */
	private Map<String, String> findNo2Name(String longNumber, String splitSpa) {
		if (StringUtils.isBlank(longNumber)) {
			throw new RuntimeException("空的长编码");
		}
		Map<String, String> map = new LinkedHashMap<>();
		String reg = splitSpa;
		if (StringUtils.equals(".", splitSpa)) {
			reg = "\\.";
		}
		String[] longNumbers = longNumber.split(reg);
		for (String number : longNumbers) {
			Map<String, String> rowMap = findNameByNo(number);
			map.put(number, rowMap.get("B"));
		}
		return map;

	}

	/**
	 * Method Description: Created by whx
	 * 〈 根据编码 查询对应数据〉
	 *
	 * @param number 编码
	 * @return java.util.Map<java.lang.String, java.lang.String>  行数据*
	 * @date 2020/9/23 18:52
	 */
	private Map<String, String> findNameByNo(String number) {
		return baselineResMap.get(number);
	}

	private static ExcelDO buildExcelDO(Map<String, Cell> cellMap) {
		ExcelDO excelDO = Builder.of(ExcelDO::new)
				.with(ExcelDO::setNumber, ExcelReaderUtil.getCellValue(cellMap.get("A")))
				.with(ExcelDO::setName, ExcelReaderUtil.getCellValue(cellMap.get("B")))
				.with(ExcelDO::setEnName, ExcelReaderUtil.getCellValue(cellMap.get("C")))
				.with(ExcelDO::setTrName, ExcelReaderUtil.getCellValue(cellMap.get("D")))
				.with(ExcelDO::setDataStatus, ExcelReaderUtil.getCellValue(cellMap.get("E")))
				.with(ExcelDO::setCreatorNumber, ExcelReaderUtil.getCellValue(cellMap.get("F")))
				.with(ExcelDO::setCreatorName, ExcelReaderUtil.getCellValue(cellMap.get("G")))
				.with(ExcelDO::setUseStatus, ExcelReaderUtil.getCellValue(cellMap.get("H")))
				.with(ExcelDO::setCreateTime, ExcelReaderUtil.getCellValue(cellMap.get("I")))
				.with(ExcelDO::setLongNumber, ExcelReaderUtil.getCellValue(cellMap.get("J")))
				.with(ExcelDO::setLevel, ExcelReaderUtil.getCellValue(cellMap.get("K")))
				.with(ExcelDO::setLongName, ExcelReaderUtil.getCellValue(cellMap.get("L")))
				.with(ExcelDO::setLongEnName, ExcelReaderUtil.getCellValue(cellMap.get("M")))
				.with(ExcelDO::setLongTrName, ExcelReaderUtil.getCellValue(cellMap.get("N")))
				.with(ExcelDO::setIsLeafNode, ExcelReaderUtil.getCellValue(cellMap.get("O")))
				.with(ExcelDO::setParentNumber, ExcelReaderUtil.getCellValue(cellMap.get("P")))
				.with(ExcelDO::setParentName, ExcelReaderUtil.getCellValue(cellMap.get("Q")))
				.with(ExcelDO::setForbiddenNumber, ExcelReaderUtil.getCellValue(cellMap.get("R")))
				.with(ExcelDO::setForbiddenName, ExcelReaderUtil.getCellValue(cellMap.get("S")))
				.with(ExcelDO::setForbiddenTime, ExcelReaderUtil.getCellValue(cellMap.get("T")))
				.with(ExcelDO::setDesc, ExcelReaderUtil.getCellValue(cellMap.get("U")))
				.with(ExcelDO::setEnDesc, ExcelReaderUtil.getCellValue(cellMap.get("V")))
				.with(ExcelDO::setTrDesc, ExcelReaderUtil.getCellValue(cellMap.get("W")))
				.with(ExcelDO::setEnFullName, ExcelReaderUtil.getCellValue(cellMap.get("X")))
				.with(ExcelDO::setEnSimpleName, ExcelReaderUtil.getCellValue(cellMap.get("Y")))
				.with(ExcelDO::setPhoneAreaNumber, ExcelReaderUtil.getCellValue(cellMap.get("Z")))
				.with(ExcelDO::setIsCity, ExcelReaderUtil.getCellValue(cellMap.get("AA")))
				.with(ExcelDO::setLevelNumber, ExcelReaderUtil.getCellValue(cellMap.get("AB")))
				.with(ExcelDO::setLevelName, ExcelReaderUtil.getCellValue(cellMap.get("AC")))
				.with(ExcelDO::setCountryNumber, ExcelReaderUtil.getCellValue(cellMap.get("AD")))
				.with(ExcelDO::setCountryName, ExcelReaderUtil.getCellValue(cellMap.get("AE")))
				.build();
		return excelDO;
	}
}
