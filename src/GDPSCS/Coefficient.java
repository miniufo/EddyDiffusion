//
package GDPSCS;

import java.util.ArrayList;
import java.util.List;

import miniufo.application.statisticsModel.LagrangianStatisticsByTalyor;
import miniufo.database.AccessGDPDrifter;
import miniufo.database.DataBaseUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.Particle;
import miniufo.util.Region2D;
import static miniufo.basic.ArrayUtil.concatAll;


//
public class Coefficient{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final int seglen=180;
	
	private static final String path="d:/Data/GDP/SCS/";
	
	
	/** test*/
	public static void main(String[] args){
		List<? extends Particle> ls=getLagrangianData();
		postProcess(ls);
	}
	
	static List<? extends Particle> getLagrangianData(){
		String[] files={
			"d:/Data/GDP/buoydata_1_5000.dat",
			"d:/Data/GDP/buoydata_5001_dec11.dat"
		};
		
		List<Particle> res=new ArrayList<Particle>();
		
		for(String f:files){
			List<GDPDrifter> all=new ArrayList<GDPDrifter>();
			
			AccessGDPDrifter.parseBasicGDPInfo(all,f,SCS);
			
			System.out.println("\nsize: "+all.size()+"\n");
			
			for(GDPDrifter drftr:all){
				if(drftr.isContinuous()){
					drftr.removeEndpointUndefVelocityRecords();
					
					if(drftr.hasLargeVelocityRecords(5)){
						System.out.println(drftr.getID()+" has large velocity record");
						continue;
					}
					
					GDPDrifter[] drs=null;
					
					if(drftr.hasUndefRecords(0)){
						System.out.println(drftr.getID()+" has undef velocity record");
						drs=drftr.splitByUndef();
						
					}else drs=new GDPDrifter[]{drftr};
					
					for(GDPDrifter dr:drs){
						GDPDrifter[] drSegs=AccessGDPDrifter.getRecordsWithinRegion(dr,SCS);
						
						for(GDPDrifter drSeg:drSegs)
						if(dr.getTCount()>0){
							GDPDrifter[] re=drSeg.split(seglen);
							
							for(GDPDrifter dfre:re) if(dfre.getTCount()==seglen)
							res.add(dfre);
						}
					}
					
				}else throw new IllegalArgumentException("GDPDrifter ("+drftr.getID()+") is not continuous");
			}
			
			all.clear();
			all=null;
			for(int i=0;i<10;i++) System.gc();
		}
		
		return res;
	}
	
	static void postProcess(List<? extends Particle> ls){
		DataDescriptor dd=DiagnosisFactory.DF2P5.getDataDescriptor();
		
		LagrangianStatisticsByTalyor lstat=new LagrangianStatisticsByTalyor(ls,dd);
		lstat.removeLagrangianMean();
		lstat.removeLagrangianTrend();
		
		Variable[] statis =lstat.cStatisticsByTaylorTheory(90,3600*6);
		Variable[] gridded=lstat.binningMeanByMedianPosition(statis);
		Variable count=DataBaseUtil.binningMedianCount(dd,ls);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"KHGrid.dat");
		dw.writeData(dd,concatAll(Variable.class,gridded,count));	dw.closeFile();
	}
}
