//
package GDPIO;

import java.util.Arrays;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;


public final class ValidBins{
	// Indian Ocean
	private static final String path="/lustre/home/qianyk/Data/";
	
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	//
	public static void main(String[] args){
		postProcess(DiffusionModel.readDrifterList(path+"GDP/IO2013JunAllC.dat"));
	}
	
	static void postProcess(List<GDPDrifter> ls){
		int len=4;		// length of seasons
		int seg=12/len;	// months in each seasons
		
		int segthreshold=32;
		int totalthreshold=0;
		
		int[] mons=new int[seg];
		
		BinningStatistics bs=new BinningStatistics(template);
		
		Variable[] counts=new Variable[len];
		Variable totalCnt=bs.binningCount(ls);
		
		for(int i=0;i<len;i++){
			mons[0]=i*seg+1;
			for(int ii=1;ii<seg;ii++) mons[ii]=mons[ii-1]+1;
			System.out.println(Arrays.toString(mons));
			counts[i]=bs.binningCountByDate(ls,"month",mons);
		}
		
		Variable valid=new Variable("valid",new Range("",template));
		valid.setUndef(-9999);
		valid.setCommentAndUnit("seg(len:"+seg+")>="+segthreshold+", total>="+totalthreshold);
		
		float[][] vdata=valid.getData()[0][0];
		
		for(int j=0;j<valid.getYCount();j++)
		for(int i=0;i<valid.getXCount();i++){
			boolean vld=true;
			
			for(int m=0;m<len;m++)
			if(counts[m].getData()[0][0][j][i]<segthreshold||totalCnt.getData()[0][0][j][i]<totalthreshold){
				vld=false;
				break;
			}
			
			if(vld) vdata[j][i]=1;
			else vdata[j][i]=-1;
		}
		
		DataWrite dw=DataIOFactory.getDataWrite(template,path+"/GDP/ValidBin/ValidBins"+seg+".dat");
		dw.writeData(template,valid);	dw.closeFile();
	}
}
