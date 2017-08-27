//
package GDPIO;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.Record;
import miniufo.mathsphysics.PowerSpectrum;
import miniufo.mathsphysics.WindowFunction;
import miniufo.statistics.StatisticsUtil;


//
public final class RotarySpectra{
	// Indian Ocean region
	private static final boolean variancePreserving=false;
	
	private static final int segmentLength=4*120;
	private static final int offset=4*90;
	
	private static final float Fs=4f;
	private static final float percentage=0.80f;
	
	private static final float[] window=WindowFunction.tukey(segmentLength,0.1f);
	
	private static final String path="/lustre/home/qianyk/Data/GDP/";
	
	
	/** test*/
	public static void main(String[] args){
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCRes2.dat"));
	}
	
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("overlapping "+(segmentLength-offset)*100f/segmentLength+"%");
		
		List<GDPDrifter> segs=segmentalizeAll(ls);
		
		Averager r1=computeRegion(segs,46,7,78,26);
		Averager r2=computeRegion(segs,78,7,100,25);
		Averager r3=computeRegion(segs,38,-7,100,7);
		Averager r4=computeRegion(segs,55,-20,105,-8);
		Averager r5=computeRegion(segs,55,-37,105,-23);
		
		toFile(toString(r1.average()),path+"PS/cpsd1"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r2.average()),path+"PS/cpsd2"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r3.average()),path+"PS/cpsd3"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r4.average()),path+"PS/cpsd4"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r5.average()),path+"PS/cpsd5"+(variancePreserving?"VP":"")+".txt");
	}
	
	static Averager computeRegion(List<GDPDrifter> segs,float lons,float lats,float lone,float late){
		Averager av=new Averager();
		
		int count=0;
		for(GDPDrifter seg:segs)
		if(mostlyWithinRegion(seg,lons,lats,lone,late,percentage)){
			count++;
			
			float[][] spec=getRotarySpectrum(seg);
			
			av.addSample(spec);
		}
		
		System.out.println(
			String.format("Region [%5.1fE -- %5.1fE, %5.1fN -- %5.1fN] has %5d segments",
			lons,lone,lats,late,count
		));
		
		return av;
	}
	
	static float[][] getRotarySpectrum(GDPDrifter dr){
		float[] u=dr.getAttachedData(0);
		float[] v=dr.getAttachedData(1);
		
		float[][] cspd=PowerSpectrum.fftCPSDEstimate(u,v,window,Fs);
		
		if(variancePreserving)
		for(int i=0;i<4;i++)
		for(int j=0,J=cspd[0].length;j<J;j++) cspd[i][j]*=j*Fs/segmentLength;
		
		return cspd;
	}
	
	static List<GDPDrifter> segmentalizeAll(List<GDPDrifter> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		List<GDPDrifter> allSegs=new ArrayList<>();
		
		for(GDPDrifter dr:ls){
			GDPDrifter[] drs=segmentalize(dr,segmentLength,offset);
			
			if(drs!=null)
			for(GDPDrifter drr:drs){
				float[] uvel=drr.getUVel();
				
				boolean hasUndef=false;
				
				for(int l=0;l<drr.getTCount();l++)
				if(uvel[l]==Record.undef){ hasUndef=true; break;}
				
				if(!hasUndef) allSegs.add(drr);
			}
		}
		
		System.out.println("segments spans "+
			LagrangianUtil.cTotalDrifterYear(allSegs)+
		" drifter-years");
		
		return allSegs;
	}
	
	static GDPDrifter[] segmentalize(GDPDrifter dr,int seglen,int offset){
		int len=dr.getTCount();
		
		if(seglen<3) throw new IllegalArgumentException("minimum segment length is 3");
		if(offset<1||seglen<offset) throw new IllegalArgumentException("offset should be in [1 seglen]");
		if(len<seglen) return null;
		
		int segments=(len-seglen)/offset+1;
		
		String[] vars=dr.getDataNames();
		GDPDrifter[] drs=new GDPDrifter[segments];
		
		for(int i=0;i<segments;i++){
			drs[i]=new GDPDrifter(dr.getID()+"_"+i,seglen,vars.length);
			drs[i].setAttachedDataNames(vars);
			
			for(int j=0,ptr=i*offset;j<seglen;j++) drs[i].addRecord(dr.getRecord(ptr++));
		}
		
		return drs;
	}
	
	static boolean mostlyWithinRegion(GDPDrifter dr,float lons,float lats,float lone,float late,float percent){
		int cc=0;
		
		for(int l=0,L=dr.getTCount();l<L;l++){
			float lon=dr.getXPosition(l);
			float lat=dr.getYPosition(l);
			
			if(lon>=lons&&lon<=lone&&lat>=lats&&lat<=late) cc++;
		}
		
		if(cc*1f/dr.getTCount()>=percent) return true;
		else return false;
	}
	
	static StringBuilder toString(float[][][] data){
		StringBuilder sb=new StringBuilder();
		
		int I=data[0][0].length;
		int J=data[0].length;
		
		for(int i=0;i<I;i++){
			for(int j=0;j<J;j++) sb.append(data[0][j][i]+"\t"+data[1][j][i]+"\t");
			
			sb.append("\n");
		}
		
		return sb;
	}
	
	static void toFile(StringBuilder sb,String fname){
		try(FileWriter fw=new FileWriter(fname)){
			fw.write(sb.toString());
			
		}catch(IOException e){ e.printStackTrace();System.exit(0);}
	}
	
	
	private static final class Averager{
		//
		private List<float[][]> smpls=null;
		
		Averager(){ smpls=new ArrayList<>();}
		
		void addSample(float[][] sample){ smpls.add(sample);}
		
		float[][][] average(){
			int I=smpls.get(0).length;
			int J=smpls.get(0)[0].length;
			int L=smpls.size();
			
			float[]    tmp=new float[L];
			float[][] mean=new float[I][J];
			float[][] stde=new float[I][J];
			
			for(int i=0;i<I;i++)
			for(int j=0;j<J;j++){
				for(int l=0;l<L;l++) tmp[l]=smpls.get(l)[i][j];
				
				mean[i][j]=StatisticsUtil.cArithmeticMean(tmp);
				stde[i][j]=StatisticsUtil.cStandardDeviation(tmp)/(float)Math.sqrt(L);
			}
			
			return new float[][][]{mean,stde};
		}
	}
}
