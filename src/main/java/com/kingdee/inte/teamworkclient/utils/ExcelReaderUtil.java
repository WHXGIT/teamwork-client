package com.kingdee.inte.teamworkclient.utils;

import com.kingdee.inte.teamworkclient.pojo.ExcelDO;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * description: Excel 读取
 *
 * @author Administrator
 * @date 2020/9/23 14:13
 */
public class ExcelReaderUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelReaderUtil.class);
	private static final List<ExcelDO> excelDOList = new ArrayList<>();
	private static List<Integer> numberList = null;

	public static Map<String, Sheet> getSheetMap(Workbook workbook) {
		Map<String, Sheet> sheepMap = new LinkedHashMap<>();
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			Sheet sheet = workbook.getSheetAt(i);
			sheepMap.put(sheet.getSheetName(), sheet);
		}
		return sheepMap;
	}

	public static List<Row> getRowList(Sheet sheet, int headRowSize) {
		List<Row> rowList = new ArrayList<>();
		for (int i = headRowSize; i < sheet.getPhysicalNumberOfRows(); i++) {
			Row row = sheet.getRow(i);
			rowList.add(row);
		}
		return rowList;
	}

	public static Map<String, Cell> getCellMap(Row row) {
		Map<String, Cell> cellMap = new LinkedHashMap<>();
		for (int i = 0; i < row.getLastCellNum(); i++) {
			cellMap.put(Columns.getIndexLabel(i), row.getCell(i));
		}
		return cellMap;
	}

	public static String getCellValue(Cell cell) {
		String ret = "";
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				ret = cell.getStringCellValue();
				break;
			case FORMULA:
				try {
					ret = String.valueOf(cell.getNumericCellValue());
				} catch (IllegalStateException e) {
					if (e.getMessage().indexOf("from a STRING cell") != -1) {
						ret = String.valueOf(cell.getStringCellValue());
					} else if (e.getMessage().indexOf("from a ERROR formula cell") != -1) {
						ret = String.valueOf(cell.getErrorCellValue());
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
				break;

			case NUMERIC:
				// 处理日期格式、时间格式
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					SimpleDateFormat sdf = null;
					if (cell.getCellStyle().getDataFormat() == HSSFDataFormat
							.getBuiltinFormat("h:mm")) {
						sdf = new SimpleDateFormat("HH:mm");
					} else {// 日期
						sdf = new SimpleDateFormat("yyyy-MM-dd");
					}
					Date date = cell.getDateCellValue();
					ret = sdf.format(date);
				} else if (cell.getCellStyle().getDataFormat() == 58) {
					// 处理自定义日期格式：m月d日(通过判断单元格的格式id解决，id的值是58)
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					double value = cell.getNumericCellValue();
					Date date = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(value);
					ret = sdf.format(date);
				} else {
					ret = NumberToTextConverter.toText(cell.getNumericCellValue());
				}
				break;
			case BLANK:
				ret = "";
				break;
			case BOOLEAN:
				ret = String.valueOf(cell.getBooleanCellValue());
				break;
			case ERROR:
				ret = null;
				break;
			default:
				ret = null;
				break;
		}
		//有必要自行trim
		return ret;
	}


	/**
	 * Method Description: Created by whx
	 * 〈读取某一sheet 页的所有Cell〉
	 *
	 * @param srcPath     源目录
	 * @param sheetIndex  sheet 页下标
	 * @param headRowSize sheet 页表头大小
	 * @return java.util.List<java.util.List < org.apache.poi.ss.usermodel.Cell>>
	 * @date 2020/9/23 15:13
	 */
	public static List<Map<String, Cell>> readXSheetAllCell(String srcPath, int sheetIndex, int headRowSize) {
		List<Map<String, Cell>> allCellList = new ArrayList<>();
		Workbook workbook = null;
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(srcPath));) {
			// 直接传入输入流即可，此时excel就已经解析了
			workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheetAt(sheetIndex);
			List<Row> rowList = getRowList(sheet, headRowSize);
			rowList.forEach(row -> {
				Map<String, Cell> cellMap = getCellMap(row);
				allCellList.add(cellMap);
			});
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
		return allCellList;
	}


}

