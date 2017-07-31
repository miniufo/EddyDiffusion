//
package GDPSCS;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.application.statisticsModel.LagrangianStatResult;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.GDPDrifter;
import miniufo.util.Region2D;


//
public class CoeffDavis{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final Region2D[] regions=new Region2D[]{
		new Region2D(121.5f, 19.0f, 124.0f, 21.5f),
		new Region2D(117.0f, 21.0f, 120.0f, 23.0f),
		new Region2D(117.0f, 18.0f, 120.0f, 20.5f),
		new Region2D(113.0f, 17.5f, 116.0f, 20.5f),
		new Region2D(109.0f, 16.0f, 112.5f, 19.0f),
		new Region2D(108.5f, 11.0f, 111.0f, 15.5f),
		new Region2D(105.0f,  6.5f, 109.5f, 10.5f)
	};
	
	private static final String path="/lustre/home/qianyk/Data/GDP/SCS/DavisTheory/";
	
	private static final String[] dfiles={
		"/lustre/home/qianyk/Data/GDP/buoydata_1_5001.dat",
		"/lustre/home/qianyk/Data/GDP/buoydata_5001_10000.dat",
		"/lustre/home/qianyk/Data/GDP/buoydata_10001_jun13.dat"
	};
	
	private static final String[] mfiles={
		"/lustre/home/qianyk/Data/GDP/dirfl_1_5000.dat",
		"/lustre/home/qianyk/Data/GDP/dirfl_5001_10000.dat",
		"/lustre/home/qianyk/Data/GDP/dirfl_10001_jun13.dat"
	};
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(1);
		List<GDPDrifter> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,SCS);
		postProcess(ls);
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls){
		DataDescriptor dd=DiagnosisFactory.DFHalf.getDataDescriptor();
		
		EulerianStatistics estat=new EulerianStatistics(ls,dd,false);
		
		estat.removeCyclesByGM(new float[]{1,2},1f/365f,2);
		//estat.removeMeansOfBins();
		
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ls,dd);
		
		LagrangianStatResult r1=lstat.cStatisticsByDavisTheory1(r->regions[0].inRange(r.getLon(),r.getLat()), 60);
		LagrangianStatResult r2=lstat.cStatisticsByDavisTheory1(r->regions[1].inRange(r.getLon(),r.getLat()), 60);
		LagrangianStatResult r3=lstat.cStatisticsByDavisTheory1(r->regions[2].inRange(r.getLon(),r.getLat()), 60);
		LagrangianStatResult r4=lstat.cStatisticsByDavisTheory1(r->regions[3].inRange(r.getLon(),r.getLat()), 60);
		LagrangianStatResult r5=lstat.cStatisticsByDavisTheory1(r->regions[4].inRange(r.getLon(),r.getLat()), 60);
		LagrangianStatResult r6=lstat.cStatisticsByDavisTheory1(r->regions[5].inRange(r.getLon(),r.getLat()), 60);
		LagrangianStatResult r7=lstat.cStatisticsByDavisTheory1(r->regions[6].inRange(r.getLon(),r.getLat()), 60);
		
		r1.toFile(path+"Region1.txt");
		r2.toFile(path+"Region2.txt");
		r3.toFile(path+"Region3.txt");
		r4.toFile(path+"Region4.txt");
		r5.toFile(path+"Region5.txt");
		r6.toFile(path+"Region6.txt");
		r7.toFile(path+"Region7.txt");
	}
}
