//
package diffuse;

import java.util.List;
import miniufo.application.statisticsModel.LagrangianStatisticsByTalyor;
import miniufo.diagnosis.Variable;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.statistics.StatisticsUtil;


//
public final class ParticleStatistics{
	//
	private float[] stdU=null;	// characteristic U-speed
	private float[] stdV=null;	// characteristic V-speed
	private float[] Tu  =null;	// characteristic zonal time scale
	private float[] Tv  =null;	// characteristic meridional time scale
	private float[] Lu  =null;	// characteristic zonal length scale
	private float[] Lv  =null;	// characteristic meridional length scale
	private float[] KHu =null;	// zonal diffusivity
	private float[] KHv =null;	// meridional diffusivity
	
	private String region=null;
	
	private static final int maxLag=90;
	private static final int dt=3600*6;
	
	private List<? extends Particle> ls=null;
	
	
	//
	public ParticleStatistics(List<? extends Particle> ls,String region){
		this.ls=ls;
		this.region=region;
		
		int len=ls.get(0).getTCount();
		for(Particle p:ls) if(p.getTCount()!=len)
		throw new IllegalArgumentException("lengths of particle in the list are not the same");
		
		cStatistics();
	}
	
	
	public void printResults(){
		System.out.println("=====================================================================================");
		System.out.println(
			region+
			":   total drifter year("+LagrangianUtil.cMeanDrifterYear(ls)+")\t" +
			"mean traj spanning ("+LagrangianUtil.cMeanTrajLength(ls)+")"
		);
		System.out.println("           stdU      stdV        Tu        Tv        Lu        Lv       KHu       KHv");
		System.out.println(String.format(
			"mean: %9.3f %9.3f %9.3f %9.3f %9.3f %9.3f %9.3f %9.3f",
			StatisticsUtil.cArithmeticMean(stdU),
			StatisticsUtil.cArithmeticMean(stdV),
			StatisticsUtil.cArithmeticMean(Tu),
			StatisticsUtil.cArithmeticMean(Tv),
			StatisticsUtil.cArithmeticMean(Lu),
			StatisticsUtil.cArithmeticMean(Lv),
			StatisticsUtil.cArithmeticMean(KHu),
			StatisticsUtil.cArithmeticMean(KHv)
		));
		System.out.println(String.format(
			"std.: %9.3f %9.3f %9.3f %9.3f %9.3f %9.3f %9.3f %9.3f\n",
			StatisticsUtil.cStandardError(stdU),
			StatisticsUtil.cStandardError(stdV),
			StatisticsUtil.cStandardError(Tu),
			StatisticsUtil.cStandardError(Tv),
			StatisticsUtil.cStandardError(Lu),
			StatisticsUtil.cStandardError(Lv),
			StatisticsUtil.cStandardError(KHu),
			StatisticsUtil.cStandardError(KHv)
		));
		System.out.println("=====================================================================================");
	}
	
	
	/*** helper methods ***/
	private void cStatistics(){
		// Variable[] vs=new Variable[]{varu,varv,stdu,stdv,Tu,Tv,Lu,Lv,KHu,KHv};
		LagrangianStatisticsByTalyor lstat=new LagrangianStatisticsByTalyor(ls,null);
		lstat.removeLagrangianMean();
		lstat.removeLagrangianTrend();
		
		Variable[] vs=lstat.cStatisticsByTaylorTheory(maxLag,dt);
		
		stdU=vs[2].getData()[0][0][0].clone();
		stdV=vs[3].getData()[0][0][0].clone();
		Tu  =vs[4].getData()[0][0][0].clone();
		Tv  =vs[5].getData()[0][0][0].clone();
		Lu  =vs[6].getData()[0][0][0].clone();
		Lv  =vs[7].getData()[0][0][0].clone();
		KHu =vs[8].getData()[0][0][0].clone();
		KHv =vs[9].getData()[0][0][0].clone();
		
		int len=ls.size();
		
		if(stdU.length!=len) throw new IllegalArgumentException("invalid stdU length");
		if(stdV.length!=len) throw new IllegalArgumentException("invalid stdV length");
		if(Tu.length  !=len) throw new IllegalArgumentException("invalid Tu   length");
		if(Tv.length  !=len) throw new IllegalArgumentException("invalid Tv   length");
		if(Lu.length  !=len) throw new IllegalArgumentException("invalid Lu   length");
		if(Lv.length  !=len) throw new IllegalArgumentException("invalid Lv   length");
		if(KHu.length !=len) throw new IllegalArgumentException("invalid KHu  length");
		if(KHv.length !=len) throw new IllegalArgumentException("invalid KHv  length");
	}
}
