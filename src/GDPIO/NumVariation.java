//
package GDPIO;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.CtlDataWriteStream;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;


//
public class NumVariation{
	// Indian Ocean region
	private static final String path="/lustre/home/qianyk/Data/";
	
	
	/** test*/
	public static void main(String[] args){
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunC.dat"));
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		// time-variant DataDescriptor
		DataDescriptor dd=DiagnosisFactory.getDataDescriptor(path+"NumVariation.ctl");
		
		Variable count=new BinningStatistics(dd).binningCount(ls);
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"NumVariation.dat");
		cdws.writeData(count);	cdws.closeFile();
	}
}
