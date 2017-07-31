//
package diffuse;

import miniufo.application.statisticsModel.StatisticsBasicAnalysisMethods;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


//
public final class ADTVariability{
	//
	public static void main(String[] args){
		DiagnosisFactory df=DiagnosisFactory.parseFile("D:/Data/Aviso/madt/h/madt_scs.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable adt=df.getVariables(new Range("",dd),"adt")[0];
		
		Variable var=StatisticsBasicAnalysisMethods.cTVariance(adt);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,"d:/Data/Aviso/madt/madtvar.dat");
		dw.writeData(dd,var);	dw.closeFile();
	}
}
