//
package GDPIO;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.basic.ArrayUtil;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;


//
public final class CycleEllipses2{
	// Indian Ocean region
	private static final String path="/lustre/home/qianyk/Data/GDP/";
	
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(28);
		
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllC.dat"));
		
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,true);
		
		Variable[][] amp_pha=estat.cCycleAmplitudesAndPhases(new float[]{1,2},4f/365f);
		Variable[][] ellipse=estat.cCycleEllipses(new float[]{1,2},4f/365f);
		
		Variable[] vs=ArrayUtil.concatAll(Variable.class,
			ArrayUtil.concatAll(Variable.class,ellipse),
			ArrayUtil.concatAll(Variable.class,amp_pha)
		);
		
		Variable mask=DiagnosisFactory.getVariables(path+"ValidBin/ValidBins3.ctl","","valid")[0];
		estat.writeDataForMatlab(path+"Cycles/CycleEllipse.txt",mask.getData()[0][0],vs);
		
		for(Variable v:vs) System.out.println(v.getName()+"\t"+v.getCommentAndUnit());
	}
}
