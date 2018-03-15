//
package GDPIO;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.lagrangian.AttachedMeta;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.Record;
import miniufo.mathsphysics.PowerSpectrum;
import miniufo.mathsphysics.WindowFunction;


//
public final class PowerSpectra{
	// Indian Ocean region
	private static final boolean variancePreserving=true;
	private static final boolean outputData=false;
	
	private static final int segmentLength=4*120;
	private static final int offset=4*90;
	
	private static final float Fs=4f;
	private static final float percentage=0.80f;
	
	private static final float[] window=WindowFunction.tukey(segmentLength,0.1f);
	
	private static final String path="/lustre/home/qianyk/Data/GDP/";
	
	
	/** test*/
	public static void main(String[] args){
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunCRes2.dat"));
	}
	
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println("overlapping "+(segmentLength-offset)*100f/segmentLength+"%");
		
		List<GDPDrifter> segs=segmentalizeAll(ls);
		
		Averager[] r1=computeRegion(segs,46,7,78,26);
		Averager[] r2=computeRegion(segs,78,7,100,25);
		Averager[] r3=computeRegion(segs,38,-7,100,7);
		Averager[] r4=computeRegion(segs,55,-20,105,-8);
		Averager[] r5=computeRegion(segs,55,-37,105,-23);
		
		if(outputData){
			toFile(toString(r1[0],"data"),path+"PS/dataU1"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r1[1],"data"),path+"PS/dataV1"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r2[0],"data"),path+"PS/dataU2"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r2[1],"data"),path+"PS/dataV2"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r3[0],"data"),path+"PS/dataU3"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r3[1],"data"),path+"PS/dataV3"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r4[0],"data"),path+"PS/dataU4"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r4[1],"data"),path+"PS/dataV4"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r5[0],"data"),path+"PS/dataU5"+(variancePreserving?"VP":"")+".txt");
			toFile(toString(r5[1],"data"),path+"PS/dataV5"+(variancePreserving?"VP":"")+".txt");
		}
		
		toFile(toString(r1[0],"psd" ),path+"PS/psdU1"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r1[1],"psd" ),path+"PS/psdV1"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r2[0],"psd" ),path+"PS/psdU2"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r2[1],"psd" ),path+"PS/psdV2"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r3[0],"psd" ),path+"PS/psdU3"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r3[1],"psd" ),path+"PS/psdV3"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r4[0],"psd" ),path+"PS/psdU4"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r4[1],"psd" ),path+"PS/psdV4"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r5[0],"psd" ),path+"PS/psdU5"+(variancePreserving?"VP":"")+".txt");
		toFile(toString(r5[1],"psd" ),path+"PS/psdV5"+(variancePreserving?"VP":"")+".txt");
	}
	
	static Averager[] computeRegion(List<GDPDrifter> segs,float lons,float lats,float lone,float late){
		Averager avU=new Averager();
		Averager avV=new Averager();
		
		int count=0;
		for(GDPDrifter seg:segs)
		if(mostlyWithinRegion(seg,lons,lats,lone,late,percentage)){
			count++;
			
			float[][] uspec=getSpectrum(seg,GDPDrifter.UVEL);
			float[][] vspec=getSpectrum(seg,GDPDrifter.VVEL);
			
			avU.addSample(new Sample(uspec));
			avV.addSample(new Sample(vspec));
		}
		
		System.out.println(
			String.format("Region [%5.1fE -- %5.1fE, %5.1fN -- %5.1fN] has %5d segments",
			lons,lone,lats,late,count
		));
		
		return new Averager[]{avU,avV};
	}
	
	static float[][] getSpectrum(GDPDrifter dr,AttachedMeta meta){
		float[] data=dr.getAttachedData(meta);
		
		float[] psd=PowerSpectrum.fftPSDEstimate(data,window,Fs)[0];
		
		if(variancePreserving)
		for(int i=0,I=psd.length;i<I;i++) psd[i]*=i*Fs/segmentLength;
		
		return new float[][]{data,psd};
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
		
		AttachedMeta[] meta=dr.getAttachedMeta();
		GDPDrifter[] drs=new GDPDrifter[segments];
		
		for(int i=0;i<segments;i++){
			drs[i]=new GDPDrifter(dr.getID()+"_"+i,seglen,meta.length);
			drs[i].setAttachedMeta(meta);
			
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
	
	static StringBuilder toString(Averager av,String type){
		StringBuilder sb=new StringBuilder();
		
		if(type.equals("psd")){
			for(int j=0,J=av.smpls.get(0).psd.length;j<J;j++){
				for(int i=0,I=av.smpls.size();i<I;i++) sb.append(av.smpls.get(i).psd[j]+"\t");
				
				sb.append("\n");
			}
			
		}else if(type.equals("data")){
			for(int j=0;j<segmentLength;j++){
				for(int i=0,I=av.smpls.size();i<I;i++) sb.append(av.smpls.get(i).data[j]+"\t");
				
				sb.append("\n");
			}
			
		}else throw new IllegalArgumentException("unknow type: "+type);
		
		return sb;
	}
	
	static void toFile(StringBuilder sb,String fname){
		try(FileWriter fw=new FileWriter(fname)){
			fw.write(sb.toString());
			
		}catch(IOException e){ e.printStackTrace();System.exit(0);}
	}
	
	
	private static final class Averager{
		//
		private List<Sample> smpls=null;
		
		Averager(){ smpls=new ArrayList<>();}
		
		void addSample(Sample sample){ smpls.add(sample);}
	}
	
	private static final class Sample{
		//
		private float[] data=null;
		private float[] psd =null;
		
		Sample(float[][] smpl){
			this.data=smpl[0];
			this.psd =smpl[1];
		}
	}
}
