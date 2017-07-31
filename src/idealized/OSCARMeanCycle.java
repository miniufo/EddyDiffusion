//
package idealized;

import miniufo.basic.ArrayUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.mathsphysics.HarmonicFitter;
import miniufo.test.diagnosis.MDate;


//
public final class OSCARMeanCycle{
	//
	private static final int strY=2006;
	private static final int endY=2010;
	
	private static final int[] tags=new int[endY-strY+2];
	private static final int[] days=new int[endY-strY+1];
	
	private static final String path="/lustre/home/qianyk/Data/OSCAR/";
	
	static{
		for(int y=strY;y<=endY;y++)
		if(MDate.isLeapYear(y)) tags[y-strY+1]=tags[y-strY]+366;
		else tags[y-strY+1]=tags[y-strY]+365;
		
		for(int y=strY;y<=endY;y++) days[y-strY]=MDate.isLeapYear(y)?366:365;
		
		//System.out.println(Arrays.toString(tags));
		//System.out.println(Arrays.toString(days));
	}
	
	
	//
	public static void main(String[] args){
		//computeParameters();
		reconstructMeanCycle();
	}
	
	static void computeParameters(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IOPentad"+strY+endY+"Masked.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable[] uv=df.getVariables(new Range("",dd),false,"um","vm");
		
		Variable[] u1=lsfit(uv[0],72);
		Variable[] v1=lsfit(uv[1],72);
		Variable[] u2=lsfit(uv[0],36);
		Variable[] v2=lsfit(uv[1],36);
		Variable[] u3=lsfit(uv[0],24);
		Variable[] v3=lsfit(uv[1],24);
		
		Variable um=uv[0].anomalizeT();
		Variable vm=uv[1].anomalizeT();
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"IOMeanCycleParam"+strY+endY+".dat");
		dw.writeData(dd,ArrayUtil.concatAll(Variable.class,new Variable[]{um,vm},u1,v1,u2,v2,u3,v3));
		dw.closeFile();
	}
	
	static void reconstructMeanCycle(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IOMeanCycleParam"+strY+endY+".ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable[] means=df.getVariables(new Range("",dd),false,"um","vm");
		Variable[] ucyc1=df.getVariables(new Range("",dd),false,"umamp72","umspha72");
		Variable[] ucyc2=df.getVariables(new Range("",dd),false,"umamp36","umspha36");
		Variable[] vcyc1=df.getVariables(new Range("",dd),false,"vmamp72","vmspha72");
		Variable[] vcyc2=df.getVariables(new Range("",dd),false,"vmamp36","vmspha36");
		
		Variable urec1=reconstructByAmpAndSPha(ucyc1[0],ucyc1[1],1);
		Variable urec2=reconstructByAmpAndSPha(ucyc2[0],ucyc2[1],2);
		Variable vrec1=reconstructByAmpAndSPha(vcyc1[0],vcyc1[1],1);
		Variable vrec2=reconstructByAmpAndSPha(vcyc2[0],vcyc2[1],2);
		
		Variable u=urec1.plus(urec2);	u.setName("urec");
		Variable v=vrec1.plus(vrec2);	v.setName("vrec");
		
		float undef=dd.getUndef(null);
		
		float[][][] umdata=means[0].getData()[0];
		float[][][] vmdata=means[1].getData()[0];
		
		for(int j=0,J=u.getYCount();j<J;j++)
		for(int i=0,I=u.getXCount();i<I;i++){
			float um=umdata[j][i][0];
			float vm=vmdata[j][i][0];
			
			float[] uc=u.getData()[0][j][i];
			float[] vc=v.getData()[0][j][i];
			
			if(uc[0]!=undef&&vc[0]!=undef){
				for(int l=0,L=u.getTCount();l<L;l++){
					uc[l]+=um;
					vc[l]+=vm;
				}
			}else{
				for(int l=0,L=u.getTCount();l<L;l++){
					uc[l]=undef;
					vc[l]=undef;
				}
			}
		}
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"IOMeanCycleRec"+strY+endY+".dat");
		dw.writeData(u,v); dw.closeFile();
	}
	
	static void computeInterannualCycle(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IOPentad"+strY+endY+"Masked.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable[] uv=df.getVariables(new Range("",dd),false,"um","vm");
		
		Variable[] u1=lsfit(uv[0],72);
		Variable[] v1=lsfit(uv[1],72);
		Variable[] u2=lsfit(uv[0],36);
		Variable[] v2=lsfit(uv[1],36);
		Variable[] u3=lsfit(uv[0],24);
		Variable[] v3=lsfit(uv[1],24);
		
		Variable um=uv[0].anomalizeT();
		Variable vm=uv[1].anomalizeT();
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"IOMeanCycleParam"+strY+endY+".dat");
		dw.writeData(dd,ArrayUtil.concatAll(Variable.class,new Variable[]{um,vm},u1,v1,u2,v2,u3,v3));
		dw.closeFile();
	}
	
	static int numberOfDays(){
		int days=0;
		
		for(int yy=strY;yy<=endY;yy++)
		if(MDate.isLeapYear(yy)) days+=366;
		else days+=365;
		
		return days;
	}
	
	static double getFs(int l){
		for(int i=0,I=endY-strY+1;i<I;i++)
		if(l>=tags[i]&&l<tags[i+1]) return (l-tags[i]+0.0)/days[i];
		
		throw new IllegalArgumentException("out of range for l="+l);
	}
	
	static Variable reconstructByAmpAndSPha(Variable amp,Variable pha,int factor){
		int noOfDays=numberOfDays();
		int y=amp.getYCount();
		int x=amp.getXCount();
		
		float undef=amp.getUndef();
		
		float[] o=new float[noOfDays];
		
		for(int l=0;l<noOfDays;l++){
			double fs=getFs(l);
			
			o[l]=(float)(2.0*Math.PI*fs*factor);
		}
		
		Variable re=new Variable("re",false,new Range(noOfDays,1,y,x));
		re.setUndef(undef); re.setCommentAndUnit("reconstructed");
		re.getRange().setXRange(amp.getRange());
		re.getRange().setYRange(amp.getRange());
		re.getRange().setZRange(amp.getRange());
		
		float[][][] adata=amp.getData()[0];
		float[][][] pdata=pha.getData()[0];
		
		for(int j=0;j<y;j++)
		for(int i=0;i<x;i++){
			float[] data=re.getData()[0][j][i];
			
			float a=adata[j][i][0];
			float p=pdata[j][i][0];
			
			if(a!=undef)
			for(int l=0;l<noOfDays;l++) data[l]=(float)(a*Math.sin(o[l]+p));
			else
			for(int l=0;l<noOfDays;l++) data[l]=undef;
		}
		
		return re;
	}
	
	static Variable[] lsfit(Variable v,float T){
		int t=v.getTCount(),z=v.getZCount(),y=v.getYCount(),x=v.getXCount();
		
		float undef=v.getUndef();
		
		Variable[] re=new Variable[5];
		re[0]=new Variable(v.getName()+"amp" +Math.round(T),false,new Range(1,z,y,x));
		re[1]=new Variable(v.getName()+"spha"+Math.round(T),false,new Range(1,z,y,x));
		re[2]=new Variable(v.getName()+"cpha"+Math.round(T),false,new Range(1,z,y,x));
		re[3]=new Variable(v.getName()+"a"   +Math.round(T),false,new Range(1,z,y,x));
		re[4]=new Variable(v.getName()+"b"   +Math.round(T),false,new Range(1,z,y,x));
		
		re[0].setCommentAndUnit("amplitude of harmonics of period "+T);
		re[1].setCommentAndUnit("sin initial phase of harmonics of period "+T);
		re[2].setCommentAndUnit("cos initial phase of harmonics of period "+T);
		re[3].setCommentAndUnit("sin coeff of harmonics of period "+T);
		re[4].setCommentAndUnit("cos coeff of harmonics of period "+T);
		
		re[0].setUndef(undef);
		re[1].setUndef(undef);
		re[2].setUndef(undef);
		re[3].setUndef(undef);
		re[4].setUndef(undef);
		
		float[][][] vdata =    v.getData()[0];
		float[][][] adata =re[0].getData()[0];
		float[][][] psdata=re[1].getData()[0];
		float[][][] pcdata=re[2].getData()[0];
		float[][][] sdata =re[3].getData()[0];
		float[][][] cdata =re[4].getData()[0];
		
		HarmonicFitter hf=new HarmonicFitter(t,T);
		
		for(int j=0;j<y;j++)
cc:		for(int i=0;i<x;i++){
			float[] buf=vdata[j][i];
			
			for(int l=0;l<t;l++)
			if(buf[l]==undef){
				adata [j][i][0]=undef;
				psdata[j][i][0]=undef;
				pcdata[j][i][0]=undef;
				sdata [j][i][0]=undef;
				cdata [j][i][0]=undef;
				continue cc;
			}
			
			hf.fit(buf);
			adata [j][i][0]=hf.getAmplitudes()[0];
			psdata[j][i][0]=hf.getSinInitPhases()[0];
			pcdata[j][i][0]=hf.getCosInitPhases()[0];
			sdata [j][i][0]=hf.getSinCoeffs()[0];
			cdata [j][i][0]=hf.getCosCoeffs()[0];
		}
		
		for(int m=0;m<5;m++){
			re[m].getRange().setTRange(v.getRange().getTRange()[0]);
			re[m].getRange().setZRange(v.getRange());
			re[m].getRange().setYRange(v.getRange());
			re[m].getRange().setXRange(v.getRange());
		}
		
		return re;
	}
}
