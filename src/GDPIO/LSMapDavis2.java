//
package GDPIO;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;


//
public final class LSMapDavis2{
	// Indian Ocean region
	private static final String path="/lustre/home/qianyk/Data/GDP/IO/";
	
	private static final DataDescriptor dd=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(24);
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCRes0.dat"),"No0");
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCRes1.dat"),"No1");
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCRes2.dat"),"No2");
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls,String prefix){
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ls,dd);
		
		int tRad=4*30;
		int str =4*10;
		int end =4*15;
		int minT=1000;
		
		float bRad=2f;
		
		Variable[] stats=lstat.cStatisticsMapByDavisTheory3(tRad,bRad,str,end,minT);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Diff/NstatsMap"+prefix+"3.dat");
		dw.writeData(dd,stats);	dw.closeFile();
	}
}
