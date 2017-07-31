//
package GDPIO;

import java.util.List;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.util.Region2D;
import static miniufo.basic.ArrayUtil.concatAll;


//
public class MeanComparison{
	// Indian Ocean region
	private static final Region2D IO=new Region2D(18,-43,121,30,"IO region");
	//private static final int mask=15;	// for matlab data only
	
	private static final boolean writeTraj=false;
	
	private static final String path="/lustre/home/qianyk/Data/";
	
	private static final String[] dfiles={
		path+"GDP/buoydata_1_5001.dat",
		path+"GDP/buoydata_5001_10000.dat",
		path+"GDP/buoydata_10001_dec12.dat"
	};
	
	private static final String[] mfiles={
		path+"GDP/dirfl_1_5000.dat",
		path+"GDP/dirfl_5001_10000.dat",
		path+"GDP/dirfl_10001_dec12.dat"
	};
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(20);
		
		List<GDPDrifter> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,IO);
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ls,path,IO);
		
		postProcess(ls);
		
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		DataDescriptor dd=DiagnosisFactory.DF1.getDataDescriptor();
		
		EulerianStatistics estat=new EulerianStatistics(ls,dd,true);
		
		Variable[] current=estat.cMeansOfBins();
		Variable[] stdcurr=estat.cSTDsOfBins();
		Variable[][] amps1=estat.cCycleAmplitudesAndPhases(new float[]{1}, 5f/365f);
		Variable[][] amps2=estat.cCycleAmplitudesAndPhases(new float[]{1,2}, 5f/365f);
		Variable[][] amps3=estat.cCycleAmplitudesAndPhases(new float[]{1,2,3}, 5f/365f);
		Variable[][] amps4=estat.cCycleAmplitudesAndPhases(new float[]{1,2}, 2f/365f);
		Variable[][] amps5=estat.cCycleAmplitudesAndPhases(new float[]{1,2},10f/365f);
		Variable v=DataBaseUtil.binningCount(dd,ls);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"MeanComparison.dat");
		dw.writeData(dd,concatAll(Variable.class,
			current,stdcurr,new Variable[]{v},
			concatAll(Variable.class,amps1),
			concatAll(Variable.class,amps2),
			concatAll(Variable.class,amps3),
			concatAll(Variable.class,amps4),
			concatAll(Variable.class,amps5)
		));
		dw.closeFile();
	}
}
