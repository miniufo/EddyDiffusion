package GDPIO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.mathsphysics.GaussMarkovEstimator;
import miniufo.mathsphysics.GMBlas;
import miniufo.mathsphysics.GaussMarkovEstimator.AutoCorrType;
import miniufo.mathsphysics.HarmonicFitter;


//
public final class GridDecomposition{
	//
	private static final String path="d:/Data/GDP/IO/Interannual/";
	private static boolean method1=true;
	
	//
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(3);
		
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"sst.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable sst=df.getVariables(new Range("",dd),false,"sst")[0];
		
		long str=System.currentTimeMillis();
		Variable[] amps=method1?
			compute(sst,dd.getTDef().getLongSamples(),new float[]{1,2},5f/365f):
			compute2(sst,new float[]{1,2});
		System.out.println(" using "+(System.currentTimeMillis()-str)/1000+" seconds");
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+(method1?"amps.dat":"amps2.dat"));
		dw.writeData(amps);	dw.closeFile();
		
		ConcurrentUtil.shutdown();
	}
	
	
	static Variable[] compute(Variable v,long[] times,float[] freqs,float TL){
		float[][][] data=v.getData()[0];
		
		Variable[] amps=new Variable[freqs.length];
		
		for(int m=0,M=amps.length;m<M;m++){
			amps[m]=new Variable("amp"+m,v);
			amps[m].setCommentAndUnit("amplitude for frequency "+m);
		}
		
		List<Future<float[]>> ls=new ArrayList<>(v.getXCount());
		ExecutorService es=ConcurrentUtil.defaultExecutor();
		CompletionService<float[]> cs=new ExecutorCompletionService<>(es);
		
		for(int j=0,J=v.getYCount();j<J;j++){
			System.out.println(j);
			
			for(int i=0,I=v.getXCount();i<I;i++)
			ls.add(cs.submit(new SolverAmplitude(data[j][i],times,freqs,TL)));
			
			try{
				for(int i=0,I=v.getXCount();i<I;i++){
					float[] res=ls.get(i).get();
					
					for(int m=0,M=amps.length;m<M;m++) amps[m].getData()[0][j][i][0]=res[m];
				}
			}
			catch(InterruptedException e){ e.printStackTrace(); System.exit(0);}
			catch(ExecutionException   e){ e.printStackTrace(); System.exit(0);}
			
			ls.clear();
		}
		
		return amps;
	}
	
	static Variable[] compute2(Variable v,float[] freqs){
		float[][][] data=v.getData()[0];
		
		Variable[] amps=new Variable[freqs.length];
		
		for(int m=0,M=amps.length;m<M;m++){
			amps[m]=new Variable("amp"+(m+1),v);
			amps[m].setCommentAndUnit("amplitude for frequency "+(m+1));
		}
		
		List<Future<float[]>> ls=new ArrayList<>(v.getXCount());
		ExecutorService es=ConcurrentUtil.defaultExecutor();
		CompletionService<float[]> cs=new ExecutorCompletionService<>(es);
		
		for(int j=0,J=v.getYCount();j<J;j++){
			System.out.println(j);
			
			for(int i=0,I=v.getXCount();i<I;i++)
			ls.add(cs.submit(new SolverAmplitude2(data[j][i],freqs)));
			
			try{
				for(int i=0,I=v.getXCount();i<I;i++){
					float[] res=ls.get(i).get();
					
					for(int m=0,M=amps.length;m<M;m++) amps[m].getData()[0][j][i][0]=res[m];
				}
			}
			catch(InterruptedException e){ e.printStackTrace(); System.exit(0);}
			catch(ExecutionException   e){ e.printStackTrace(); System.exit(0);}
			
			ls.clear();
		}
		
		return amps;
	}
	
	
	/*** helper class ***/
	private static final class SolverAmplitude implements Callable<float[]>{
		//
		private float TL=0;
		
		private  long[] tims =null;
		private float[] freqs=null;
		private float[] data =null;
		
		public SolverAmplitude(float[] data,long[] tims,float[] freqs,float TL){
			this.data =data;
			this.tims =tims;
			this.freqs=freqs;
			this.TL   =TL;
		}
		
		public float[] call(){
			if(data[0]!=-9.99e8f){
				GaussMarkovEstimator gme=new GMBlas(data,tims);
				gme.setFrequenciesAndTimescales(AutoCorrType.TCosExp,freqs,TL);
				gme.estimateCycles(false);
				
				return gme.getCycleAmplitudes();
			}
			
			float[] re=new float[freqs.length];
			
			for(int l=0,L=re.length;l<L;l++) re[l]=-9.99e8f;
			
			return re;
		}
	}
	
	private static final class SolverAmplitude2 implements Callable<float[]>{
		//
		private float[] data=null;
		private float[] fs  =null;
		
		public SolverAmplitude2(float[] data,float[] fs){
			this.data=data;
			this.fs  =fs;
		}
		
		public float[] call(){
			float[] re=new float[fs.length];
			
			for(int l=0,L=fs.length;l<L;l++) re[l]=-9.99e8f;
			
			if(data[0]!=-9.99e8f){
				int ptr=0;
				
				for(float f:fs){
					float T=12/f;
					HarmonicFitter hf=new HarmonicFitter(data.length,T);
					hf.fit(data);
					re[ptr++]=hf.getAmplitudes()[0];
				}
			}
			
			return re;
		}
	}
}
