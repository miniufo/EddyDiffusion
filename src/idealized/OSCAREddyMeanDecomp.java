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
import miniufo.lagrangian.Record;
import miniufo.util.GridDataFetcher;


public final class OSCAREddyMeanDecomp{
	//
	private static final float TL=4f;
	
	private static final String path="/lustre/home/qianyk/Data/OSCAR/Synthetic/";
	
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(10);
		
		for(Method m:Method.values()) eddyMeanFlowDecomposition(m);
		
		ConcurrentUtil.shutdown();
	}
	
	static void eddyMeanFlowDecomposition(Method m){
		List<Particle> all=DiffusionModel.readParticleList(path+"LDnew.dat");
		System.out.println("finish reading");
		
		/**************** Eulerian statistics ****************/
		System.out.println("\neddy-mean flow decomposing...");
		
		switch(m){
		case Bin:{
			System.out.println("\n using binning ("+template.getDXDef()[0]+"-deg) method...");
			
			EulerianStatistics estat=new EulerianStatistics(all,template,false);
			Variable[] count=new Variable[]{new BinningStatistics(template).binningCount(all)};
			Variable[] emean=estat.cMeansOfBins();
			Variable[] sbias=estat.cSeasonalSamplingBias();
			Variable[] abias=estat.cConcentrationAndArrayBias(new float[][]{{20000,0},{0,10000}});
			
			DataWrite dw=DataIOFactory.getDataWrite(template,path+"estat"+m+".dat");
			dw.writeData(template,ArrayUtil.concatAll(Variable.class,count,emean,sbias,abias));
			dw.closeFile();
			
			estat.removeMeansOfBins();
			DiffusionModel.writeParticleList(path+m+".dat",all);
			break;
		}
		case Season2:{
			System.out.println("\n using seasonal method with "+DiffusionModel.season2.length+" seasons...");
			
			EulerianStatistics estat=new EulerianStatistics(all,template,false);
			Variable[] count=new Variable[]{new BinningStatistics(template).binningCount(all)};
			Variable[] emean=estat.cMeansOfBins();
			Variable[] sbias=estat.cSeasonalSamplingBias();
			Variable[] abias=estat.cConcentrationAndArrayBias(new float[][]{{20000,0},{0,10000}});
			Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season2,GDPDrifter.UVEL,GDPDrifter.VVEL);
			
			DataWrite dw=DataIOFactory.getDataWrite(template,path+"estat"+m+".dat");
			dw.writeData(template,
				ArrayUtil.concatAll(Variable.class,count,emean,sbias,abias,ArrayUtil.concatAll(Variable.class,ssnl))
			);	dw.closeFile();
			
			estat.removeSeasonalBinMean(DiffusionModel.season2);
			DiffusionModel.writeParticleList(path+m+".dat",all);
			break;
		}
		case Season4:{
			System.out.println("\n using seasonal method with "+DiffusionModel.season4.length+" seasons...");
			
			EulerianStatistics estat=new EulerianStatistics(all,template,false);
			Variable[] count=new Variable[]{new BinningStatistics(template).binningCount(all)};
			Variable[] emean=estat.cMeansOfBins();
			Variable[] sbias=estat.cSeasonalSamplingBias();
			Variable[] abias=estat.cConcentrationAndArrayBias(new float[][]{{20000,0},{0,10000}});
			Variable[][] ssnl=estat.cSeasonalMeans(DiffusionModel.season4,GDPDrifter.UVEL,GDPDrifter.VVEL);
			
			DataWrite dw=DataIOFactory.getDataWrite(template,path+"estat"+m+".dat");
			dw.writeData(template,
				ArrayUtil.concatAll(Variable.class,count,emean,sbias,abias,ArrayUtil.concatAll(Variable.class,ssnl))
			);	dw.closeFile();
			
			estat.removeSeasonalBinMean(DiffusionModel.season4);
			DiffusionModel.writeParticleList(path+m+".dat",all);
			break;
		}
		case GM:{
			System.out.println("\n using GM method with TL="+TL+" days...");
			
			EulerianStatistics estat=new EulerianStatistics(all,template,false);
			Variable[] count=new Variable[]{new BinningStatistics(template).binningCount(all)};
			Variable[] emean=estat.cMeansOfBins();
			Variable[] sbias=estat.cSeasonalSamplingBias();
			Variable[] abias=estat.cConcentrationAndArrayBias(new float[][]{{20000,0},{0,10000}});
			Variable[][] ampli=estat.cCycleAmplitudesAndPhases(new float[]{1,2},TL/365f);
			
			DataWrite dw=DataIOFactory.getDataWrite(template,path+"estat"+m+".dat");
			dw.writeData(template,
				ArrayUtil.concatAll(Variable.class,count,emean,sbias,abias,ArrayUtil.concatAll(Variable.class,ampli))
			);	dw.closeFile();
			
			estat.removeCyclesByGM(new float[]{1,2},TL/365f,2);
			DiffusionModel.writeParticleList(path+m+".dat",all);
			break;
		}
		case GM2:{
			System.out.println("\n using GM2 method with TL="+TL+" days...");
			
			EulerianStatistics estat=new EulerianStatistics(all,template,true);
			Variable[] count=new Variable[]{new BinningStatistics(template).binningCount(all)};
			Variable[] emean=estat.cMeansOfBins();
			Variable[] sbias=estat.cSeasonalSamplingBias();
			Variable[] abias=estat.cConcentrationAndArrayBias(new float[][]{{20000,0},{0,10000}});
			Variable[][] ampli=estat.cCycleAmplitudesAndPhases(new float[]{1,2},TL/365f);
			
			DataWrite dw=DataIOFactory.getDataWrite(template,path+"estat"+m+".dat");
			dw.writeData(template,
				ArrayUtil.concatAll(Variable.class,count,emean,sbias,abias,ArrayUtil.concatAll(Variable.class,ampli))
			);	dw.closeFile();
			
			estat.removeCyclesByGM(new float[]{1,2},TL/365f);
			DiffusionModel.writeParticleList(path+m+".dat",all);
			break;
		}
		case GM3:{
			System.out.println("\n using GM3 method with TL="+TL+" days...");
			
			EulerianStatistics estat=new EulerianStatistics(all,template,true);
			Variable[] count=new Variable[]{new BinningStatistics(template).binningCount(all)};
			Variable[] emean=estat.cMeansOfBins();
			Variable[] sbias=estat.cSeasonalSamplingBias();
			Variable[] abias=estat.cConcentrationAndArrayBias(new float[][]{{20000,0},{0,10000}});
			Variable[][] ampli=estat.cCycleAmplitudesAndPhases(new float[]{1,2,3},TL/365f);
			
			DataWrite dw=DataIOFactory.getDataWrite(template,path+"estat"+m+".dat");
			dw.writeData(template,
				ArrayUtil.concatAll(Variable.class,count,emean,sbias,abias,ArrayUtil.concatAll(Variable.class,ampli))
			);	dw.closeFile();
			
			estat.removeCyclesByGM(new float[]{1,2,3},TL/365f);
			DiffusionModel.writeParticleList(path+m+".dat",all);
			break;
		}
		case True:{
			System.out.print("removing recontructed \"true\" mean");
			
			DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IOMeanCycleRec20062010.ctl");
			DataDescriptor dd=df.getDataDescriptor();
			
			GridDataFetcher gdsU=new GridDataFetcher(dd);
			GridDataFetcher gdsV=new GridDataFetcher(dd);
			
			for(int l=0,L=dd.getTCount();l<L;l++){
				long time=dd.getTDef().getSamples()[l].getLongTime();
				
				Variable bufU=gdsU.prepareXYBuffer("urec",l+1,1);
				Variable bufV=gdsV.prepareXYBuffer("vrec",l+1,1);
				
				for(Particle dftr:all)
				for(int ll=0,LL=dftr.getTCount();ll<LL;ll++){
					Record r=dftr.getRecord(ll);
					
					if(r.getTime()==time){
						float lon=r.getXPos();
						float lat=r.getYPos();
						
						float urec=gdsU.fetchXYBuffer(lon,lat,bufU);
						float vrec=gdsV.fetchXYBuffer(lon,lat,bufV);
						
						float ucurr=r.getDataValue(GDPDrifter.UVEL);	// u current
						float vcurr=r.getDataValue(GDPDrifter.VVEL);	// v current
						
						if(Math.abs(ucurr-urec)>5000) System.out.println("um undef for lon:"+lon+", lat:"+lat);
						if(Math.abs(vcurr-vrec)>5000) System.out.println("vm undef for lon:"+lon+", lat:"+lat);
						
						r.setData(GDPDrifter.UVEL,ucurr-urec);
						r.setData(GDPDrifter.VVEL,vcurr-vrec);
					}
				}
				
				if(l%(365*4)==0) System.out.print(".");
			}
			
			System.out.println("\n");
			DiffusionModel.writeParticleList(path+m+".dat",all);
			break;
		}
		default: throw new IllegalArgumentException("not supported method: "+m);
		}
		
		System.out.println("finish decomposing");
	}
}
