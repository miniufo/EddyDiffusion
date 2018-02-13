//
package scenarios;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.basic.ArrayUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.CtlDataWriteStream;
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
import miniufo.util.Region2D;


public final class NoMeanFlow{
	// domain parameters
	static final int tnum=730;			// t-grids
	static final int xnum=301;			// x-grids
	static final int ynum=101;			// x-grids
	
	static final float olon=170;		// center longitude
	static final float olat=0;			// center latitude
	static final float resolution=0.2f;	// resultion of model grid
	static final float deltaT=86400f;	// delta T in DataDescriptor
	
	// deploy parameters
	static final float interv=0.5f;		// intervals for deploying particles
	
	// stochastic parameters
	static final int order=2;			// order of the model
	static final int dtRatio=2;			// ratio of deltaT/dt
	
	static final boolean constMean=false;
	
	static final float[][] Diff=new float[][]{
		{10000,    0},	// m^2/s
		{   0, 10000}	// m^2/s
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
	
	static final Function<Record,StochasticParams> mapping=r->{
		int orderOfModel=order;
		
		if(orderOfModel==0) return new StochasticParams(deltaT/dtRatio,Diff);
		else if(orderOfModel==1) return new StochasticParams(Tvel,Diff);
		else if(orderOfModel==2) return new StochasticParams(Tvel,Tacc,Diff,new float[][]{{0,0},{0,0}});
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
	};
	
	// general parameters
	static final boolean writeTraj=true;
	
	static final String path="/lustre/home/qianyk/Data/Idealized/Scenarios/NoMeanFlow/";
	
	static final String EulerCTL=
		"dset ^ctl.dat\n"+
		"title template\n"+
		"undef -9999\n"+
		"xdef 31 linear 140 2\n"+
		"ydef 11 linear -10 2\n"+
		"zdef  1 linear   1 1\n"+
		"tdef  1 linear 1Jan2001 1dy\n"+
		"vars 1\n"+
		"u 0 99 u\n"+
		"endvars\n";
	
	
	public static void main(String[] args){
		//constructMeanFlow();
		generateLagrangianData();
		//cEulerianStatistics();
		//cLagrangianStatistics(new Region(178f,-2,182,2,"CTR"),180);
	}
	
	static void constructMeanFlow(){
		Variable u=new Variable("u",false,new Range(tnum,1,ynum,xnum));
		Variable v=new Variable("v",false,new Range(tnum,1,ynum,xnum));
		
		u.setCommentAndUnit("zonal mean flow");		u.setUndef(-9999);
		v.setCommentAndUnit("meridional mean flow");	v.setUndef(-9999);
		
		float[][][] udata=u.getData()[0];
		float[][][] vdata=v.getData()[0];
		
		for(int l=0;l<tnum;l++)
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			udata[j][i][l]=0;
			vdata[j][i][l]=0;
		}
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"MeanFlow.dat");
		cdws.writeData(u,v);	cdws.closeFile();
	}
	
	static void generateLagrangianData(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"MeanFlow.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D wholeregion=dd.toRegion2D();
		
		StochasticModel sm=null; int orderOfModel=order;
		if(orderOfModel==0) sm=new LSM0th(dtRatio,constMean,dd,BCx,BCy,mapping);
		else if(orderOfModel==1) sm=new LSM1st(dtRatio,constMean,dd,BCx,BCy,mapping);
		else if(orderOfModel==2) sm=new LSM2nd(dtRatio,constMean,dd,BCx,BCy,mapping);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
		
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(new Region2D(170,0,170,0),0.5f,500,tnum);
		
		System.out.println("\nrelease "+ps.size()+" particles\n");
		
		sm.simulateParticles(ps,"u","v",tnum);
		
		System.out.println("finished");
		
		if(orderOfModel==0) for(Particle p:ps) p.cVelocityByPosition();
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ps,path+"TXT/",wholeregion);
		
		//DiffusionModel.writeParticleList(path+"LD"+order+".dat",ps);
	}
	
	static void cEulerianStatistics(){
		System.out.println("start reading Lagrangian data...\n");
		
		List<Particle> ps=DiffusionModel.readParticleList(path+"LD"+order+".dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		/**************** Eulerian statistics ****************/
		System.out.println("Eulerian Statistics...");
		
		EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
		
		Variable[] count=new Variable[]{new BinningStatistics(dd).binningCount(ps)};
		Variable[] mean=estat.cMeansOfBins();
		Variable[] arrb=estat.cConcentrationAndArrayBias(Diff);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+order+".dat");
		dw.writeData(dd,ArrayUtil.concatAll(Variable.class,count,mean,arrb));
		dw.closeFile();
		
		estat.removeMeansOfBins();
		
		DiffusionModel.writeParticleList(path+"LDRes"+order+".dat",ps);
	}
	
	static void cLagrangianStatistics(Region2D region,int tRad){
		List<Particle> ps=DiffusionModel.readParticleList(path+"LDRes"+order+".dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		/**************** Lagrangian statistics ****************/
		System.out.println("\nLagrangian Statistics...");
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ps,dd);
		
		//long str=ps.get(0).getRecord(0).getTime();
		
		//Predicate<Record>   truetrack=r->{return region.inRange(r.getLon(),r.getLat())&&r.getTime()==str;};
		Predicate<Record> pseudotrack=r->{return region.inRange(r.getXPos(),r.getYPos());};
		
		lstat.cStatisticsByDavisTheory     (pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"1_o"+order+"pseudo.txt");
		lstat.cStatisticsByTaylorTheory    (pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"2_o"+order+"pseudo.txt");
		lstat.cStatisticsByDispersionTheory(pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"3_o"+order+"pseudo.txt");
	}
}
