//
package idealized;

import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.CtlDataWriteStream;


public final class IdealizedMeanFlow{
	// domain parameters
	private static final int xnum=301;		// domain x-size
	private static final int ynum=101;		// domain y-size
	
	// mean flow parameters
	private static final double delta =0.2;		// interval of grid (degree)
	private static final double bgflow=0.02f;	// background mean flow
	private static final double gamma =Math.toRadians(0.0);	// shear orientation
	
	private static final String path="/lustre/home/qianyk/Data/Idealized/Mean/";
	
	
	/** test*/
	public static void main(String[] args){
		int len=730;
		
		constructMeanFlow(len,"UniformMean");
		constructMeanFlow(len,"Oscillate1Mean");
		constructMeanFlow(len,"Oscillate2Mean");
		constructMeanFlow(len,"ShearOscillateMean");
	}
	
	static void constructMeanFlow(int t,String tag){
		Variable u=new Variable("um",new Range(t,1,ynum,xnum));
		Variable v=new Variable("vm",new Range(t,1,ynum,xnum));
		
		u.setCommentAndUnit("zonal mean flow");		u.setUndef(-9999);
		v.setCommentAndUnit("meridional mean flow");	v.setUndef(-9999);
		
		float[][][][] udata=u.getData();
		float[][][][] vdata=v.getData();
		
		if(tag.indexOf("UniformMean")!=-1) uniformMean(udata,vdata);
		else if(tag.indexOf("Oscillate1Mean")!=-1) oscillate1Mean(udata,vdata);
		else if(tag.indexOf("Oscillate2Mean")!=-1) oscillate2Mean(udata,vdata);
		else if(tag.indexOf("ShearOscillateMean")!=-1) shearOscillateMean(udata,vdata);
		else throw new IllegalArgumentException("unknown tag: "+tag);
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+tag+".dat");
		cdws.writeData(u,v);	cdws.closeFile();
	}
	
	static void uniformMean(float[][][][] udata,float[][][][] vdata){
		for(int l=0,L=udata[0][0][0].length;l<L;l++)
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			// uniformMean
			double tmp=bgflow;
			
			double sing=Math.sin(gamma);
			double cosg=Math.cos(gamma);
			
			udata[0][j][i][l]=(float)(cosg*tmp);
			vdata[0][j][i][l]=(float)(sing*tmp);
		}
	}
	
	static void oscillate1Mean(float[][][][] udata,float[][][][] vdata){
		for(int l=0,L=udata[0][0][0].length;l<L;l++)
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			// oscillate1Mean
			double tmp=bgflow+0.04*Math.sin(2.0*Math.PI*l/45.0);
			
			double sing=Math.sin(gamma);
			double cosg=Math.cos(gamma);
			
			udata[0][j][i][l]=(float)(cosg*tmp);
			vdata[0][j][i][l]=(float)(sing*tmp);
		}
	}
	
	static void oscillate2Mean(float[][][][] udata,float[][][][] vdata){
		double fai1=-1.64;
		double fai2=-0.85;
		
		for(int l=0,L=udata[0][0][0].length;l<L;l++)
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			// oscillate2Mean
			double tmp=bgflow+0.03*Math.sin(2.0*Math.PI*l/365.0+fai1)+0.02*Math.sin(2.0*Math.PI*l/182.5+fai2);
			
			double sing=Math.sin(gamma);
			double cosg=Math.cos(gamma);
			
			udata[0][j][i][l]=(float)(cosg*tmp);
			vdata[0][j][i][l]=(float)(sing*tmp);
		}
	}
	
	static void shearOscillateMean(float[][][][] udata,float[][][][] vdata){
		double fai1=-1.64;
		double fai2=-0.85;
		
		for(int l=0,L=udata[0][0][0].length;l<L;l++)
		for(int j=0;j<ynum;j++)
		for(int i=0;i<xnum;i++){
			// shearOscillateMean
			double tmp=bgflow+0.05*(-10.5+j*delta)+0.03*Math.sin(2.0*Math.PI*l/365.0+fai1)+0.02*Math.sin(2.0*Math.PI*l/182.5+fai2);
			
			double sing=Math.sin(gamma);
			double cosg=Math.cos(gamma);
			
			udata[0][j][i][l]=(float)(cosg*tmp);
			vdata[0][j][i][l]=(float)(sing*tmp);
		}
	}
}
