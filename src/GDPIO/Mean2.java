//
package GDPIO;

import java.util.ArrayList;
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
import miniufo.lagrangian.Particle;
import static miniufo.basic.ArrayUtil.concatAll;


//
public final class Mean2{
	// Indian Ocean region
	private static final int[][] seasons=new int[][]{{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12}};
	
	private static final String path="/lustre/home/qianyk/Data/GDP/IO/";
	
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(10);
		
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunUndrC.dat"));
		
		ConcurrentUtil.shutdown();
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,true);
		
		Variable[] current=estat.cMeansOfBins();
		Variable[] stdcurr=estat.cSTDsOfBins();
		Variable[] seasBias=estat.cSeasonalSamplingBias();
		Variable[] ellipse=estat.cVarianceEllipse();
		Variable[] v=new Variable[]{DataBaseUtil.binningCount(template,ls)};
		Variable[] seasonal=concatAll(Variable.class,DataBaseUtil.binningSeasonalData(template,ls,true,seasons,0,1));
		Variable[] seaCount=DataBaseUtil.binningSeasonalCount(template,ls,seasons);
		Variable[][] amps =estat.cCycleAmplitudesAndPhases(new float[]{1,2},4f/365f);
		
		DataWrite dw=DataIOFactory.getDataWrite(template,path+"Mean/recordsUndrC.dat");
		dw.writeData(template,concatAll(Variable.class,
			current,stdcurr,seasonal,seaCount,seasBias,ellipse,v,concatAll(Variable.class,amps))
		);	dw.closeFile();
		
		
		List<GDPDrifter> lsdrg=new ArrayList<>();
		List<GDPDrifter> lsudg=new ArrayList<>();
		
		for(GDPDrifter dr:ls){
			GDPDrifter[] split=dr.splitByDrogueOffDate(3);
			
			if(split[0]!=null) lsdrg.add(split[0]);
			if(split[1]!=null) lsudg.add(split[1]);
		}
		
		{	// output trajectory length bins
			int[] sep=new int[]{4,8,12,16,20,28,40,60,80,120,200,280,400,600,800,1000,Integer.MAX_VALUE};
			int[] count=getRecLengthStatistics(ls,sep);
			
			System.out.println("\noutput trajectory length bins:");
			for(int i=0;i<count.length;i++)
			System.out.println("<"+sep[i]/4+"\t"+count[i]);
		}
		
		/********************** for all drifter **********************/
		{	// output annual frequency
			int[][] count=getAnnualFrequency(ls);
			
			System.out.println("\noutput annual frequency (all):");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
		
		{	// output month frequency
			int[][] count=getMonthFrequency(ls);
			
			System.out.println("\noutput month frequency (all):");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
		
		/********************** for drogued drifter **********************/
		{	// output annual frequency
			int[][] count=getAnnualFrequency(lsdrg);
			
			System.out.println("\noutput annual frequency (drogued):");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
		
		{	// output month frequency
			int[][] count=getMonthFrequency(lsdrg);
			
			System.out.println("\noutput month frequency (drogued):");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
		
		/********************** for undrogued drifter **********************/
		{	// output annual frequency
			int[][] count=getAnnualFrequency(lsudg);
			
			System.out.println("\noutput annual frequency (undrogued):");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
		
		{	// output month frequency
			int[][] count=getMonthFrequency(lsudg);
			
			System.out.println("\noutput month frequency (undrogued):");
			for(int i=0;i<count[0].length;i++)
			System.out.println(count[0][i]+":\t"+count[1][i]);
		}
	}
	
	
	/*** helper methods ***/
	private static int[] getRecLengthStatistics(List<? extends Particle> ls,int[] separators){
		int len=separators.length;
		
		int[] count=new int[len];
		
		for(Particle l:ls){
			int size=l.getTCount();
			
			for(int i=0;i<len;i++)
			if(size<separators[i]){ count[i]++; break;}
		}
		
		return count;
	}
	
	private static int[][] getAnnualFrequency(List<? extends Particle> ls){
		int str=1978;
		int end=2013;
		int len=end-str+1;
		
		int[] year=new int[len];
		int[] freq=new int[len];
		
		for(int l=str;l<=end;l++) year[l-str]=l;
		
		for(Particle l:ls){
			long[] time=l.getTimes();
			
			for(long t:time){
				int yr=Integer.parseInt(String.valueOf(t).substring(0,4));
				
				for(int y=str;y<=end;y++)
				if(y==yr){ freq[y-str]++; break;}
			}
		}
		
		return new int[][]{year,freq};
	}
	
	private static int[][] getMonthFrequency(List<? extends Particle> ls){
		int str=1;
		int end=12;
		int len=end-str+1;
		
		int[] mnth=new int[len];
		int[] freq=new int[len];
		
		for(int l=str;l<=end;l++) mnth[l-str]=l;
		
		for(Particle l:ls){
			long[] time=l.getTimes();
			
			for(long t:time){
				int month=Integer.parseInt(String.valueOf(t).substring(4,6));
				
				for(int mo=str;mo<=end;mo++)
				if(mo==month){ freq[mo-str]++; break;}
			}
		}
		
		return new int[][]{mnth,freq};
	}
}
