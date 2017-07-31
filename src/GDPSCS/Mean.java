//
package GDPSCS;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.util.Region2D;
import static miniufo.basic.ArrayUtil.concatAll;


//
public class Mean{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final int mask=15;
	
	private static final boolean writeTraj=false;
	
	private static final int[][] seasons=new int[][]{{3,4,5},{6,7,8},{9,10,11},{12,1,2}};
	
	private static final String path="/lustre/home/qianyk/Data/GDP/SCS/Mean/";
	
	private static final String[] dfiles={
		"/lustre/home/qianyk/Data/GDP/buoydata_1_5001.dat",
		"/lustre/home/qianyk/Data/GDP/buoydata_5001_10000.dat",
		"/lustre/home/qianyk/Data/GDP/buoydata_10001_jun13.dat"
	};
	
	private static final String[] mfiles={
		"/lustre/home/qianyk/Data/GDP/dirfl_1_5000.dat",
		"/lustre/home/qianyk/Data/GDP/dirfl_5001_10000.dat",
		"/lustre/home/qianyk/Data/GDP/dirfl_10001_jun13.dat"
	};
	
	private static final DataDescriptor template=DiagnosisFactory.DFHalf.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(8);
		
		List<GDPDrifter> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,SCS);
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ls,path,SCS);
		
		postProcess(ls);
		
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("This subset spans "+LagrangianUtil.cTotalDrifterYear(ls)+" drifter-years");
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,true);
		
		Variable[] current=estat.cMeansOfBins();
		Variable[] stdcurr=estat.cSTDsOfBins();
		Variable[] seasBias=estat.cSeasonalSamplingBias();
		Variable[][] amps =estat.cCycleAmplitudesAndPhases(new float[]{1,2},1f/365f);
		Variable[] ellipse=estat.cVarianceEllipse();
		Variable v=DataBaseUtil.binningCount(template,ls);
		Variable[] seasonal=concatAll(Variable.class,DataBaseUtil.binningSeasonalData(template,ls,true,seasons,0,1));
		Variable[] seaCount=DataBaseUtil.binningSeasonalCount(template,ls,seasons);
		
		estat.removeMeansOfBins();
		Variable EKE=estat.cEKE();
		
		writeData2D(template,EKE.getData()[0][0],v.getData()[0][0],path+"EKEST.txt");
		writeData2D(template,current[0].getData()[0][0],v.getData()[0][0],path+"umeanST.txt");
		writeData2D(template,current[1].getData()[0][0],v.getData()[0][0],path+"vmeanST.txt");
		
		DataWrite dw=DataIOFactory.getDataWrite(template,path+"recordsST.dat");
		dw.writeData(template,concatAll(Variable.class,concatAll(Variable.class,
			current,stdcurr,seasonal,seaCount,seasBias,ellipse,concatAll(Variable.class,amps)),v,EKE
		));	dw.closeFile();
		
		estat.writeDataForMatlab(path+"ellipseST.txt",mask,concatAll(Variable.class,current,ellipse));
		
		{	// output trajectory length bins
			int[] sep=new int[]{4,8,12,16,20,28,40,60,80,120,200,280,400,600,800,1000,Integer.MAX_VALUE};
			int[] count=getRecLengthStatistics(ls,sep);
			
			System.out.println("\noutput trajectory length bins:");
			for(int i=0;i<count.length;i++)
			System.out.println("<"+sep[i]/4+"\t"+count[i]);
		}
		
		{	// output annual frequency
			int[][] count=getAnnualFrequency(ls);
			
			System.out.println("\noutput annual frequency:");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
		
		{	// output month frequency
			int[][] count=getMonthFrequency(ls);
			
			System.out.println("\noutput month frequency:");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
	}
	
	
	/*** helper methods ***/
	private static int[] getRecLengthStatistics(List<GDPDrifter> ls,int[] separators){
		int len=separators.length;
		
		int[] count=new int[len];
		
		for(GDPDrifter l:ls){
			int size=l.getTCount();
			
			for(int i=0;i<len;i++)
			if(size<separators[i]){ count[i]++; break;}
		}
		
		return count;
	}
	
	private static int[][] getAnnualFrequency(List<GDPDrifter> ls){
		int str=1978;
		int end=2012;
		int len=end-str+1;
		
		int[] year=new int[len];
		int[] freq=new int[len];
		
		for(int l=str;l<=end;l++) year[l-str]=l;
		
		for(GDPDrifter l:ls){
			long[] time=l.getTimes();
			
			for(long t:time){
				int yr=Integer.parseInt(String.valueOf(t).substring(0,4));
				
				for(int y=str;y<=end;y++)
				if(y==yr){ freq[y-str]++; break;}
			}
		}
		
		return new int[][]{year,freq};
	}
	
	private static int[][] getMonthFrequency(List<GDPDrifter> ls){
		int str=1;
		int end=12;
		int len=end-str+1;
		
		int[] mnth=new int[len];
		int[] freq=new int[len];
		
		for(int l=str;l<=end;l++) mnth[l-str]=l;
		
		for(GDPDrifter l:ls){
			long[] time=l.getTimes();
			
			for(long t:time){
				int month=Integer.parseInt(String.valueOf(t).substring(4,6));
				
				for(int mo=str;mo<=end;mo++)
				if(mo==month){ freq[mo-str]++; break;}
			}
		}
		
		return new int[][]{mnth,freq};
	}
	
	private static void writeData2D(DataDescriptor dd,float[][] data,float[][] count,String path){
		try{
			BufferedWriter br=new BufferedWriter(new FileWriter(path));
			
			float lon1=SCS.getLonMin();
			float lon2=SCS.getLonMax();
			float lat1=SCS.getLatMin();
			float lat2=SCS.getLatMax();
			
			for(int j=dd.getYNum(lat1),I=dd.getXNum(lon2),J=dd.getYNum(lat2);j<=J;j++){
				for(int i=dd.getXNum(lon1);i<I;i++)
				if(count[j][i]>mask) br.write(data[j][i]+"  ");
				else br.write("-9999.0  ");
				
				if(count[j][I]>mask) br.write(data[j][I]+"\n");
				else br.write("-9999.0\n");
			}
			
			br.close();
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
}
