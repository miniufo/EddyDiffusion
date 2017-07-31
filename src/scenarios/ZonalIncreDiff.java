//
package scenarios;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import diffuse.DiffusionModel;
import miniufo.application.basic.DynamicMethodsInSC;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.basic.ArrayUtil;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SphericalSpatialModel;
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

public final class ZonalIncreDiff{
	// domain parameters
	static final int tnum=365*5;		// t-grids
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
	static final int dtRatio=2;			// ratio of deltaT/dt
	
	static final float[][] Tvel=new float[][]{
		{5, 1},	// days
		{1, 5}	// days
	};
	
	static final float[][] Tacc=new float[][]{
		{3, 1},	// days
		{1, 3}	// days
	};
	
	static final BCType BCx =BCType.Reflected;	// zonal BC
	static final BCType BCy =BCType.Reflected;	// meridional BC
	
	static final Function<Record,StochasticParams> mapping=r->{
		int orderOfModel=order;
		
		float lon=r.getLon();
		float rlon=(xnum-1)/2*resolution;
		
		float Diffxx=1000+(50-1)*1000*(lon-(olon-rlon))/(rlon*2f);
		
		float[][] Diff=new float[][]{
			{Diffxx,    0},	// m^2/s
			{     0, 1000}	// m^2/s
		};
		
		if(orderOfModel==0) return new StochasticParams(deltaT/dtRatio,Diff);
		else if(orderOfModel==1) return new StochasticParams(Tvel,Diff);
		else if(orderOfModel==2) return new StochasticParams(Tvel,Tacc,Diff);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
	};
	
	// general parameters
	static final boolean writeTraj=false;
	
	static final String path="/lustre/home/qianyk/Data/Idealized/Scenarios/ZonalIncreDiff/";
	
	static final String EulerCTLAll=
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
	
	static final String EulerCTLT=
			"dset ^ctl.dat\n"+
			"title template\n"+
			"undef -9999\n"+
			"xdef  31 linear 140 2\n"+
			"ydef  11 linear -10 2\n"+
			"zdef   1 linear   1 1\n"+
			"tdef "+tnum+" linear 1Jan2001 1dy\n"+
			"vars 1\n"+
			"u 0 99 u\n"+
			"endvars\n";
	
	
	public static void main(String[] args){
		//constructMeanFlow();
		constructDiffusivity();
		computeDiffGradient();
		//generateLagrangianData();
		//cEulerianStatistics();
		//cLagrangianStatistics(new Region(168f,-2,172,2),180);
	}
	
	static void constructDiffusivity(){
		Variable diffu=new Variable("diffu",new Range(1,1,ynum,xnum));
		Variable diffv=new Variable("diffv",new Range(1,1,ynum,xnum));
		
		diffu.setCommentAndUnit("zonal diffusivity");		diffu.setUndef(-9999);
		diffv.setCommentAndUnit("meridional diffusivity");	diffv.setUndef(-9999);
		
		float[][] udata=diffu.getData()[0][0];
		float[][] vdata=diffv.getData()[0][0];
		
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			udata[j][i]=9100f+9000f*(float)Math.sin((i-150.0)/150.0*Math.PI/2.0);
			vdata[j][i]=4500f+4400f*(float)Math.sin((j- 50.0)/ 50.0*Math.PI/2.0);
		}
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"Diffusivity.dat");
		cdws.writeData(diffu,diffv);	cdws.closeFile();
	}
	
	static void computeDiffGradient(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"Diffusivity.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		SphericalSpatialModel ssm=new SphericalSpatialModel(dd);
		DynamicMethodsInSC dm=new DynamicMethodsInSC(ssm);
		
		Variable[] diff=df.getVariables(new Range("",dd),"kxx","kyy");
		Variable[] dxGrd=dm.c2DGradient(diff[0]); dxGrd[0].setName("kxxx"); dxGrd[1].setName("kxxy");
		Variable[] dyGrd=dm.c2DGradient(diff[1]); dyGrd[0].setName("kyyx"); dyGrd[1].setName("kyyy");
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"DiffGrd.dat");
		cdws.writeData(dd,diff[0],diff[1],dxGrd[0],dxGrd[1],dyGrd[0],dyGrd[1]);
		cdws.closeFile();
	}
	
	static void constructMeanFlow(){
		Variable u=new Variable("u",new Range(tnum,1,ynum,xnum));
		Variable v=new Variable("v",new Range(tnum,1,ynum,xnum));
		
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
		if(orderOfModel==0) sm=new LSM0th(dtRatio,dd,BCx,BCy,mapping);
		else if(orderOfModel==1) sm=new LSM1st(dtRatio,dd,BCx,BCy,mapping);
		else if(orderOfModel==2) sm=new LSM2nd(dtRatio,dd,BCx,BCy,mapping);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
		
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(wholeregion,interv,1,tnum);
		
		System.out.println("\nrelease "+ps.size()+" particles\n");
		
		sm.simulateParticles(ps,"u","v",tnum);
		
		System.out.println("finished");
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ps,path+"TXT/",wholeregion);
		
		DiffusionModel.writeParticleList(path+"LD"+order+".dat",ps);
	}
	
	static void cEulerianStatistics(){
		System.out.println("start reading Lagrangian data...\n");
		
		List<Particle> ps=DiffusionModel.readParticleList(path+"LD"+order+".dat");
		
		DataDescriptor dd =DiagnosisFactory.parseContent(EulerCTLAll).getDataDescriptor();
		DataDescriptor ddT=DiagnosisFactory.parseContent(EulerCTLT  ).getDataDescriptor();
		
		/**************** Eulerian statistics ****************/
		System.out.println("Eulerian Statistics...");
		
		EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
		
		Variable[] count=new Variable[]{DataBaseUtil.binningCount(ddT,ps)};
		Variable[] arrb=estat.cConcentrationAndArrayBias(new float[][]{{20000,0},{0,10000}},ddT);
		
		DataWrite dw=DataIOFactory.getDataWrite(ddT,path+"Estat"+order+".dat");
		dw.writeData(dd,ArrayUtil.concatAll(Variable.class,count,arrb));
		dw.closeFile();
		
		estat.removeMeansOfBins();
		
		DiffusionModel.writeParticleList(path+"LDRes"+order+".dat",ps);
	}
	
	static void cLagrangianStatistics(Region2D region,int tRad){
		List<Particle> ps=DiffusionModel.readParticleList(path+"LD"+order+".dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTLAll).getDataDescriptor();
		
		/**************** Lagrangian statistics ****************/
		System.out.println("\nLagrangian Statistics...");
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ps,dd);
		
		Predicate<Record> cond=r->region.inRange(r.getLon(),r.getLat());
		
		lstat.cStatisticsByDavisTheory1(cond,tRad).toFile(path+"Diff/Lstat1_o"+order+".txt");
		lstat.cStatisticsByDavisTheory2(cond,tRad).toFile(path+"Diff/Lstat2_o"+order+".txt");
		lstat.cStatisticsByDavisTheory3(cond,tRad).toFile(path+"Diff/Lstat3_o"+order+".txt");
	}
}
