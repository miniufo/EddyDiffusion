//
package diffuse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import miniufo.application.statisticsModel.LagrangianStatisticsByTalyor;
import miniufo.database.AccessArgoNC;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.ArgoFloat;
import miniufo.lagrangian.Particle;


//
public class ArgoCoefficient{
	//
	private static final boolean writeTraj=false;
	
	private static final String path="d:/Data/Argo/Traj/";
	
	
	/** test*/
	public static void main(String[] args){
		List<? extends Particle> ls=getLagrangianData();
		postProcess(ls);
	}
	
	static List<? extends Particle> getLagrangianData(){
		final String[] centers={"aoml","coriolis","csio","incois","jma","kma","kordi","meds"};
		
		List<ArgoFloat> ls=new ArrayList<ArgoFloat>();
		
		StringBuffer sb=null;
		
		if(writeTraj){
			sb=new StringBuffer();
			sb.append("'sdfopen d:/Data/uwnd.2010.4dl.500.nc'\n");
			sb.append("'enable print "+path+"trajectory.gmf'\n\n");
			sb.append("'setvpage 1.3 1.1 1 1'\n");
			sb.append("'setlopts 8 0.2 60 30'\n\n");
			sb.append("'set line 2 1 1'\n\n");
		}
		
		for(String center:centers){
			List<ArgoFloat> afs=new ArrayList<ArgoFloat>();
			
			File[] fs=new File(path+center).listFiles();
			
			System.out.println(String.format("%8s has %5d files:  ",center,fs.length));
			
			for(File f:fs) if(!f.isDirectory()) AccessArgoNC.parseBasicInfo(afs,f.getAbsolutePath());
			
			int osize=afs.size();
			int nsize=0;
			long t1=Long.MAX_VALUE,t2=Long.MIN_VALUE;
			for(ArgoFloat af:afs){
				if(af.getTCount()>=1){
					long tmp=af.getTime(0);
					if(tmp<t1){ System.out.println(af.getTimes()[0]+"\t"+af.getTimes()[af.getTCount()-1]); t1=tmp;}
					tmp=af.getTime(af.getTCount()-1);
					if(tmp>t2){ System.out.println(af.getTimes()[0]+"\t"+af.getTimes()[af.getTCount()-1]); t2=tmp;}
				}
				
				af.sort();
				af.crossIDLToContinuousRecord();
				
				ArgoFloat af2=af.toDailyData();
				if(af2!=null&&af2.isContinousPosition()){
					af2.interpolateDailyPosition();
					af2.cVelocityByPosition();
					af2.crossIDLToDiscontinuousRecord();
					
					if(writeTraj){
						af2.toTrajectoryFile(path+"TXT/");
						sb.append("'tctrack uwnd "+path+"TXT/"+af.getID()+".txt'\n");
					}
					
					final int size=120;
					
					ArgoFloat[] re=af2.split(size);
					
					for(ArgoFloat afre:re) if(afre.getTCount()==size)
					ls.add(afre);
					nsize++;
				}
			}
			
			System.gc();
			System.out.println(String.format("summary: %5d/%5d (%6.2f%%)\n",nsize,osize,((float)nsize/osize*100f)));
		}
		
		if(writeTraj){
			sb.append("\n'draw title Argo trajectories'\n\n");
			sb.append("'print'\n");
			sb.append("'c'\n\n");
			sb.append("'disable print'\n");
			sb.append("'close 1'\n");
			sb.append("'reinit'\n");
			
			try{
				FileWriter fw=new FileWriter(path+"trajectory.gs");
				fw.write(sb.toString());	fw.close();
				
			}catch(IOException e){ e.printStackTrace(); System.exit(0);}
		}
		
		if(writeTraj) System.exit(0);
		
		return ls;
	}
	
	static void postProcess(List<? extends Particle> ls){
		DataDescriptor dd=DiagnosisFactory.DF2P5.getDataDescriptor();
		
		LagrangianStatisticsByTalyor lstat=new LagrangianStatisticsByTalyor(ls,dd);
		lstat.removeLagrangianMean();
		lstat.removeLagrangianTrend();
		
		Variable[] statis =lstat.cStatisticsByTaylorTheory(40,86400);
		Variable[] gridded=lstat.binningMeanByMedianPosition(statis);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"coeff.dat");
		dw.writeData(dd,gridded);	dw.closeFile();
	}
}
