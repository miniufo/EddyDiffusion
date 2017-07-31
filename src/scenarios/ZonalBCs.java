//
package scenarios;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.basic.ArrayUtil;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SpatialModel;
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
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;


public final class ZonalBCs{
	// domain parameters
	static final int tnum=730;			// t-grids
	static final int xnum=201;			// x-grids
	static final int ynum=151;			// x-grids
	
	static final float olon=180;		// center longitude
	static final float olat=0;			// center latitude
	static final float resolution=0.2f;	// resultion of model grid
	static final float deltaT=86400f;	// delta T in DataDescriptor
	
	// deploy parameters
	static final float interv=0.5f;		// intervals for deploying particles
	
	// stochastic parameters
	static final int order=0;			// order of the model
	static final int dtRatio=2;			// ratio of deltaT/dt
	
	static final float[][] Diff=new float[][]{
		{2000,  400},	// m^2/s
		{-200, 1000}	// m^2/s
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
		else if(orderOfModel==2) return new StochasticParams(Tvel,Tacc,Diff);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
	};
	
	// general parameters
	static final boolean writeTraj=false;
	
	static final String path="/lustre/home/qianyk/Data/Idealized/Scenarios/ZonalBCs/";
	
	static final String EulerCTL=
		"dset ^ctl.dat\n"+
		"title template\n"+
		"undef -9999\n"+
		"xdef 41 linear 160 1\n"+
		"ydef 31 linear -15 1\n"+
		"zdef  1 linear   1 1\n"+
		"tdef  1 linear 1Jan2001 1dy\n"+
		"vars 1\n"+
		"u 0 99 u\n"+
		"endvars\n";
	
	
	public static void main(String[] args){
		//constructMeanFlow();
		//generateLagrangianData();
		//cEulerianStatistics();
		cLagrangianStatistics(new Region2D(178f,-2,182,2,"CTR"),180);
		cLagrangianStatistics(new Region2D(178f, 2,182,5,"NTH"),180);
	}
	
	static void constructMeanFlow(){
		Variable u =new Variable("u" ,new Range(tnum,1,ynum,xnum));
		Variable v =new Variable("v" ,new Range(tnum,1,ynum,xnum));
		Variable s =new Variable("s" ,new Range(tnum,1,ynum,xnum));
		
		u.setCommentAndUnit("zonal mean flow");		u.setUndef(-9999);
		v.setCommentAndUnit("meridional mean flow");	v.setUndef(-9999);
		s.setCommentAndUnit("stream function");		s.setUndef(-9999);
		
		float[][][] udata=u.getData()[0];
		float[][][] vdata=v.getData()[0];
		float[][][] sdata=s.getData()[0];
		
		double A=1e6,C=Math.toRadians(0.2)*SpatialModel.EARTH_RADIUS;
		
		for(int l=0;l<tnum;l++)
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			double coeff=Math.cos(Math.toRadians(-15+j*0.2));
			sdata[j][i][l]=(float)(-A*sin(PI*j*2/(ynum-1))*sin(PI*i/(xnum-1)));
			udata[j][i][l]=(float)( A*2*PI/(ynum-1)*cos(PI*j*2/(ynum-1))*sin(PI*i/(xnum-1))/C);
			vdata[j][i][l]=(float)(-A*PI/(xnum-1)*sin(PI*j*2/(ynum-1))*cos(PI*i/(xnum-1))/(C*coeff));
		}
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"MeanFlow.dat");
		cdws.writeData(u,v,s);	cdws.closeFile();
	}
	
	static void generateLagrangianData(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"MeanFlow.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D wholeregion=dd.toRegion2D();
		
		StochasticModel sm=null; int orderOfModel=order;
		if(orderOfModel==0) sm=new LSM0th(dtRatio,false,dd,BCx,BCy,mapping);
		else if(orderOfModel==1) sm=new LSM1st(dtRatio,false,dd,BCx,BCy,mapping);
		else if(orderOfModel==2) sm=new LSM2nd(dtRatio,false,dd,BCx,BCy,mapping);
		else throw new IllegalArgumentException("invalid order: "+orderOfModel);
		
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(new Region2D(178f,-2,182,5),0.05f,2,2);
		//List<Particle> ps=new ArrayList<>();
		/*
		Particle p1=sm.deployAt(""+(1000001),olon,-14,tnum); if(p1!=null) ps.add(p1);
		Particle p2=sm.deployAt(""+(1000002),olon,-12,tnum); if(p2!=null) ps.add(p2);
		Particle p3=sm.deployAt(""+(1000003),olon,-10,tnum); if(p3!=null) ps.add(p3);
		Particle p4=sm.deployAt(""+(1000004),olon, -8,tnum); if(p4!=null) ps.add(p4);
		Particle p5=sm.deployAt(""+(1000005),olon, -6,tnum); if(p5!=null) ps.add(p5);
		Particle p6=sm.deployAt(""+(1000006),olon, -4,tnum); if(p6!=null) ps.add(p6);
		Particle p7=sm.deployAt(""+(1000007),olon, -2,tnum); if(p7!=null) ps.add(p7);
		Particle p8=sm.deployAt(""+(1000008),olon,  0,tnum); if(p8!=null) ps.add(p8);
		Particle p9=sm.deployAt(""+(1000009),olon,  2,tnum); if(p9!=null) ps.add(p9);
		Particle p10=sm.deployAt(""+(1000010),olon,  4,tnum); if(p10!=null) ps.add(p10);
		Particle p11=sm.deployAt(""+(1000011),olon,  6,tnum); if(p11!=null) ps.add(p11);
		Particle p12=sm.deployAt(""+(1000012),olon,  8,tnum); if(p12!=null) ps.add(p12);
		Particle p13=sm.deployAt(""+(1000013),olon, 10,tnum); if(p13!=null) ps.add(p13);
		Particle p14=sm.deployAt(""+(1000014),olon, 12,tnum); if(p14!=null) ps.add(p14);
		Particle p15=sm.deployAt(""+(1000015),olon, 14,tnum); if(p15!=null) ps.add(p15);*/
		
		System.out.println("\nrelease "+ps.size()+" particles\n");
		
		sm.simulateParticles(ps,"u","v",tnum);
		
		System.out.println("finished");
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ps,path+"TXT/",wholeregion);
		
		DiffusionModel.writeParticleList(path+"LD"+order+".dat",ps);
	}
	
	static void cEulerianStatistics(){
		System.out.println("start reading Lagrangian data...\n");
		
		List<Particle> ps=DiffusionModel.readParticleList(path+"LD"+order+".dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		/**************** Eulerian statistics ****************/
		System.out.println("Eulerian Statistics...");
		
		EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
		
		Variable[] count=new Variable[]{DataBaseUtil.binningCount(dd,ps)};
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
		
		long str=ps.get(0).getRecord(0).getTime();
		
		//Predicate<Record>   truetrack=r->{return region.inRange(r.getLon(),r.getLat())&&r.getTime()==str;};
		Predicate<Record> pseudotrack=r->{return region.inRange(r.getLon(),r.getLat())&&r.getTime()==str;};
		
		lstat.cStatisticsByDavisTheory1(pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"1_o"+order+"pseudo.txt");
		lstat.cStatisticsByDavisTheory2(pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"2_o"+order+"pseudo.txt");
		lstat.cStatisticsByDavisTheory3(pseudotrack,tRad).toFile(path+"Diff/"+region.getName()+"3_o"+order+"pseudo.txt");
	}
}
