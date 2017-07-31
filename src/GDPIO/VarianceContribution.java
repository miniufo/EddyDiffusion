//
package GDPIO;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import static miniufo.basic.ArrayUtil.concatAll;


//
public final class VarianceContribution{
	// Indian Ocean region
	private static final String path="/lustre/home/qianyk/Data/GDP/";
	
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(24);
		
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllC.dat"));
		
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,true);
		
		Variable[][] vc=estat.cVarianceContribution(new float[]{1,2},4f/365f);
		
		DataWrite dw=DataIOFactory.getDataWrite(template,path+"VarCon/varContrib.dat");
		dw.writeData(template,concatAll(Variable.class,vc));	dw.closeFile();
	}
}
