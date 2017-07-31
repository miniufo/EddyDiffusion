package idealized;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.mathsphysics.PowerSpectrum;
import miniufo.mathsphysics.WindowFunction;
import miniufo.statistics.FilterModel;


public final class OSCARPSA{
	//
	private static final boolean variancePreserving=false;
	private static final boolean removeSeasonalCycle=false;
	
	private static final int len=360;
	
	private static final float Fs=1f/5f;
	private static final float undef=-9.99e8f;
	
	private static final float[] window=WindowFunction.tukey(len,0.1f);
	
	private static final float[][] regions=new float[][]{
		{60, -3,85,  3},
		{46,  2,57, 16},
		{75,-35,90,-26}
	};
	
	private static final String path="/lustre/home/qianyk/Data/OSCAR/";
	
	
	//
	public static void main(String[] args){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IOPentad20062010.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		for(int r=0;r<regions.length;r++){
			String area="lon("+regions[r][0]+","+regions[r][2]+");lat("+regions[r][1]+","+regions[r][3]+")";
			
			System.out.println("for region: "+area);
			
			Variable u=df.getVariables(new Range(area+";t(1,"+len+")",dd),"um")[0];
			Variable v=df.getVariables(new Range(area+";t(1,"+len+")",dd),"vm")[0];
			
			Averager avU=new Averager(u.getYCount()*u.getXCount());
			Averager avV=new Averager(v.getYCount()*v.getXCount());
			
			for(int j=0,J=u.getYCount();j<J;j++)
			for(int i=0,I=u.getXCount();i<I;i++){
				float[] dataU=u.getData()[0][j][i];
				float[] dataV=v.getData()[0][j][i];
				
				float[][] reU=getSpectrum(dataU);
				float[][] reV=getSpectrum(dataV);
				
				if(reU!=null) avU.addSample(new Sample(reU));
				if(reV!=null) avV.addSample(new Sample(reV));
			}
			
			//toFile(toString(avU,"data"),path+"PS/dataU.txt");
			//toFile(toString(avV,"data"),path+"PS/dataV.txt");
			
			toFile(toString(avU,"psd" ),path+"PS/psdU"+(variancePreserving?"VP":"")+r+(removeSeasonalCycle?"NoSC":"")+"E.txt");
			toFile(toString(avV,"psd" ),path+"PS/psdV"+(variancePreserving?"VP":"")+r+(removeSeasonalCycle?"NoSC":"")+"E.txt");
		}
	}
	
	static float[][] getSpectrum(float[] data){
		for(int i=0;i<len;i++) if(data[i]==undef) return null;
		
		float[] copy=removeSeasonalCycle?FilterModel.FourierFilter(data,72,36,24):data;
		
		float[] spd=PowerSpectrum.fftPSDEstimate(copy,window,Fs)[0];
		
		if(variancePreserving)
		for(int i=0,I=spd.length;i<I;i++) spd[i]*=i*Fs/len;
		
		return new float[][]{data,spd};
	}
	
	static StringBuilder toString(Averager av,String type){
		StringBuilder sb=new StringBuilder();
		
		if(type.equals("psd")){
			for(int j=0,J=av.smpls.get(0).psd.length;j<J;j++){
				for(int i=0,I=av.smpls.size();i<I;i++) sb.append(av.smpls.get(i).psd[j]+"\t");
				
				sb.append("\n");
			}
			
		}else if(type.equals("data")){
			for(int j=0,J=av.smpls.get(0).psd.length;j<J;j++){
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
		
		Averager(int size){ smpls=new ArrayList<>(size);}
		
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
