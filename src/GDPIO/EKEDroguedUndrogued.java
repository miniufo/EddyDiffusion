//
package GDPIO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.basic.ArrayUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataWrite;
import miniufo.io.DataIOFactory;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.Particle;
import miniufo.util.Region2D;


//
public final class EKEDroguedUndrogued{
	// Indian Ocean region
	private static final Region2D IO=new Region2D(29,-41,116,26,"IO region");
	
	private static final int binThreshold=-1;
	
	private static final String path="/lustre/home/qianyk/Data/";
	
	// time-invariant DataDescriptor
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunDrog.dat"),"Drog");
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunUndr.dat"),"Undr");
	}
	
	static void postProcess(List<? extends Particle> ls,String prefix){
		System.out.println("This subset spans "+
			LagrangianUtil.cTotalDrifterYear(ls)+
		" drifter-years");
		
		EulerianStatistics estat=new EulerianStatistics(ls,template,true);
		estat.maskoutByBinObservation(binThreshold);
		
		Variable EKE=estat.cEKE();
		Variable[] ellipse=estat.cVarianceEllipse();
		
		Variable mask=DiagnosisFactory.getVariables(path+"GDP/ValidBin/ValidBins3.ctl","","valid")[0];
		writeData2D(template,EKE.getData()[0][0],mask.getData()[0][0],path+"GDP/EKE/EKE_"+prefix+".txt");
		estat.writeDataForMatlab(path+"GDP/EKE/ellipse_"+prefix+".txt",mask.getData()[0][0],ellipse);
		
		DataWrite dw=DataIOFactory.getDataWrite(template,path+"GDP/EKE/EKE_"+prefix+".dat");
		dw.writeData(template,ArrayUtil.concatAll(Variable.class,ellipse,EKE));
		dw.closeFile();
	}
	
	static void writeData2D(DataDescriptor dd,float[][] data,float[][] count,String path){
		try(BufferedWriter br=new BufferedWriter(new FileWriter(path))){
			float lon1=IO.getXMin();
			float lon2=IO.getXMax();
			float lat1=IO.getYMin();
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
}
