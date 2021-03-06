//
package idealized;

import java.util.List;
import diffuse.DiffusionModel;
import diffuse.DiffusionModel.Method;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.basic.ArrayUtil;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.Particle;


public final class IdealizedEddyMeanDecomp{
	//
	private static boolean cEulerianStatistics=true;
	
	private static final int TL=4;	// Lagrangian timescale
	
	private static final String path="/lustre/home/qianyk/Data/Idealized/Mean/";
	
	private static final String EulerCTL=
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
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(24);
		
		for(Method m:Method.values()) if(m!=Method.True){
			if(m==Method.Bin) eddyMeanFlowDecomposition("UniformMean",m);
			if(m!=Method.GM&&m!=Method.GM2) eddyMeanFlowDecomposition("Oscillate1Mean",m);
			if(m!=Method.GM2) eddyMeanFlowDecomposition("Oscillate2Mean",m);
			eddyMeanFlowDecomposition("ShearOscillateMean",m);
		}
		
		ConcurrentUtil.shutdown();
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
	static void eddyMeanFlowDecomposition(String tag,Method m){
		List<Particle> ps=DiffusionModel.readParticleList(path+tag+"LD.dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		/**************** Eulerian statistics ****************/
		System.out.println("\nEulerian Statistics...");
		
		switch(m){
		case Bin:{
			System.out.println(" using binning ("+dd.getDXDef()[0]+"-deg) method...");
			
			EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{new BinningStatistics(dd).binningCount(ps)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,GDPDrifter.UVEL,GDPDrifter.VVEL);
				Variable[] bias=estat.cSeasonalSamplingBias();
				
				DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+tag+m+".dat");
				dw.writeData(dd,ArrayUtil.concatAll(Variable.class,
					count,mean,bias,ArrayUtil.concatAll(Variable.class,ssnl)
				));
				dw.closeFile();
			}
			
			estat.removeMeansOfBins();
			
			DiffusionModel.writeParticleList(path+tag+"LD"+m+".dat",ps);
			
			break;
		}
			
		case Season2:{
			System.out.println(" using seasonal method with 2 seasons...");
			
			EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{new BinningStatistics(dd).binningCount(ps)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,GDPDrifter.UVEL,GDPDrifter.VVEL);
				Variable[] bias=estat.cSeasonalSamplingBias();
				
				DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+tag+m+".dat");
				dw.writeData(dd,ArrayUtil.concatAll(Variable.class,
					count,mean,bias,ArrayUtil.concatAll(Variable.class,ssnl)
				));
				dw.closeFile();
			}
			
			estat.removeSeasonalBinMean(DiffusionModel.season2);
			
			DiffusionModel.writeParticleList(path+tag+"LD"+m+".dat",ps);
			
			break;
		}
		
		case Season4:{
			System.out.println(" using seasonal method with 4 seasons...");
			
			EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{new BinningStatistics(dd).binningCount(ps)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season4,GDPDrifter.UVEL,GDPDrifter.VVEL);
				Variable[] bias=estat.cSeasonalSamplingBias();
				
				DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+tag+m+".dat");
				dw.writeData(dd,ArrayUtil.concatAll(Variable.class,
					count,mean,bias,ArrayUtil.concatAll(Variable.class,ssnl)
				));
				dw.closeFile();
			}
			
			estat.removeSeasonalBinMean(DiffusionModel.season4);
			
			DiffusionModel.writeParticleList(path+tag+"LD"+m+".dat",ps);
			
			break;
		}
			
		case GM:{
			System.out.println(" using GM method with TL="+TL+" days...");
			
			EulerianStatistics estat=new EulerianStatistics(ps,dd,false);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{new BinningStatistics(dd).binningCount(ps)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,GDPDrifter.UVEL,GDPDrifter.VVEL);
				Variable[] bias=estat.cSeasonalSamplingBias();
				Variable[][] ampli=estat.cCycleAmplitudesAndPhases(new float[]{1,2},TL/365f);
				
				DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+tag+m+".dat");
				dw.writeData(dd,ArrayUtil.concatAll(Variable.class,count,mean,bias,
					ArrayUtil.concatAll(Variable.class,ssnl),ArrayUtil.concatAll(Variable.class,ampli)
				));
				dw.closeFile();
			}
			
			estat.removeCyclesByGM(new float[]{1,2},TL/365f,2);
			
			DiffusionModel.writeParticleList(path+tag+"LD"+m+".dat",ps);
			
			break;
		}
			
		case GM2:{
			System.out.println(" using GM2 method with TL="+TL+" days...");
			
			EulerianStatistics estat=new EulerianStatistics(ps,dd,true);
			
			if(cEulerianStatistics){
				Variable[] count=new Variable[]{new BinningStatistics(dd).binningCount(ps)};
				Variable[] mean=estat.cMeansOfBins();
				Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,GDPDrifter.UVEL,GDPDrifter.VVEL);
				Variable[] bias=estat.cSeasonalSamplingBias();
				Variable[][] ampli=estat.cCycleAmplitudesAndPhases(new float[]{1,2},TL/365f);
				
				DataWrite dw=DataIOFactory.getDataWrite(dd,path+"Estat"+tag+m+".dat");
				dw.writeData(dd,ArrayUtil.concatAll(Variable.class,count,mean,bias,
					ArrayUtil.concatAll(Variable.class,ssnl),ArrayUtil.concatAll(Variable.class,ampli)
				));
				dw.closeFile();
			}
			
			estat.removeCyclesByGM(new float[]{1,2},TL/365f,2);
			
			DiffusionModel.writeParticleList(path+tag+"LD"+m+".dat",ps);
			
			break;
		}
			
		default:
			throw new IllegalArgumentException("unsupported method: "+m);
		}
		
		System.out.println("finish decomposition");
	}
}
