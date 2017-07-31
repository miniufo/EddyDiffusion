//
package GDPIO;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.EulerianStatistics;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.Record;
import miniufo.util.Region2D;


//
public final class PreprocessDrifterData{
	// Indian Ocean region
	private static final Region2D IO=new Region2D(29,-41,116,26,"IO region");
	
	private static final boolean writeTraj=false;
	private static final boolean correctWindSlip=true;
	
	private static final String path="/lustre/home/qianyk/Data/";
	
	private static final String[] dfiles={
		path+"GDP/buoydata_1_5001.dat",
		path+"GDP/buoydata_5001_10000.dat",
		path+"GDP/buoydata_10001_jun13.dat"
	};
	
	private static final String[] mfiles={
		path+"GDP/dirfl_1_5000.dat",
		path+"GDP/dirfl_5001_10000.dat",
		path+"GDP/dirfl_10001_jun13.dat"
	};
	
	
	/** test*/
	public static void main(String[] args){
		//processDrifterData();
		eddyMeanDecompose();
	}
	
	static void processDrifterData(){
		List<GDPDrifter> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,6,IO);
		//List<GDPDrifter> drog=new ArrayList<>();
		//List<GDPDrifter> undr=new ArrayList<>();
		
		// excluding South China Sea
		DiffusionModel.removeDrifterWithin(ls,new Region2D(100, 0,125,35));
		DiffusionModel.removeDrifterWithin(ls,new Region2D(106,-6,125,0 ));
		DiffusionModel.removeDrifterWithin(ls,new Region2D(15 ,17,40 ,35));
		
		// write trajectory data
		if(writeTraj) DiffusionModel.writeTrajAndGS(ls,path,IO);
		
		// attaching wind data
		DiffusionModel.addGridDataToDrifter(ls,
			DiagnosisFactory.parseFile(path+"NCEP/uvIO.ctl").getDataDescriptor()
		,"uwnd","vwnd");
		
		//splitIntoDroguedAndUndrogued(ls,drog,undr);
		
		outputVelDistribution(ls,"All");
		//outputVelDistribution(drog,"Drog");
		//outputVelDistribution(undr,"Undr");
		
		DiffusionModel.writeDrifterList(path+"GDP/IO/IO2013JunAll.dat",ls);
		//DiffusionModel.writeDrifterList(path+"GDP/IO/IO2013JunDrog.dat",drog);
		//DiffusionModel.writeDrifterList(path+"GDP/IO/IO2013JunUndr.dat",undr);
		
		// correct wind slip
		if(correctWindSlip){
			DiffusionModel.correctWindSlip(ls);
			//DiffusionModel.correctWindSlip(drog);
			//DiffusionModel.correctWindSlip(undr);
			
			System.out.println("finish correcting wind slippage\n");
			
			outputVelDistribution(ls,"AllC");
			//outputVelDistribution(drog,"DrogC");
			//outputVelDistribution(undr,"UndrC");
			
			DiffusionModel.writeDrifterList(path+"GDP/IO/IO2013JunAllC.dat",ls);
			//DiffusionModel.writeDrifterList(path+"GDP/IO/IO2013JunDrogC.dat",drog);
			//DiffusionModel.writeDrifterList(path+"GDP/IO/IO2013JunUndrC.dat",undr);
		}
	}
	
	static void eddyMeanDecompose(){
		ConcurrentUtil.initDefaultExecutor(8);
		
		boolean hasST=true;
		String suffix=hasST?"":"NoST";
		
		List<GDPDrifter> ls=
		DiffusionModel.readDrifterList(path+"GDP/IO/IO2013JunAll"+(correctWindSlip?"C":"")+".dat");
		
		EulerianStatistics estat=new EulerianStatistics(ls,
			DiagnosisFactory.DF2.getDataDescriptor(),hasST
		);
		estat.removeCyclesByGM(new float[]{1,2},4f/365f,0);
		
		DiffusionModel.writeDrifterList(path+"GDP/IO/IO2013JunAll"+(correctWindSlip?"C":"")+"Res0"+suffix+".dat",ls);
		
		ConcurrentUtil.shutdown();
	}
	
	static void splitIntoDroguedAndUndrogued(List<GDPDrifter> total,List<GDPDrifter> drog,List<GDPDrifter> undr){
		int undrogued=0,drogued=0,all=0;
		
		for(GDPDrifter dr:total)
		for(int l=0,L=dr.getTCount();l<L;l++){
			Record r=dr.getRecord(l); all++;
			
			float v=r.getDataValue(3);
			
			if(v==1) drogued++;
			else if(v==-1) undrogued++;
			else throw new IllegalArgumentException("unknown value "+v);
		}
		
		System.out.println("\ndrogued "+drogued+"("+((float)drogued/all)+")");
		System.out.println("undrogued "+undrogued+"("+((float)undrogued/all)+")");
		System.out.println("all "+all);
		
		for(GDPDrifter dr:total){
			GDPDrifter[] split=dr.splitByDrogueOffDate(3);
			
			if(split[0]!=null) drog.add(split[0]);
			if(split[1]!=null) undr.add(split[1]);
		}
		
		int dcount=0;
		for(GDPDrifter dr:drog){
			for(int l=0,L=dr.getTCount();l<L;l++)
			if(dr.getRecord(l).getDataValue(3)!=1) throw new IllegalArgumentException(dr.toString());
			
			dcount+=dr.getTCount();
		}
		
		int ucount=0;
		for(GDPDrifter dr:undr){
			for(int l=0,L=dr.getTCount();l<L;l++)
			if(dr.getRecord(l).getDataValue(3)!=-1) throw new IllegalArgumentException(dr.toString());
			
			ucount+=dr.getTCount();
		}
		
		System.out.println("\nthere are "+drog.size()+" drogued drifters " +
			"yielding "+dcount+" samples ("+(float)dcount/(dcount+ucount)+")"
		);
		System.out.println("there are "+undr.size()+" undrogued drifters " +
			"yielding "+ucount+" samples ("+(float)ucount/(dcount+ucount)+")\n"
		);
	}
	
	static void outputVelDistribution(List<GDPDrifter> ls,String suffix){
		StringBuffer uc=new StringBuffer();
		StringBuffer vc=new StringBuffer();
		StringBuffer dist=new StringBuffer();
		
		int ptr=0;
		for(GDPDrifter dr:ls)
		for(int l=0,L=dr.getTCount();l<L;l++){
			Record r=dr.getRecord(l);
			
			float u=r.getDataValue(0);
			float v=r.getDataValue(1);
			float lon=r.getLon();
			float lat=r.getLat();
			
			uc.append(u+"\n");
			vc.append(v+"\n");
			dist.append(Math.hypot(u,v)+"\t"+lon+"\t"+lat+"\n");
			
			ptr++;
		}
		
		System.out.println("ptr="+ptr+", total length="+LagrangianUtil.cTotalCount(ls));
		
		try(FileWriter fw=new FileWriter(path+"GDP/IO/PDF/uvel"+suffix+".txt")){ fw.write(uc.toString());}
		catch(IOException e){ e.printStackTrace(); System.exit(0);}
		try(FileWriter fw=new FileWriter(path+"GDP/IO/PDF/vvel"+suffix+".txt")){ fw.write(vc.toString());}
		catch(IOException e){ e.printStackTrace(); System.exit(0);}
		try(FileWriter fw=new FileWriter(path+"GDP/IO/PDF/dist"+suffix+".txt")){ fw.write(dist.toString());}
		catch(IOException e){ e.printStackTrace(); System.exit(0);}
	}
}
