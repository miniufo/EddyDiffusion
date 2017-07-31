//
package scenarios;

import java.util.List;
import java.util.function.Function;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.LSM0th;
import miniufo.lagrangian.LSM1st;
import miniufo.lagrangian.LSM2nd;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.lagrangian.StochasticModel;
import miniufo.lagrangian.StochasticParams;
import miniufo.lagrangian.StochasticModel.BCType;
import miniufo.util.GridDataFetcher;
import miniufo.util.Region2D;


public final class WellMixing{
	// domain parameters
	static final int tnum=365;			// t-grids
	static final int xnum=201;			// x-grids
	static final int ynum=151;			// x-grids
	
	static final float olon=180;		// center longitude
	static final float olat=0;			// center latitude
	static final float resolution=0.2f;	// resultion of model grid
	static final float deltaT=86400f;	// delta T in DataDescriptor
	
	// deploy parameters
	static final float interv=0.5f;		// intervals for deploying particles
	
	// stochastic parameters
	static final int order=2;			// order of the model
	static final int dtRatio=1;			// ratio of deltaT/dt
	
	static final float[][] Diff=new float[][]{
		{1000,    0},	// m^2/s
		{   0, 1000}	// m^2/s
	};
	
	static final float[][] Tvel=new float[][]{
		{10,  1},	// days
		{ 1, 10}	// days
	};
	
	static final float[][] Tacc=new float[][]{
		{5, 1},	// days
		{1, 5}	// days
	};
	
	static final BCType BCx =BCType.Reflected;	// zonal BC
	static final BCType BCy =BCType.Reflected;	// meridional BC
	
	static final String path="/lustre/home/qianyk/Data/Idealized/Scenarios/WellMixing/";
	
	static final GridDataFetcher gdf=new GridDataFetcher(DiagnosisFactory.parseFile(path+"DiffGrd.ctl").getDataDescriptor());
	
	static final float[][] kxxBuf =gdf.prepareXYBuffer("kxx" ,1,1);
	static final float[][] kxxxBuf=gdf.prepareXYBuffer("kxxx",1,1);
	static final float[][] kyyBuf =gdf.prepareXYBuffer("kyy" ,1,1);
	static final float[][] kyyyBuf=gdf.prepareXYBuffer("kyyy",1,1);
	
	static final Function<Record,StochasticParams> mapping=r->{
		int orderOfModel=order;
		
		float lon=r.getLon();
		float lat=r.getLat();
		
		float kxx =gdf.fetchXYBuffer(lon,lat,kxxBuf );
		float kxxx=gdf.fetchXYBuffer(lon,lat,kxxxBuf);
		float kyy =gdf.fetchXYBuffer(lon,lat,kyyBuf );
		float kyyy=gdf.fetchXYBuffer(lon,lat,kyyyBuf);
		
		float[][] diff=new float[][]{{kxx ,0},{0, kyy}};
		float[][] dGrd=new float[][]{{kxxx,0},{0,kyyy}};
		
		if(orderOfModel==0) return new StochasticParams(deltaT/dtRatio,diff,dGrd);
		else if(orderOfModel==1) return new StochasticParams(Tvel,diff,dGrd);
		else if(orderOfModel==2) return new StochasticParams(Tvel,Tacc,diff,dGrd);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
	};
	
	static final String EulerCTL=
		"dset ^ctl.dat\n"+
		"title template\n"+
		"undef -9999\n"+
		"xdef 61 linear 140 1\n"+
		"ydef 21 linear -10 1\n"+
		"zdef  1 linear   1 1\n"+
		"tdef  1 linear 1Jan2001 1dy\n"+
		"vars 1\n"+
		"u 0 99 u\n"+
		"endvars\n";
	
	
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(6);
		
		//constructMeanFlow();
		generateLagrangianData();
		
		ConcurrentUtil.shutdown();
	}
	
	static void generateLagrangianData(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"MeanFlow.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D wholeregion=dd.toRegion2D();
		
		StochasticModel sm=null; int orderOfModel=order;
		if(orderOfModel==0) sm=new LSM0th(dtRatio,true,dd,BCx,BCy,mapping);
		else if(orderOfModel==1) sm=new LSM1st(dtRatio,true,dd,BCx,BCy,mapping);
		else if(orderOfModel==2) sm=new LSM2nd(dtRatio,true,dd,BCx,BCy,mapping);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
		
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(wholeregion,0.1f,1,2);
		
		System.out.println("\nrelease "+ps.size()+" particles\n");
		
		int years=10;
		
		Variable[] concs=new Variable[years];
		concs[0]=cEulerianStatistics(ps);
		concs[0].setName("conc0");
		concs[0].setCommentAndUnit("concentration at 0 years");
		
		for(int i=1;i<years;i++){
			System.out.println(" tracking for "+i+" years");
			sm.simulateParticles(ps,"u","v",tnum,false,6);
			concs[i]=cEulerianStatistics(ps);
			concs[i].setName("conc"+i);
			concs[i].setCommentAndUnit("concentration at "+i+" years");
		}
		
		System.out.println("finished");
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+order+".dat");
		dw.writeData(DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor(),concs); dw.closeFile();
	}
	
	static Variable cEulerianStatistics(List<Particle> ps){
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		return new EulerianStatistics(ps,dd,false).cConcentrationAndArrayBias(Diff)[0];
	}
}
