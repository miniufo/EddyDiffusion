//
package idealized;

import java.util.ArrayList;
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
import miniufo.lagrangian.StochasticParams;
import miniufo.lagrangian.StochasticModel.BCType;
import miniufo.util.Region2D;


//
public final class OSCARTrajectory{
	//
	private static final int initLen=90;
	
	private static final float TL      =3;			// days
	private static final float kappa   =1;			// m^2 s^-1
	
	private static final float[][] diff=new float[][]{{kappa,0},{0,kappa}};
	private static final float[][] Tvel=new float[][]{{TL,1},{1,TL}};
	
	private static final Region2D IO=new Region2D(20,-45,120,30);
	
	private static final String path="/lustre/home/qianyk/Data/OSCAR/Synthetic/";
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(1);
		
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IODaily20062010.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Function<Record,StochasticParams> f1=r->{ return new StochasticParams(Tvel,diff);};
		
		StochasticModel sm=new LSM1st(2,dd,BCType.Landing,BCType.Landing,f1);
		sm.setVelocityBuffer("um","vm",1);	// set initial velocity buffer
		
		List<Particle> ps=new ArrayList<>();
		
		Particle p1=sm.deployAt(""+100001,50,  0,initLen);	if(p1!=null) ps.add(p1);
		Particle p2=sm.deployAt(""+100002,70,  0,initLen);	if(p2!=null) ps.add(p2);
		Particle p3=sm.deployAt(""+100003,90,  0,initLen);	if(p3!=null) ps.add(p3);
		Particle p4=sm.deployAt(""+100004,55, 10,initLen);	if(p4!=null) ps.add(p4);
		Particle p5=sm.deployAt(""+100005,50,-30,initLen);	if(p5!=null) ps.add(p5);
		Particle p6=sm.deployAt(""+100006,80,-20,initLen);	if(p6!=null) ps.add(p6);
		
		System.out.println(" release "+ps.size()+" particles\n");
		
		System.out.println("start tracking...");
		for(int l=1;l<initLen;l++){
			sm.setVelocityBuffer("um","vm",l);
			sm.integrateForward(ps);
		}
		
		DiffusionModel.writeTrajAndGS(ps,path+"TXT/",IO);
		
		ConcurrentUtil.shutdown();
	}
}
