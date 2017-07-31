//
package GDPSCS;

import java.util.List;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.LagrangianStatisticsByTalyor;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.GDPDrifter;
import miniufo.util.Region2D;


//
public class Segment{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final int seglen=120;
	
	private static final boolean writeTraj=false;
	
	private static final String path="d:/Data/GDP/SCS/SegmentDistr/";
	
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
		DataDescriptor dd=DiagnosisFactory.DF1.getDataDescriptor();
		
		LagrangianStatisticsByTalyor lstat=new LagrangianStatisticsByTalyor(ls,dd);
		lstat.removeLagrangianMean();
		lstat.removeLagrangianTrend();
		
		lstat.writeMedianDistribution(path+"median"+seglen+".txt");
	}
}
