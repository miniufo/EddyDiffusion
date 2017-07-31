//
package GDPIO;

import java.util.List;
import java.util.function.Predicate;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.LagrangianStatResult;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.Record;
import miniufo.util.Region2D;


//
public final class CoeffDavis2{
	// Indian Ocean region
	private static final String path="/lustre/home/qianyk/Data/GDP/";
	
	private static final DataDescriptor dd=DiagnosisFactory.DF2.getDataDescriptor();
	
	private static final Region2D[] regions=new Region2D[]{
		new Region2D( 65f,  8f, 72f, 17f),
		new Region2D( 83f,  8f, 92f, 17f),
		new Region2D( 48f,  4f, 58f, 12f),
		new Region2D( 65f, -3f, 92f,  3f),
		new Region2D( 62f,-18f, 87f,-10f),
		new Region2D( 35f,-25f, 45f,-13f),
		new Region2D( 38f,-36f, 52f,-28f),
		new Region2D( 60f,-34f, 95f,-25f),
		new Region2D(105f,-33f,112f,-23f)
	};
	
	
	/** test*/
	public static void main(String[] args){
		//postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllC.dat"),"All");
		//postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCNoBinMean.dat"),"Bin");
		//postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCRes0.dat"),"No0");
		//postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCRes1.dat"),"No1");
		postProcess(DiffusionModel.readDrifterList(path+"IO2013JunAllCRes2NoST.dat"),"No2");
	}
	
	static void postProcess(List<GDPDrifter> ls,String prefix){
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ls,dd);
		
		int tRad=4*60;
		
		LagrangianStatResult[] r1=new LagrangianStatResult[10];
		LagrangianStatResult[] r2=new LagrangianStatResult[10];
		
		for(int i=0;i<regions.length;i++){
			final int itag=i;
			
			Predicate<Record> cond=r->regions[itag].inRange(r.getLon(),r.getLat());
			
			r1[i]=lstat.cStatisticsByDavisTheory1(cond,tRad);
			r2[i]=lstat.cStatisticsByDavisTheory2(cond,tRad);
			
			r1[i].toFile(path+"Diff/R"+i+"1"+prefix+".txt");
			r2[i].toFile(path+"Diff/R"+i+"2"+prefix+".txt");
		}
	}
}
