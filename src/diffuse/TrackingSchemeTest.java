//
package diffuse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import diffuse.DiffusionModel;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.LSM0th;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.lagrangian.LSM1st;
import miniufo.lagrangian.LSM2nd;
import miniufo.lagrangian.StochasticModel;
import miniufo.lagrangian.StochasticModel.BCType;
import miniufo.lagrangian.StochasticParams;
import miniufo.util.Region2D;


//
public final class TrackingSchemeTest{
	//
	private static final int initLen=40;
	
	private static final float strLon=120f;
	private static final float strLat=20f;
	
	private static final float kappa=1000;		// m^2/s
	private static final float Tvsym=3;			// days
	private static final float Tasym=1;			// days
	
	private static final float[][] diff=new float[][]{{kappa,0},{0,kappa}};
	private static final float[][] Tvel=new float[][]{{Tvsym,1},{1,Tvsym}};
	private static final float[][] Tacc=new float[][]{{Tasym,1},{1,Tasym}};
	
	private static final String path="D:/Data/Idealized/Validation/";
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(1);
		
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"uv.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Region2D region=dd.toRegion2D();
		
		int dtRatio=2;
		
		Function<Record,StochasticParams> f0=r->{ return new StochasticParams(dd.getDTDef()[0]/dtRatio,diff);};
		Function<Record,StochasticParams> f1=r->{ return new StochasticParams(Tvel,diff);};
		Function<Record,StochasticParams> f2=r->{ return new StochasticParams(Tvel,Tacc,diff);};
		
		StochasticModel sm0=new LSM0th(dtRatio,dd,BCType.Periodic,BCType.Reflected,f0);
		StochasticModel sm1=new LSM1st(dtRatio,dd,BCType.Periodic,BCType.Reflected,f1);
		StochasticModel sm2=new LSM2nd(dtRatio,dd,BCType.Periodic,BCType.Reflected,f2);
		
		sm0.setVelocityBuffer("u","v",1);
		sm1.setVelocityBuffer("u","v",1);
		sm2.setVelocityBuffer("u","v",1);
		
		List<Particle> ps0=new ArrayList<>();
		List<Particle> ps1=new ArrayList<>();
		List<Particle> ps2=new ArrayList<>();
		
		Particle p1=sm0.deployAt(""+100001,strLon,strLat,initLen);	if(p1!=null) ps0.add(p1);
		Particle p2=sm0.deployAt(""+100002,strLon,strLat,initLen);	if(p2!=null) ps0.add(p2);
		Particle p3=sm0.deployAt(""+100003,strLon,strLat,initLen);	if(p3!=null) ps0.add(p3);
		
		Particle p4=sm1.deployAt(""+100004,strLon,strLat,initLen);	if(p4!=null) ps1.add(p4);
		Particle p5=sm1.deployAt(""+100005,strLon,strLat,initLen);	if(p5!=null) ps1.add(p5);
		Particle p6=sm1.deployAt(""+100006,strLon,strLat,initLen);	if(p6!=null) ps1.add(p6);
		
		Particle p7=sm2.deployAt(""+100007,strLon,strLat,initLen);	if(p7!=null) ps2.add(p7);
		Particle p8=sm2.deployAt(""+100008,strLon,strLat,initLen);	if(p8!=null) ps2.add(p8);
		Particle p9=sm2.deployAt(""+100009,strLon,strLat,initLen);	if(p9!=null) ps2.add(p9);
		
		System.out.println(" release "+ps0.size()+" particles\n");
		for(int l=1;l<initLen;l++){
			sm0.setVelocityBuffer("u","v",l);
			sm0.integrateForward(ps0);
		}
		DiffusionModel.writeTrajAndGS(ps0,path+"TXT/",region);
		
		
		System.out.println(" release "+ps1.size()+" particles\n");
		for(int l=1;l<initLen;l++){
			sm1.setVelocityBuffer("u","v",l);
			sm1.integrateForward(ps1);
		}
		DiffusionModel.writeTrajAndGS(ps1,path+"TXT/",region);
		
		System.out.println(" release "+ps2.size()+" particles\n");
		for(int l=1;l<initLen;l++){
			sm2.setVelocityBuffer("u","v",l);
			sm2.integrateForward(ps2);
		}
		DiffusionModel.writeTrajAndGS(ps2,path+"TXT/",region);
		
		ConcurrentUtil.shutdown();
	}
}
