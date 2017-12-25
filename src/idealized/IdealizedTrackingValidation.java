//
package idealized;

import java.io.FileWriter;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.application.statisticsModel.SingleParticleStatResult;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.basic.ArrayUtil;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.io.CtlDataWriteStream;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.LSM1st;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.lagrangian.StochasticModel;
import miniufo.lagrangian.StochasticModel.BCType;
import miniufo.lagrangian.StochasticParams;
import miniufo.util.Region2D;


public final class IdealizedTrackingValidation{
	// domain parameters
	private static final int   xnum=201;		// domain x-size
	private static final int   ynum=101;		// domain y-size
	private static final float olon=180;		// center longitude
	private static final float olat=0;			// center latitude
	private static final float dist=0.2f;		// grid interval in degree
	
	// mean flow parameters
	private static final double shear=3e-7;		// shear strength
	private static final double gamma=Math.toRadians(45.0);	// shear orientation
	
	// tracking parameters
	private static final int  intLen=1000;		// length of integration
	private static final float kappa=100;		// m^2/s
	private static final float TL   =3;			// days
	
	private static final float[][] diff=new float[][]{{kappa,0},{0,kappa}};
	private static final float[][] Tvel=new float[][]{{TL,1},{1,TL}};
	
	// general parameters
	private static final boolean writeTraj=false;
	private static final String path="/lustre/home/qianyk/Data/Idealized/Shear/";
	private static final String EulerCTL=
		"dset ^ctl.dat\n"+
		"title template\n"+
		"undef -9999\n"+
		"xdef 401 linear 160 0.1\n"+
		"ydef 201 linear -10 0.1\n"+
		"zdef   1 linear   1 1\n"+
		"tdef   1 linear 1Jan2001 6hr\n"+
		"vars 1\n"+
		"u 0 99 u\n"+
		"endvars\n";
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(1);
		
		//constructMeanFlow(1460,path+"meanNoShear.dat"); System.exit(0);
		//expNoShearSinglePoint();
		//expShearSinglePoint();
		expShearMultiPoints();
		
		ConcurrentUtil.shutdown();
	}
	
	static void expNoShearSinglePoint(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"meanNoShear.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D region=new Region2D(olon-30,olat-10,olon+30,olat+10);
		Region2D point=new Region2D(olon,olat,olon,olat);
		
		Function<Record,StochasticParams> f1=r->{ return new StochasticParams(Tvel,diff);};
		
		StochasticModel sm=new LSM1st(2,dd,BCType.Landing,BCType.Landing,f1);
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(point,0.2f,2000,intLen);
		
		sm.simulateParticles(ps,"u","v",intLen);
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ps,path,region);
		
		System.out.println(" contains "+ps.size()+" particles");

		cStatistics(ps,new Region2D(179.9f,-0.1f,180.9f,0.1f),240,"NoShear");
	}
	
	static void expShearSinglePoint(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"meanShear.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D point=new Region2D(olon,olat,olon,olat);
		
		Function<Record,StochasticParams> f1=r->{ return new StochasticParams(Tvel,diff);};
		
		StochasticModel sm=new LSM1st(2,dd,BCType.Landing,BCType.Landing,f1);
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(point,0.2f,2000,intLen);
		
		sm.simulateParticles(ps,"u","v",intLen);
		
		System.out.println(" contains "+ps.size()+" particles");
		
		cStatistics(ps,new Region2D(179.9f,-0.1f,180.9f,0.1f),240,"ShearSP");
	}
	
	static void expShearMultiPoints(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"meanShear45.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D region=new Region2D(olon-20,olat-10,olon+20,olat+10);
		
		Function<Record,StochasticParams> f1=r->{ return new StochasticParams(Tvel,diff);};
		
		StochasticModel sm=new LSM1st(2,dd,BCType.Landing,BCType.Landing,f1);
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		List<Particle> ps=sm.deployPatch(region,0.2f,2000,intLen);
		
		sm.simulateParticles(ps,"u","v",intLen);
		
		System.out.println(" contains "+ps.size()+" particles");
		
		cStatistics(ps,new Region2D(177f,-3,183,3),240,"ShearMP");
	}
	
	
	/**
	 * compute Eulerian and Lagrangian statistics
	 * 
	 * @param	ps		particle list
	 * @param	lon1	start lon for Lagrangian statistics
	 * @param	lat1	start lat for Lagrangian statistics
	 * @param	lon2	end lon for Lagrangian statistics
	 * @param	lat2	end lat for Lagrangian statistics
	 * @param	tRad	maximum time lag
	 * @param	fname	file name stamp
	 */
	static void cStatistics(List<Particle> ps,Region2D region,int tRad,String fname){
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
		Variable[] count=new Variable[]{new BinningStatistics(dd).binningCount(ps)};
		Variable[] mean=estat.cMeansOfBins();
		Variable[] bias=estat.cSeasonalSamplingBias();
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"estat"+fname+".dat");
		dw.writeData(dd,ArrayUtil.concatAll(Variable.class,count,mean,bias));
		dw.closeFile();
		
		estat.removeMeansOfBins();
		
		/**
		//estat.normalizeByDividingSTD();
		int len=estat.getDefinedCount();
		
		System.out.println("total data size (6hr): "+len);
		
		float[] dataU=new float[len];
		float[] dataV=new float[len];
		float[] posiX=new float[len];
		float[] posiY=new float[len];
		
		int pos=0;
		for(Particle p:ps){
			float[] uvel=p.getZonalSpeeds();
			float[] vvel=p.getMeridionalSpeeds();
			float[] lons=p.getLongitudes();
			float[] lats=p.getLatitudes();
			
			for(int l=0,L=uvel.length;l<L;l++)
			if(uvel[l]!=Record.undef){
				dataU[pos]=uvel[l];
				dataV[pos]=vvel[l];
				posiX[pos]=lons[l];
				posiY[pos]=lats[l];
				pos++;
			}
		}
		
		dataToFile(path,"",count[0].getData()[0][0],dataU,dataV,posiX,posiY);*/
		
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ps,dd);
		
		Predicate<Record> cond=r->region.inRange(r.getXPos(),r.getYPos());
		
		SingleParticleStatResult r1=lstat.cStatisticsByDavisTheory1(cond,tRad);
		SingleParticleStatResult r2=lstat.cStatisticsByDavisTheory2(cond,tRad);
		
		r1.toFile(path+"lstat"+fname+"1.txt");
		r2.toFile(path+"lstat"+fname+"2.txt");
	}
	
	/**
	 * construct mean flow using Oh and Zhurbas (2000, JGR) method
	 * 
	 * @param x			grid count in zonal
	 * @param y			grid count in meridional
	 * @param dis		grid spacing (km)
	 * @param Ul		shear (s^-1)
	 * @param gamma		direction from east (degree)
	 * 
	 * @return	re		mean flow, [0] is zonal and [1] is meridional components
	 */
	static void constructMeanFlow(int t,String filename){
		int midx=xnum/2;	double lonstr=-dist*midx;
		int midy=ynum/2;	double latstr=-dist*midy;
		
		Variable u=new Variable("um",new Range(t,1,ynum,xnum));
		Variable v=new Variable("vm",new Range(t,1,ynum,xnum));
		
		u.setCommentAndUnit("zonal mean flow");		u.setUndef(-9999);
		v.setCommentAndUnit("meridional mean flow");	v.setUndef(-9999);
		
		float[][][][] udata=u.getData();
		float[][][][] vdata=v.getData();
		
		for(int l=0;l<t;l++)
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			double lon=Math.toRadians(lonstr+i*dist);
			double lat=Math.toRadians(latstr+j*dist);
			
			double dx=lon*SpatialModel.EARTH_RADIUS*Math.cos(lat);
			double dy=lat*SpatialModel.EARTH_RADIUS;
			
			double sing=Math.sin(gamma);
			double cosg=Math.cos(gamma);
			
			double tmp=shear*(-dx*sing+dy*cosg);
			
			udata[0][j][i][l]=(float)(cosg*tmp);
			vdata[0][j][i][l]=(float)(sing*tmp);
		}
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(filename);
		cdws.writeData(u,v);	cdws.closeFile();
	}
	
	static void dataToFile(String fname,String resolution,float[][] count,
	float[] dataU,float[] dataV,float[] posiX,float[] posiY){
		StringBuilder sbU=new StringBuilder();
		StringBuilder sbV=new StringBuilder();
		StringBuilder sbP=new StringBuilder();
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		for(int i=0,I=dataU.length;i<I;i++){
			int tagX=dd.getXNum(posiX[i]);
			int tagY=dd.getYNum(posiY[i]);
			
			if(count[tagY][tagX]>=30){
				sbU.append(dataU[i]+"\n");
				sbV.append(dataV[i]+"\n");
				sbP.append(posiX[i]+"\t"+posiY[i]+"\n");
			}
		}
		
		try{
			FileWriter fw=null;
			fw=new FileWriter(fname+"dataU"+resolution+".txt");
			fw.write(sbU.toString());	fw.close();
			
			fw=new FileWriter(fname+"dataV"+resolution+".txt");
			fw.write(sbV.toString());	fw.close();
			
			fw=new FileWriter(fname+"posit"+resolution+".txt");
			fw.write(sbP.toString());	fw.close();
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
}
