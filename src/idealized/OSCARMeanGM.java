//
package idealized;

import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.Particle;


public final class OSCARMeanGM{
	//
	private static final String path="/lustre/home/qianyk/Data/OSCAR/Synthetic/";
	
	private static final DataDescriptor template=DiagnosisFactory.DF2.getDataDescriptor();
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(24);
		
		List<Particle> all=DiffusionModel.readParticleList(path+"LD.dat");
		System.out.println("finish reading");
		
		DataDescriptor grid=DiagnosisFactory.parseFile(path+"oscarDaily.ctl").getDataDescriptor();
		
		EulerianStatistics estat=new EulerianStatistics(all,template,true);
		
		Variable[] means=estat.reconstructMeanCyclesByGM(new float[]{1,2},4f/365f,grid);
		
		DataWrite dw=DataIOFactory.getDataWrite(grid,path+"meanGM.dat");
		dw.writeData(grid,means);	dw.closeFile();
		
		ConcurrentUtil.shutdown();
	}
}
