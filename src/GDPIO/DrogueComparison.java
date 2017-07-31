package GDPIO;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.basic.ArrayUtil;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;


//
public final class DrogueComparison{
	// Indian Ocean
	private static final String path="/lustre/home/qianyk/Data/GDP/";
	
	private static final DataDescriptor dd=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(10);
		
		List<GDPDrifter> lsdrg=DiffusionModel.readDrifterList(path+"IO2013JunDrog.dat");
		List<GDPDrifter> lsudg=DiffusionModel.readDrifterList(path+"IO2013JunUndr.dat");
		
		EulerianStatistics estat=new EulerianStatistics(lsdrg,dd,true);
		Variable[] v=new Variable[]{DataBaseUtil.binningCount(dd,lsdrg)};
		Variable[] mean=estat.cMeansOfBins();
		Variable[] bias=estat.cSeasonalSamplingBias();
		Variable[] amps=ArrayUtil.concatAll(Variable.class,estat.cCycleAmplitudesAndPhases(new float[]{1,2},4f/365f));
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"DrogCompare/drogued.dat");
		dw.writeData(dd,ArrayUtil.concatAll(Variable.class,v,mean,bias,amps));
		
		EulerianStatistics estat2=new EulerianStatistics(lsudg,dd,true);
		Variable[] v2=new Variable[]{DataBaseUtil.binningCount(dd,lsudg)};
		Variable[] mean2=estat2.cMeansOfBins();
		Variable[] bias2=estat2.cSeasonalSamplingBias();
		Variable[] amps2=ArrayUtil.concatAll(Variable.class,estat.cCycleAmplitudesAndPhases(new float[]{1,2},4f/365f));
		
		DataWrite dw2=DataIOFactory.getDataWrite(dd,path+"DrogCompare/undrogued.dat");
		dw2.writeData(dd,ArrayUtil.concatAll(Variable.class,v2,mean2,bias2,amps2));
		
		ConcurrentUtil.shutdown();
	}
}
