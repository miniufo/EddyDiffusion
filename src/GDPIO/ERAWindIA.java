//
package GDPIO;

import miniufo.application.statisticsModel.FilterMethods;
import miniufo.application.statisticsModel.StatisticsBasicAnalysisMethods;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


//
public final class ERAWindIA{
	//
	private static final String path="/lustre/home/qianyk/Data/ERAInterim/";
	
	//
	public static void main(String[] args){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"ERAInterim.uv10.1993-2012.monthly.nc");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable[] uv=df.getVariables(new Range("time(1993.1.1,2012.12.1)",dd),false,"u10","v10");
		
		uv[0].setUndef(-9999);
		uv[1].setUndef(-9999);
		
		Variable mag=uv[0].hypotenuse(uv[1]);	mag.setName("wspd");
		
		FilterMethods.cycleFilter(uv[0],12);
		FilterMethods.cycleFilter(uv[1],12);
		FilterMethods.cycleFilter(mag,12);
		
		Variable urng=StatisticsBasicAnalysisMethods.cTRange(uv[0]);
		Variable vrng=StatisticsBasicAnalysisMethods.cTRange(uv[1]);
		Variable srng=StatisticsBasicAnalysisMethods.cTRange(mag);
		
		Variable ustd=StatisticsBasicAnalysisMethods.cTStandardDeviation(uv[0]);
		Variable vstd=StatisticsBasicAnalysisMethods.cTStandardDeviation(uv[1]);
		Variable sstd=StatisticsBasicAnalysisMethods.cTStandardDeviation(mag);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"ERAWindIA.dat");
		dw.writeData(dd,urng,vrng,srng,ustd,vstd,sstd);	dw.closeFile();
	}
}
