//
package idealized;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import diffuse.DiffusionModel;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.LSM1st;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.lagrangian.StochasticModel;
import miniufo.lagrangian.StochasticParams;
import miniufo.lagrangian.StochasticModel.BCType;
import miniufo.test.diagnosis.MDate;
import miniufo.util.Region2D;


public final class OSCARGenerateLD{
	//
	private static final boolean writeGS=false;
	
	private static final int strYear =2006;
	private static final int endYear =2010;
	private static final int ensemble=1;
	private static final int trackLen=180;
	
	private static final float interval=1.5f;
	private static final float TL      =4;			// days
	private static final float kappa   =1;			// m^2 s^-1
	
	private static final float[][] diff=new float[][]{{kappa,0},{0,kappa}};
	private static final float[][] Tvel=new float[][]{{TL,1},{1,TL}};
	
	private static final Region2D IO=new Region2D(20,-45,120,30);
	
	private static final String path="/lustre/home/qianyk/Data/OSCAR/Synthetic/";
	
	
	/** test*/
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(1);
		
		List<Particle> all=generateLagrangianData();
		
		DiffusionModel.writeParticleList(path+"LDnew.dat",all);
		
		if(writeGS) DiffusionModel.writeTrajAndGS(all,path,new Region2D(20,120,-45,30));
		
		ConcurrentUtil.shutdown();
	}
	
	
	static List<Particle> generateLagrangianData(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"IODaily"+strYear+endYear+".ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Function<Record,StochasticParams> f1=r->{ return new StochasticParams(Tvel,diff);};
		
		StochasticModel sm=new LSM1st(2,dd,BCType.Landing,BCType.Landing,f1);
		
		int ddstrYear=dd.getTDef().getSamples()[0].getYear();
		int[][] tags=initTags(dd);
		
		int stopTrakcingTag=dd.getTCount();
		
		List<Particle> all=new ArrayList<>();
		
		System.out.println("stop t-tag: "+stopTrakcingTag);
		
		System.out.println("start tracking...");
		
		for(int y=strYear;y<=endYear;y++){
			int[] integLen=MDate.isLeapYear(y)?new int[]{91,91,92,92}:new int[]{90,91,92,92};
			
			int original=tags[y-ddstrYear][0];
			
			for(int ii=0;ii<4;ii++){
				long time=dd.getTDef().getSamples()[original-1].getLongTime();
				
				sm.setVelocityBuffer("um","vm",original);	// set initial velocity buffer
				
				List<Particle> ps=sm.deployPatch(IO,interval,ensemble,trackLen+1);
				
				DiffusionModel.removeDrifterWithin(ps,new Region2D(100, 0,125,35));
				DiffusionModel.removeDrifterWithin(ps,new Region2D(106,-6,125,0 ));
				DiffusionModel.removeDrifterWithin(ps,new Region2D(15 ,17,40 ,35));
				
				System.out.println(original+"("+time+")\t"+(original+trackLen)+", yielding "+ps.size()+" particles");
				
				for(int l=original,L=original+trackLen;l<L;l++){
					if(l>=stopTrakcingTag) break;
					
					sm.setVelocityBuffer("um","vm",l);
					sm.integrateForward(ps);
				}
				
				all.addAll(ps);
				
				original+=integLen[ii];
			}
			
			System.out.println("finish "+y+" year");
		}
		
		processDrifterInfo(all,dd.getUndef(null));
		
		return all;
	}
	
	static int[][] initTags(DataDescriptor dd){
		int ystr=dd.getTDef().getSamples()[0].getYear();
		int yend=dd.getTDef().getSamples()[dd.getTCount()-1].getYear();
		
		int[][] tags=new int[yend-ystr+1][2];
		
		tags[0][0]=1;
		
		for(int l=ystr;l<yend;l++){
			if(MDate.isLeapYear(l)) tags[l-ystr][1]=tags[l-ystr][0]+366;
			else tags[l-ystr][1]=tags[l-ystr][0]+365;
			
			tags[l-ystr+1][0]=tags[l-ystr][1];
		}
		
		tags[tags.length-1][1]=tags[tags.length-1][0]+(MDate.isLeapYear(yend)?366:365);
		
		return tags;
	}
	
	static void changeUndef(List<Particle> ps,float originUndef){
		for(Particle p:ps)
		for(int l=0;l<p.getTCount();l++){
			Record r=p.getRecord(l);
			
			if(r.getDataValue(0)==originUndef) r.setData(0,Record.undef);
			if(r.getDataValue(1)==originUndef) r.setData(1,Record.undef);
		}
	}
	
	static void processDrifterInfo(List<Particle> ps,float undef){
		int fnshC=0,unfhC=0;
		
		for(Particle p:ps){
			if(p.isFinished()) fnshC++;
			else unfhC++;
		}
		
		System.out.println("deployed "+ps.size()+" drifters with "+fnshC+" stop tracking and "+unfhC+" alive");
		
		if(writeGS) DiffusionModel.writeTrajAndGS(ps,path+"",IO);
		
		changeUndef(ps,undef);
		
		System.out.println(
			"yielding total "+LagrangianUtil.cTotalDrifterYear(ps)+" drifter-year data"
		);
	}
}
