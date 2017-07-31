//
package idealized;

import java.util.List;
import java.util.function.Predicate;
import diffuse.DiffusionModel;
import diffuse.DiffusionModel.Method;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.basic.ArrayUtil;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.util.Region2D;


public final class RealDiffusivity{
	//
	private static boolean cEulerianStatistics=true;
	
	// Indian Ocean region
	static final Region2D IO=new Region2D(29,-41,116,26);
	
	static final float TL=4;
	
	static final Region2D[] regions=new Region2D[]{
		new Region2D(60, -3,85,  3,"EQ"),
		new Region2D(46,  2,57, 16,"WB"),
		new Region2D(60,-35,95,-25,"SIO")
	};
	
	static final String path="/lustre/home/qianyk/Data/";
	
	static final String[] dfiles={
		path+"GDP/buoydata_1_5001.dat",
		path+"GDP/buoydata_5001_10000.dat",
		path+"GDP/buoydata_10001_jun13.dat"
	};
	
	static final String[] mfiles={
		path+"GDP/dirfl_1_5000.dat",
		path+"GDP/dirfl_5001_10000.dat",
		path+"GDP/dirfl_10001_jun13.dat"
	};
	
	static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		//extractDrogueDrifters();
		//for(Method m:Method.values()) if(m!=Method.True) eddyMeanDecomposition(m);
		//System.exit(0);
		
		for(Method m:Method.values())
		if(m!=Method.True){
			cLagrangianStatistics(regions[0],240,regions[0].getName(),m);
			cLagrangianStatistics(regions[1],240,regions[1].getName(),m);
			cLagrangianStatistics(regions[2],240,regions[2].getName(),m);
		}
	}
	
	
	static void extractDrogueDrifters(){
		List<GDPDrifter> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,4,IO);
		
		// excluding South China Sea
		DiffusionModel.removeDrifterWithin(ls,new Region2D(100, 0,125,35));
		DiffusionModel.removeDrifterWithin(ls,new Region2D(106,-6,125,0 ));
		DiffusionModel.removeDrifterWithin(ls,new Region2D(15 ,17,40 ,35));
		
		List<GDPDrifter> drogued=DiffusionModel.getDroguedDrifters(ls);
		
		// correct wind slip
		DiffusionModel.correctWindSlip(drogued,
			DiagnosisFactory.getDataDescriptor(path+"NCEP/uvIO.ctl"),
		"uwnd","vwnd");
		
		System.out.println("finish correcting wind slippage\n");
		
		// write to file
		DiffusionModel.writeDrifterList(path+"Idealized/Real/IO2013JunDroguedC.dat",drogued);
	}
	
	static void eddyMeanDecomposition(Method m){
		ConcurrentUtil.initDefaultExecutor(20);
		
		List<GDPDrifter> drogued=DiffusionModel.readDrifterList(path+"Idealized/Real/IO2013JunDroguedC.dat");
		
		/**************** Eulerian statistics ****************/
		System.out.println("\nEulerian Statistics...");
		
		switch(m){
		case Bin:{
			System.out.println(" using binning ("+template.getDXDef()[0]+"-deg) method...");
			
			EulerianStatistics estat=new EulerianStatistics(drogued,template,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{DataBaseUtil.binningCount(template,drogued)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,0,1);
				Variable[] bias=estat.cSeasonalSamplingBias();
				
				DataWrite dw=DataIOFactory.getDataWrite(template,path+"Estat"+m+".dat");
				dw.writeData(template,ArrayUtil.concatAll(Variable.class,
					count,mean,bias,ArrayUtil.concatAll(Variable.class,ssnl)
				));
				dw.closeFile();
			}
			
			estat.removeMeansOfBins();
			
			DiffusionModel.writeDrifterList(path+"Idealized/Real/IO2013JunDroguedC"+m+".dat",drogued);
			
			break;
		}
			
		case Season2:{
			System.out.println(" using seasonal method with 2 seasons...");
			
			EulerianStatistics estat=new EulerianStatistics(drogued,template,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{DataBaseUtil.binningCount(template,drogued)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,0,1);
				Variable[] bias=estat.cSeasonalSamplingBias();
				
				DataWrite dw=DataIOFactory.getDataWrite(template,path+"Estat"+m+".dat");
				dw.writeData(template,ArrayUtil.concatAll(Variable.class,
					count,mean,bias,ArrayUtil.concatAll(Variable.class,ssnl)
				));
				dw.closeFile();
			}
			
			estat.removeSeasonalBinMean(DiffusionModel.season2);
			
			DiffusionModel.writeDrifterList(path+"Idealized/Real/IO2013JunDroguedC"+m+".dat",drogued);
			
			break;
		}
		
		case Season4:{
			System.out.println(" using seasonal method with 4 seasons...");
			
			EulerianStatistics estat=new EulerianStatistics(drogued,template,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{DataBaseUtil.binningCount(template,drogued)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season4,0,1);
				Variable[] bias=estat.cSeasonalSamplingBias();
				
				DataWrite dw=DataIOFactory.getDataWrite(template,path+"Estat"+m+".dat");
				dw.writeData(template,ArrayUtil.concatAll(Variable.class,
					count,mean,bias,ArrayUtil.concatAll(Variable.class,ssnl)
				));
				dw.closeFile();
			}
			
			estat.removeSeasonalBinMean(DiffusionModel.season4);
			
			DiffusionModel.writeDrifterList(path+"Idealized/Real/IO2013JunDroguedC"+m+".dat",drogued);
			
			break;
		}
			
		case GM:{
			System.out.println(" using GM method with TL="+TL+" days...");
			
			EulerianStatistics estat=new EulerianStatistics(drogued,template,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{DataBaseUtil.binningCount(template,drogued)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,0,1);
				Variable[] bias=estat.cSeasonalSamplingBias();
				Variable[][] ampli=estat.cCycleAmplitudesAndPhases(new float[]{1,2},TL/365f);
				
				DataWrite dw=DataIOFactory.getDataWrite(template,path+"Estat"+m+".dat");
				dw.writeData(template,ArrayUtil.concatAll(Variable.class,count,mean,bias,
					ArrayUtil.concatAll(Variable.class,ssnl),ArrayUtil.concatAll(Variable.class,ampli)
				));
				dw.closeFile();
			}
			
			estat.removeCyclesByGM(new float[]{1,2},TL/365f,2);
			
			DiffusionModel.writeDrifterList(path+"Idealized/Real/IO2013JunDroguedC"+m+".dat",drogued);
			
			break;
		}
			
		case GM2:{
			System.out.println(" using GM2 method with TL="+TL+" days...");
			
			EulerianStatistics estat=new EulerianStatistics(drogued,template,true);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{DataBaseUtil.binningCount(template,drogued)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,0,1);
				Variable[] bias=estat.cSeasonalSamplingBias();
				Variable[][] ampli=estat.cCycleAmplitudesAndPhases(new float[]{1,2},TL/365f);
				
				DataWrite dw=DataIOFactory.getDataWrite(template,path+"Estat"+m+".dat");
				dw.writeData(template,ArrayUtil.concatAll(Variable.class,count,mean,bias,
					ArrayUtil.concatAll(Variable.class,ssnl),ArrayUtil.concatAll(Variable.class,ampli)
				));
				dw.closeFile();
			}
			
			estat.removeCyclesByGM(new float[]{1,2},TL/365f,2);
			
			DiffusionModel.writeDrifterList(path+"Idealized/Real/IO2013JunDroguedC"+m+".dat",drogued);
			
			break;
		}
			
		default:
			throw new IllegalArgumentException("unsupported method: "+m);
		}
		
		System.out.println("finish decomposition");
	}
	
	static void cLagrangianStatistics(Region2D region,int tRad,String tag,Method m){
		List<Particle> ps=DiffusionModel.readParticleList(path+"Idealized/Real/IO2013JunDroguedC"+m+".dat");
		
		/**************** Lagrangian statistics ****************/
		System.out.println("Lagrangian Statistics...");
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ps,template);
		
		Predicate<Record> cond=r->region.inRange(r.getLon(),r.getLat());
		
		lstat.cStatisticsByDavisTheory1(cond,tRad).toFile(path+"Idealized/Real/Lstat"+tag+m+"1.txt");
		lstat.cStatisticsByDavisTheory2(cond,tRad).toFile(path+"Idealized/Real/Lstat"+tag+m+"2.txt");
	}
}
