//
package GDPIO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.basic.ArrayUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataWrite;
import miniufo.io.DataIOFactory;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.util.Region2D;


//
public final class CyclesFiltering2{
	// Indian Ocean region
	private static final Region2D IO=new Region2D(29,-41,116,26,"IO region");
	
	private static final int binThreshold=-1;
	
	private static final String path="/lustre/home/qianyk/Data/";
	
	// time-invariant DataDescriptor
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunAllC.dat"),"All");
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunAllCRes0.dat"),"No0");
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunAllCRes1.dat"),"No1");
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunAllCRes2.dat"),"No2");
	}
	
	static void postProcess(List<? extends Particle> ls,String prefix){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,true);
		estat.maskoutByBinObservation(binThreshold);
		
		Variable count=new BinningStatistics(template).binningCount(ls);
		Variable EKE=estat.cEKE();
		Variable[] ellipse=estat.cVarianceEllipse();
		
		Variable mask=DiagnosisFactory.getVariables(path+"GDP/ValidBin/ValidBins3.ctl","","valid")[0];
		writeData2D(template,EKE.getData()[0][0],mask.getData()[0][0],path+"GDP/EKE/EKE_"+prefix+".txt");
		estat.writeDataForMatlab(path+"GDP/EKE/ellipse_"+prefix+".txt",mask.getData()[0][0],ellipse);
		
		DataWrite dw=DataIOFactory.getDataWrite(template,path+"GDP/EKE/EKE_"+prefix+".dat");
		dw.writeData(template,ArrayUtil.concatAll(Variable.class,ellipse,EKE));
		dw.closeFile();
		
		/***/
		estat.normalizeByDividingSTD();
		
		int len=LagrangianUtil.cDefinedCount(ls);
		
		System.out.println("total data size (6hr): "+len);
		
		float[] dataU=new float[len];
		float[] dataV=new float[len];
		float[] posiX=new float[len];
		float[] posiY=new float[len];
		
		float[][] mdata=mask.getData()[0][0];
		
		int pos=0;
		for(Particle p:ls){
			float[] uvel=p.getUVel();
			float[] vvel=p.getVVel();
			float[] lons=p.getXPositions();
			float[] lats=p.getYPositions();
			
			for(int l=0,L=uvel.length;l<L;l++){
				int jtag=template.getYNum(lats[l]);
				int itag=template.getXNum(lons[l]);
				
				if(uvel[l]!=Record.undef&&mdata[jtag][itag]>binThreshold){
					dataU[pos]=uvel[l];
					dataV[pos]=vvel[l];
					posiX[pos]=lons[l];
					posiY[pos]=lats[l];
					pos++;
				}
			}
		}
		
		System.out.println("ptr: "+pos);
		
		dataToFile(path+"GDP/PDF/",prefix,count.getData()[0][0],dataU,dataV,posiX,posiY,pos);
	}
	
	static void writeData2D(DataDescriptor dd,float[][] data,float[][] count,String path){
		try(BufferedWriter br=new BufferedWriter(new FileWriter(path))){
			float lon1=IO.getXMin();
			float lat1=IO.getXMax();
			float lon2=IO.getYMin();
			float lat2=IO.getYMax();
			
			for(int j=dd.getYNum(lat1),I=dd.getXNum(lon2),J=dd.getYNum(lat2);j<=J;j++){
				for(int i=dd.getXNum(lon1);i<I;i++)
				if(count[j][i]>binThreshold) br.write(data[j][i]+"  ");
				else br.write("-9999.0  ");
				
				if(count[j][I]>binThreshold) br.write(data[j][I]+"\n");
				else br.write("-9999.0\n");
			}
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
	
	static void dataToFile(String fname,String resolution,float[][] count,
	float[] dataU,float[] dataV,float[] posiX,float[] posiY,int size){
		StringBuilder sbU=new StringBuilder();
		StringBuilder sbV=new StringBuilder();
		StringBuilder sbP=new StringBuilder();
		
		for(int i=0;i<size;i++){
			int tagX=template.getXNum(posiX[i]);
			int tagY=template.getYNum(posiY[i]);
			
			if(count[tagY][tagX]>=binThreshold){
				sbU.append(dataU[i]+"\n");
				sbV.append(dataV[i]+"\n");
				sbP.append(posiX[i]+"\t"+posiY[i]+"\n");
			}
		}
		
		try{
			FileWriter fw=null;
			fw=new FileWriter(fname+"dataU"+resolution+".txt");
			fw.write(sbU.toString());	fw.close();
			
			fw=new FileWriter(fname+"dataV"+resolution+".txt");
			fw.write(sbV.toString());	fw.close();
			
			fw=new FileWriter(fname+"posit"+resolution+".txt");
			fw.write(sbP.toString());	fw.close();
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
}
