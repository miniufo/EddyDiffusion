//
package idealized;

import java.util.List;
import java.util.function.Function;
import diffuse.DiffusionModel;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.LSM1st;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.lagrangian.StochasticModel;
import miniufo.lagrangian.StochasticModel.BCType;
import miniufo.lagrangian.StochasticParams;
import miniufo.util.Region2D;


public final class IdealizedGenerateLD{
	// domain parameters
	private static final float olon=170;		// center longitude
	private static final float olat=0;			// center latitude
	
	// tracking parameters
	private static final int  intLen=730;		// length of integration
	private static final float kappa=1000;		// m^2/s
	private static final float TL   =4;			// days
	
	private static final float[][] diff=new float[][]{{kappa,0},{0,kappa}};
	private static final float[][] Tvel=new float[][]{{TL,1},{1,TL}};
	
	// general parameters
	private static final boolean writeTraj=false;
	
	private static final String path="/lustre/home/qianyk/Data/Idealized/Mean/";
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(1);
		
		generateLagrangianData("UniformMean");
		generateLagrangianData("Oscillate1Mean");
		generateLagrangianData("Oscillate2Mean");
		generateLagrangianData("ShearOscillateMean");
		
		ConcurrentUtil.shutdown();
	}
	
	/**
	 * generate synthetic Lagrangian drifter data
	 * 
	 * @param ctlname	mean flow data
	 * @param ensemble	ensemble number
	 * @param intLen	length of integration (steps)
	 * 
	 * @return	ps		simulated particles
	 */
	static void generateLagrangianData(String tag){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+tag+".ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Function<Record,StochasticParams> f1=r->{ return new StochasticParams(Tvel,diff);};
		
		StochasticModel sm=new LSM1st(2,dd,BCType.Landing,BCType.Landing,f1);
		sm.setVelocityBuffer("u","v",1);	// set initial velocity buffer
		
		Region2D region=new Region2D(olon-30,olat-10,olon+30,olat+10);
		
		List<Particle> ps=sm.deployPatch(region,0.5f,1,intLen);
		
		sm.simulateParticles(ps,"u","v",intLen);
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ps,path,region);
		
		System.out.println("Test of "+tag+" has "+ps.size()+" particles");
		
		DiffusionModel.writeParticleList(path+tag+"LD.dat",ps);
	}
}
