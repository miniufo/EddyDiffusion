//
package idealized;

import miniufo.basic.InterpolationModel.Type;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.test.diagnosis.MDate;


//
public final class OSCARToDaily{
	//
	private static final int strY=2006;
	private static final int endY=2010;
	
	private static final String path="/lustre/home/qianyk/Data/OSCAR/";
	
	
	//
	public static void main(String[] args){
		//maskout();
		interpolation();
	}
	
	static void interpolation(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IOPentad"+strY+endY+"Masked.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		int len=0;
		
		for(int yy=strY;yy<=endY;yy++){
			if(MDate.isLeapYear(yy)) len+=366;
			else len+=365;
		}
		
		System.out.println("from "+strY+" to "+endY+", yielding "+len+" days");
		
		Variable[] vs=df.getVariables(new Range("",dd),"um","vm");
		
		/*************** for interpolation to daily data ***************/
		Variable u=vs[0].interpolateT(len,Type.LINEAR);
		Variable v=vs[1].interpolateT(len,Type.LINEAR);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"IODaily"+strY+endY+".dat");
		dw.writeData(u,v);	dw.closeFile();
	}
	
	// maskout those grid has some undefined value along t-dim or uv has different masks
	static void maskout(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IOPentad"+strY+endY+".ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable[] vs=df.getVariables(new Range("",dd),"um","vm");
		
		float undef=dd.getUndef(null);
		
		// check temporal undef value (maskout all defined value if one undefined in T)
		for(int j=0,J=dd.getYCount();j<J;j++)
		for(int i=0,I=dd.getXCount();i<I;i++){
			float[] udata=vs[0].getData()[0][j][i];
			float[] vdata=vs[1].getData()[0][j][i];
			
			int uc=0; boolean uvsame=true;
			for(int l=0,L=udata.length;l<L;l++){
				if(udata[l]!=undef) uc++;
				uvsame=(udata[l]==undef)==(vdata[l]==undef);
			}
			
			if(uc!=0&&uc!=udata.length)
			System.out.println("j="+j+", i="+i+" has "+uc+" valid data ("+udata.length+")");
			
			if(!uvsame)
			System.out.println("j="+j+", i="+i+" uv not same");
			
			if((uc!=0&&uc!=udata.length)||(!uvsame)) for(int l=0,L=udata.length;l<L;l++){
				udata[l]=undef;
				vdata[l]=undef;
			}
		}
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"IOPentad"+strY+endY+"Masked.dat");
		dw.writeData(dd,vs);	dw.closeFile();
	}
}
