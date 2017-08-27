//
package idealized;

import java.util.List;
import java.util.function.Predicate;
import diffuse.DiffusionModel;
import diffuse.DiffusionModel.Method;
import miniufo.application.statisticsModel.LagrangianStatisticsByDavis;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.util.Region2D;


public final class IdealizedDiffusivity{
	//
	private static final String path="/lustre/home/qianyk/Data/Idealized/Mean/";
	
	private static final String EulerCTL=
		"dset ^ctl.dat\n"+
		"title template\n"+
		"undef -9999\n"+
		"xdef 31 linear 140 2\n"+
		"ydef 11 linear -10 2\n"+
		"zdef  1 linear   1 1\n"+
		"tdef  1 linear 1Jan2001 1dy\n"+
		"vars 1\n"+
		"u 0 99 u\n"+
		"endvars\n";
	
	
	/** test*/
	public static void main(String[] args){
		Region2D region=new Region2D(168f,-2,172,2);
		
		for(Method m:Method.values()){
			if(m==Method.Bin) cLagrangianStatistics(region,180,"UniformMean",m);
			if(m!=Method.GM&&m!=Method.GM2) cLagrangianStatistics(region,180,"Oscillate1Mean",m);
			if(m!=Method.GM2) cLagrangianStatistics(region,180,"Oscillate2Mean",m);
			cLagrangianStatistics(region,180,"ShearOscillateMean",m);
		}
	}
	
	
	/**
	 * compute Lagrangian statistics
	 * 
	 * @param	ps		particle list
	 * @param	lon1	start lon for Lagrangian statistics
	 * @param	lat1	start lat for Lagrangian statistics
	 * @param	lon2	end lon for Lagrangian statistics
	 * @param	lat2	end lat for Lagrangian statistics
	 * @param	tRad	maximum time lag
	 * @param	fname	file name stamp
	 */
	static void cLagrangianStatistics(Region2D region,int tRad,String tag,Method m){
		List<Particle> ps=DiffusionModel.readParticleList(path+tag+"LD"+m+".dat");
		
		DataDescriptor dd=DiagnosisFactory.parseContent(EulerCTL).getDataDescriptor();
		
		/**************** Lagrangian statistics ****************/
		System.out.println("\nLagrangian Statistics...");
		LagrangianStatisticsByDavis lstat=new LagrangianStatisticsByDavis(ps,dd);
		
		Predicate<Record> cond=r->region.inRange(r.getXPos(),r.getYPos());
		
		lstat.cStatisticsByDavisTheory1(cond,tRad).toFile(path+"Diff/Lstat"+tag+m+"1.txt");
		lstat.cStatisticsByDavisTheory2(cond,tRad).toFile(path+"Diff/Lstat"+tag+m+"2.txt");
	}
}
