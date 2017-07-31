//
package GDPIO;

import miniufo.application.statisticsModel.StatisticsBasicAnalysisMethods;
import miniufo.basic.ArrayUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


//
public final class ERAWindDecompose{
	//
	private static final String path="/lustre/home/qianyk/Data/ERAInterim/";
	
	//
	public static void main(String[] args){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"ERAInterim.uv10.1993-2012.nc");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable[] uv=df.getVariables(new Range("",dd),false,"u10","v10");
		
		uv[0].setUndef(-9999);
		uv[1].setUndef(-9999);
		
		Variable mag=uv[0].hypotenuse(uv[1]);
		
		Variable[] uamp=StatisticsBasicAnalysisMethods.cHarmonicAmplitudes(uv[0],365,182,91,60);
		Variable[] vamp=StatisticsBasicAnalysisMethods.cHarmonicAmplitudes(uv[1],365,182,91,60);
		Variable[] mamp=StatisticsBasicAnalysisMethods.cHarmonicAmplitudes( mag ,365,182,91,60);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"ERAWindAmplitudes.dat");
		dw.writeData(dd,ArrayUtil.concatAll(Variable.class,uamp,vamp,mamp));	dw.closeFile();
	}
}
