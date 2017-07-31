//
package GDPSCS;

import java.util.List;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.LagrangianStatisticsByTalyor;
import miniufo.basic.ArrayUtil;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.util.Region2D;


//
public class Scatter{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final int seglen=180;
	
	private static final boolean writeTraj=false;
	
	private static final String path="d:/Data/GDP/SCS/Scatter/";
	
	private static final String[] dfiles={
		path+"GDP/buoydata_1_5001.dat",
		path+"GDP/buoydata_5001_10000.dat",
		path+"GDP/buoydata_10001_dec12.dat"
	};
	
	private static final String[] mfiles={
		path+"GDP/dirfl_1_5000.dat",
		path+"GDP/dirfl_5001_10000.dat",
		path+"GDP/dirfl_10001_dec12.dat"
	};
	
	static final class Box{
		//
		final float lon1;
		final float lon2;
		final float lat1;
		final float lat2;
		
		public Box(float lon1,float lat1,float lon2,float lat2){
			if(lon1>=lon2||lat1>=lat2)
			throw new IllegalArgumentException("invalid box range");
			
			this.lon1=lon1;
			this.lon2=lon2;
			this.lat1=lat1;
			this.lat2=lat2;
		}
		
		public boolean inBox(float lon,float lat){
			if(lon>=lon1&&lon<=lon2&&lat>=lat1&&lat<=lat2) return true;
			
			return false;
		}
	}
	
	
	/** test*/
	public static void main(String[] args){
		List<GDPDrifter> ls=DiffusionModel.getGDPDrifterSegments(
			DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,SCS),
			seglen
		);
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ls,path,SCS);
		
		postProcess(ls);
	}
	
	static void postProcess(List<GDPDrifter> ls){
		DataDescriptor dd=DiagnosisFactory.DF2P5.getDataDescriptor();
		
		LagrangianStatisticsByTalyor lstat=new LagrangianStatisticsByTalyor(ls,dd);
		lstat.removeLagrangianMean();
		lstat.removeLagrangianTrend();
		
		Variable[] statis =lstat.cStatisticsByTaylorTheory(90,3600*6);
		Variable[] gridded=lstat.binningMeanByMedianPosition(statis);
		Variable count=DataBaseUtil.binningCount(dd,ls);
		
		Variable[] re=ArrayUtil.concatAll(Variable.class,gridded,count);
		
		System.out.println();
		for(Variable v:re) System.out.print(String.format("%10s",v.getName()));
		System.out.println();
		
		for(int j=0,J=gridded[0].getYCount();j<J;j++)
		for(int i=0,I=gridded[0].getXCount();i<I;i++)
		if(count.getData()[0][0][j][i]>1){
			for(Variable v:re)
			System.out.print(String.format("%10.5f",v.getData()[0][0][j][i]));
			
			System.out.println();
		}
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"scatter.dat");
		dw.writeData(dd,gridded);	dw.closeFile();
	}
}
