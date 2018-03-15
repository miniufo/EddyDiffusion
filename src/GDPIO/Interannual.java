//
package GDPIO;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.basic.ArrayUtil;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.database.AccessGDPDrifter;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.MDate;
import miniufo.diagnosis.Variable;
import miniufo.io.CtlDataWriteStream;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.util.Region2D;


//
public class Interannual{
	// Indian Ocean region
	private static final Region2D IO=new Region2D(29,-41,116,26,"IO region");
	
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
	
	// time-invariant DataDescriptor
	private static final DataDescriptor template=DiagnosisFactory.DF1.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(12);
		
		List<GDPDrifter> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,3,IO);
		
		DiffusionModel.removeDrifterWithin(ls,new Region2D(100, 0,125,35));
		DiffusionModel.removeDrifterWithin(ls,new Region2D(106,-6,125,0 ));
		DiffusionModel.removeDrifterWithin(ls,new Region2D(15 ,17,40 ,35));
		
		ls=AccessGDPDrifter.getRecordsWithinRange(ls,new MDate("00z1Jan1993"),new MDate("18z31Dec2012"));
		
		postProcess(ls);
		
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		// time-variant DataDescriptor
		DataDescriptor dd=DiagnosisFactory.getDataDescriptor(path+"Drifter.ctl");
		
		BinningStatistics bs=new BinningStatistics(dd);
		
		Variable[] vels=bs.binningData(ls,GDPDrifter.UVEL,GDPDrifter.VVEL);
		Variable[] temp=bs.binningData(ls,GDPDrifter.Temp);
		Variable count=bs.binningCount(ls);
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,true);
		estat.removeCyclesByGM(new float[]{1,2},4f/365f,2);
		
		Variable[] vels2=bs.binningData(ls,GDPDrifter.UVEL,GDPDrifter.VVEL);
		Variable[] temp2=bs.binningData(ls,GDPDrifter.Temp);
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"Drifter.dat");
		cdws.writeData(ArrayUtil.concatAll(Variable.class,vels,vels2,temp,temp2,new Variable[]{count}));
		cdws.closeFile();
	}
}
