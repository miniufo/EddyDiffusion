//
package scenarios;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.CtlDataWriteStream;
import miniufo.lagrangian.LSM0th;
import miniufo.lagrangian.LSM1st;
import miniufo.lagrangian.LSM2nd;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.lagrangian.StochasticModel;
import miniufo.lagrangian.StochasticParams;
import miniufo.lagrangian.StochasticModel.BCType;
import miniufo.util.Region2D;


public final class Shearing{
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
	static final int order=0;			// order of the model
	static final int dtRatio=1;			// ratio of deltaT/dt
	
	static final boolean constMean=true;
	
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
	static final boolean writeTraj=false;
	
	static final String path="/lustre/home/qianyk/Data/Idealized/Scenarios/Shearing/";
	
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
		//constructMeanFlow();System.exit(0);
		//generateLagrangianData();
		List<Particle> ps=generateLagrangianData();
		cEulerianStatistics(ps);
		cLagrangianStatistics(new Region2D(169.0f,-0.2f,171f,0.2f,"CTR"),180,ps);
		
		ConcurrentUtil.shutdown();
	}
	
	static void constructMeanFlow(){
		Variable u=new Variable("u",true,new Range(1,1,ynum,xnum));
		Variable v=new Variable("v",true,new Range(1,1,ynum,xnum));
		
		u.setCommentAndUnit("zonal mean flow");		u.setUndef(-9999);
		v.setCommentAndUnit("meridional mean flow");	v.setUndef(-9999);
		
		float[][] udata=u.getData()[0][0];
		float[][] vdata=v.getData()[0][0];
		
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			// shearOscillateMean
			double tmp=-0.05*(-10+j*resolution);
			
			double sing=Math.sin(0);
			double cosg=Math.cos(0);
			
			udata[j][i]=(float)(cosg*tmp);
			vdata[j][i]=(float)(sing*tmp);
		}
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"MeanFlow.dat");
		cdws.writeData(u,v);	cdws.closeFile();
	}
	
	static List<Particle> generateLagrangianData(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"MeanFlow.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D wholeregion=dd.toRegion2D();
		
		StochasticModel sm=null; int orderOfModel=order;
		if(orderOfModel==0) sm=new LSM0th(dtRatio,constMean,dd,BCx,BCy,mapping);
		else if(orderOfModel==1) sm=new LSM1st(dtRatio,constMean,dd,BCx,BCy,mapping);
		else if(orderOfModel==2) sm=new LSM2nd(dtRatio,constMean,dd,BCx,BCy,mapping);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
		
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(wholeregion,0.5f,2,tnum);
		
		System.out.println("\nrelease "+ps.size()+" particles\n");
		
		sm.simulateParticles(ps,"u","v",tnum);
		
		System.out.println("finished");
		
		for(Particle p:ps) p.cVelocityByPosition();
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ps,path+"TXT/",wholeregion);
		
		//DiffusionModel.writeParticleList(path+"LD"+order+".dat",ps);
		
		return ps;
	}
	
	static void cEulerianStatistics(List<Particle> ps){
		System.out.println("start reading Lagrangian data...\n");
		
		//List<Particle> ps=DiffusionModel.readParticleList(path+"LD"+order+".dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		/**************** Eulerian statistics ****************/
		System.out.println("Eulerian Statistics...");
		
		EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
		
		//Variable[] count=new Variable[]{DataBaseUtil.binningCount(dd,ps)};
		//Variable[] mean=estat.cMeansOfBins();
		//Variable[] arrb=estat.cConcentrationAndArrayBias(Diff);
		
		//DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+order+".dat");
		//dw.writeData(dd,ArrayUtil.concatAll(Variable.class,count,mean,arrb));
		//dw.closeFile();
		
		estat.removeMeansOfBins();
		
		//DiffusionModel.writeParticleList(path+"LDRes"+order+".dat",ps);
	}
	
	static void cLagrangianStatistics(Region2D region,int tRad,List<Particle> ps){
		//List<Particle> ps=DiffusionModel.readParticleList(path+"LDRes"+order+".dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		/**************** Lagrangian statistics ****************/
		System.out.println("\nLagrangian Statistics...");
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ps,dd);
		
		//long str=ps.get(0).getRecord(0).getTime();
		
		//Predicate<Record>   truetrack=r->{return region.inRange(r.getLon(),r.getLat())&&r.getTime()==str;};
		Predicate<Record> pseudotrack=r->{return region.inRange(r.getXPos(),r.getYPos());};
		
		lstat.cStatisticsByDavisTheory1(pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"1_o"+order+"pseudo.txt");
		lstat.cStatisticsByDavisTheory2(pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"2_o"+order+"pseudo.txt");
		lstat.cStatisticsByDavisTheory3(pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"3_o"+order+"pseudo.txt");
	}
}
