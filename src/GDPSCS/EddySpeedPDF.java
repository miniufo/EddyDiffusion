//
package GDPSCS;

import java.io.FileWriter;
import java.util.List;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.statistics.FilterModel;
import miniufo.util.Region2D;


//
public class EddySpeedPDF{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final int binThreshold=20;
	
	private static final int smooth=9;	// odd number only
	
	private static final String path="d:/Data/GDP/SCS/EddySpeedPDF/";
	
	private static final String[] dfiles={
		path+"GDP/buoydata_1_5001.dat",
		path+"GDP/buoydata_5001_10000.dat",
		path+"GDP/buoydata_10001_dec12.dat"
	};
	
	private static final String[] mfiles={
		path+"GDP/dirfl_1_5000.dat",
		path+"GDP/dirfl_5001_10000.dat",
		path+"GDP/dirfl_10001_dec12.dat"
	};
	
	private static final DataDescriptor template=DiagnosisFactory.DF1.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		List<? extends Particle> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,SCS);
		postProcess(ls);
	}
	
	static void postProcess(List<? extends Particle> ls){
		smoothData(ls);
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,false);
		
		estat.normalizeByBins();
		estat.maskoutByBinObservation(binThreshold);
		
		int len=LagrangianUtil.cDefinedCount(ls);
		
		System.out.println("total data size (6hr): "+len);
		
		float[] dataU=new float[len];
		float[] dataV=new float[len];
		float[] posiX=new float[len];
		float[] posiY=new float[len];
		
		int pos=0;
		for(Particle p:ls){
			//int count=p.getTCount();
			
			float[] uvel=p.getZonalVelocity();
			float[] vvel=p.getMeridionalVelocity();
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
			
			//System.arraycopy(uvel,0,dataU,pos,count);
			//System.arraycopy(vvel,0,dataV,pos,count);
			//System.arraycopy(lons,0,posiX,pos,count);
			//System.arraycopy(lats,0,posiY,pos,count);
			
			//pos+=count;
		}
		
		dataToFile(path,"1",DataBaseUtil.binningCount(template,ls).getData()[0][0],dataU,dataV,posiX,posiY);
	}
	
	static void smoothData(List<? extends Particle> ls){
		for(Particle p:ls)
		if(p.getTCount()>=smooth){
			float[] uvelOrig=p.getZonalVelocity();
			float[] vvelOrig=p.getMeridionalVelocity();
			float[] uvelSmth=new float[uvelOrig.length];
			float[] vvelSmth=new float[vvelOrig.length];
			
			FilterModel.runningMean(uvelOrig,uvelSmth,smooth);
			FilterModel.runningMean(vvelOrig,vvelSmth,smooth);
			
			for(int l=0,L=p.getTCount();l<L;l++){
				Record r=p.getRecord(l);
				r.setData(0,uvelSmth[l]);
				r.setData(1,vvelSmth[l]);
			}
		}
	}
	
	static void dataToFile(String fname,String resolution,float[][] count,
	float[] dataU,float[] dataV,float[] posiX,float[] posiY){
		StringBuilder sbU=new StringBuilder();
		StringBuilder sbV=new StringBuilder();
		StringBuilder sbP=new StringBuilder();
		
		for(int i=0,I=dataU.length;i<I;i++){
			int tagX=template.getXNum(posiX[i]);
			int tagY=template.getYNum(posiY[i]);
			
			if(count[tagY][tagX]>=binThreshold){
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
