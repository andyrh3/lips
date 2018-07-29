package Helpers;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;

import java.util.Iterator;

public class ExcelHelper {

	public ExcelHelper() {
		super();
	}
	
	public static void autoSizeColumns(Workbook workbook) {
	    int numberOfSheets = workbook.getNumberOfSheets();
	    for (int i = 0; i < numberOfSheets; i++) {
	        Sheet sheet = workbook.getSheetAt(i);
	        if (sheet.getPhysicalNumberOfRows() > 0) {
	            Row row = sheet.getRow(0);
	            Iterator<Cell> cellIterator = row.cellIterator();
	            while (cellIterator.hasNext()) {
	                Cell cell = cellIterator.next();
	                int columnIndex = cell.getColumnIndex();
	                sheet.autoSizeColumn(columnIndex);
	            }
	        }
	    }
	}
	
	public static void mergeCellsAndAlignCenter(Workbook wb, Cell startCell, Cell endCell){
	    //finding reference of start and end cell; will result like $A$1
	    CellReference startCellRef= new CellReference(startCell.getRowIndex(),startCell.getColumnIndex());
	    CellReference endCellRef = new CellReference(endCell.getRowIndex(),endCell.getColumnIndex());
	    // forming string of references; will result like $A$1:$B$5 
	    String cellReference = startCellRef.formatAsString()+":"+endCellRef.formatAsString();
	    //removing $ to make cellReference like A1:B5
	    cellReference = cellReference.replace("$","");
	    //passing cellReference to make a region
	    CellRangeAddress region = CellRangeAddress.valueOf(cellReference);
	    //use region to merge; though other method like sheet.addMergedRegion(new CellRangeAddress(1,1,4,1));
	    // is also available, but facing some problem right now.
	    startCell.getRow().getSheet().addMergedRegion( region );
	    //setting alignment to center
	    CellUtil.setAlignment(startCell, wb, CellStyle.ALIGN_CENTER);
	}

	public static String getCellValueAsString(Cell cell){
		String cellValue = "";
		if(cell!=null){
			switch (cell.getCellType()) {		
				case Cell.CELL_TYPE_STRING:
				    cellValue = cell.getStringCellValue().trim();
				    break;
		
				case Cell.CELL_TYPE_FORMULA:
				    cellValue = cell.getCellFormula();
				    break;
		
				case Cell.CELL_TYPE_NUMERIC:
				    if (DateUtil.isCellDateFormatted(cell)) {
				        cellValue = cell.getDateCellValue().toString();
				    } else {
				        cellValue = Double.toString(cell.getNumericCellValue());
				    }
				    break;
		
				case Cell.CELL_TYPE_BLANK:
				    cellValue = "";
				    break;
		
				case Cell.CELL_TYPE_BOOLEAN:
				    cellValue = Boolean.toString(cell.getBooleanCellValue());
				    break;
			}
		}
		return cellValue;
	}
}
