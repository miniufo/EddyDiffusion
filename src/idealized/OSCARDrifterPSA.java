//
package idealized;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.mathsphysics.PowerSpectrum;
import miniufo.mathsphysics.WindowFunction;
import miniufo.statistics.Bootstrap;
import miniufo.statistics.StatisticsUtil;


//
public final class OSCARDrifterPSA{
	// Indian Ocean region
	private static final boolean variancePreserving=false;
	
	private static final int segmentLength=180;
	private static final int offset=180;
	private static final int nboot=100;
	
	private static final float Fs=1f;
	private static final float percentage=0.80f;
	
	private static final float[] window=WindowFunction.tukey(segmentLength,0.1f);
	
	private static final String path="/lustre/home/qianyk/Data/OSCAR/";
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(1);
		
		postProcess(DiffusionModel.readParticleList(path+"Synthetic/LD.dat"));
		
		ConcurrentUtil.shutdown();
	}
	
	
	static void postProcess(List<Particle> ls){
		System.out.println("overlapping "+(segmentLength-offset)*100f/segmentLength+"%");
		
		List<Particle> segs=segmentalizeAll(ls);
		
		Averager[] r1=computeRegion(segs,60,-10,100,10);
		Averager[] r2=computeRegion(segs,40,0,60,20);
		Averager[] r3=computeRegion(segs,70,-35,90,-25);
		
		toFile(toString(r1[0]),path+"PS/psdU1"+(variancePreserving?"VP":"")+"L.txt");
		toFile(toString(r1[1]),path+"PS/psdV1"+(variancePreserving?"VP":"")+"L.txt");
		toFile(toString(r2[0]),path+"PS/psdU2"+(variancePreserving?"VP":"")+"L.txt");
		toFile(toString(r2[1]),path+"PS/psdV2"+(variancePreserving?"VP":"")+"L.txt");
		toFile(toString(r3[0]),path+"PS/psdU3"+(variancePreserving?"VP":"")+"L.txt");
		toFile(toString(r3[1]),path+"PS/psdV3"+(variancePreserving?"VP":"")+"L.txt");
	}
	
	static Averager[] computeRegion(List<Particle> segs,float lons,float lats,float lone,float late){
		int len=segmentLength;
		
		Averager avU=new Averager(len%2==0?len/2+1:(len+1)/2);
		Averager avV=new Averager(len%2==0?len/2+1:(len+1)/2);
		
		int count=0;
		for(Particle seg:segs)
		if(mostlyWithinRegion(seg,lons,lats,lone,late,percentage)){
			count++;
			
			float[] uspec=getSpectrum(seg,0);
			float[] vspec=getSpectrum(seg,1);
			
			avU.addSample(uspec);
			avV.addSample(vspec);
		}
		
		System.out.println(
			String.format("Region [%5.1fE -- %5.1fE, %5.1fN -- %5.1fN] has %5d segments",
			lons,lone,lats,late,count
		));
		
		avU.average(); avU.bootstrpForSTDE(nboot);
		avV.average(); avV.bootstrpForSTDE(nboot);
		
		return new Averager[]{avU,avV};
	}
	
	static float[] getSpectrum(Particle dr,int idx){
		float[] data=dr.getAttachedData(idx);
		
		float[] psd=PowerSpectrum.fftPSDEstimate(data,window,Fs)[0];
		
		if(variancePreserving)
		for(int i=0,I=psd.length;i<I;i++) psd[i]*=i*Fs/segmentLength;
		
		return psd;
	}
	
	static List<Particle> segmentalizeAll(List<Particle> ls){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		List<Particle> allSegs=new ArrayList<>();
		
		for(Particle dr:ls){
			Particle[] drs=segmentalize(dr,segmentLength,offset);
			
			if(drs!=null)
			for(Particle drr:drs){
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
	
	static Particle[] segmentalize(Particle dr,int seglen,int offset){
		int len=dr.getTCount();
		
		if(seglen<3) throw new IllegalArgumentException("minimum segment length is 3");
		if(offset<1||seglen<offset) throw new IllegalArgumentException("offset should be in [1 seglen]");
		if(len<seglen) return null;
		
		int segments=(len-seglen)/offset+1;
		
		String[] vars=dr.getDataNames();
		Particle[] drs=new Particle[segments];
		
		for(int i=0;i<segments;i++){
			drs[i]=new GDPDrifter(dr.getID()+"_"+i,seglen,vars.length);
			drs[i].setAttachedDataNames(vars);
			
			for(int j=0,ptr=i*offset;j<seglen;j++) drs[i].addRecord(dr.getRecord(ptr++));
		}
		
		return drs;
	}
	
	static boolean mostlyWithinRegion(Particle dr,float lons,float lats,float lone,float late,float percent){
		int cc=0;
		
		for(int l=0,L=dr.getTCount();l<L;l++){
			float lon=dr.getXPosition(l);
			float lat=dr.getYPosition(l);
			
			if(lon>=lons&&lon<=lone&&lat>=lats&&lat<=late) cc++;
		}
		
		if(cc*1f/dr.getTCount()>=percent) return true;
		else return false;
	}
	
	static StringBuilder toString(Averager av){
		StringBuilder sb=new StringBuilder();
		
		for(int j=0,J=av.len;j<J;j++) sb.append(av.mean[j]+"\t"+av.stde[j]+"\n");
		
		return sb;
	}
	
	static void toFile(StringBuilder sb,String fname){
		try(FileWriter fw=new FileWriter(fname)){
			fw.write(sb.toString());
			
		}catch(IOException e){ e.printStackTrace();System.exit(0);}
	}
	
	
	private static final class Averager{
		//
		private int len=0;
		
		private float[] mean=null;
		private float[] stde=null;
		
		private List<float[]> smpls=null;
		
		
		Averager(int len){
			this.len=len;
			
			mean=new float[len];
			stde=new float[len];
			
			smpls=new ArrayList<>();
		}
		
		void addSample(float[] sample){
			if(sample.length!=len)
			throw new IllegalArgumentException("invalid sample length "+sample.length+", should be "+len);
			
			smpls.add(sample);
		}
		
		void average(){
			for(int i=0;i<len;i++){
				float[] tmp=new float[smpls.size()];
				
				for(int l=0,L=smpls.size();l<L;l++) tmp[l]=smpls.get(l)[i];
				
				mean[i]=StatisticsUtil.cArithmeticMean(tmp);
			}
		}
		
		void bootstrpForSTDE(int nboot){
			float[][] samples=new float[smpls.size()][];
			float[][] remeans=new float[nboot][len];
			
			for(int i=0,I=smpls.size();i<I;i++) samples[i]=smpls.get(i);
			
			for(int n=0;n<nboot;n++){
				float[][] resample=Bootstrap.resample(float[].class,samples);
				
				for(int i=0;i<len;i++){
					float[] tmp=new float[resample.length];
					
					for(int j=0,J=resample.length;j<J;j++) tmp[j]+=resample[j][i];
					
					remeans[n][i]=StatisticsUtil.cArithmeticMean(tmp);
				}
			}
			
			for(int i=0;i<len;i++){
				float[] tmp=new float[nboot];
				
				for(int n=0;n<nboot;n++) tmp[n]=remeans[n][i];
				
				stde[i]=StatisticsUtil.cStandardDeviation(tmp);
			}
		}
	}
}
