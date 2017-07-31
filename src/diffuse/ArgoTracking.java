//
package diffuse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import miniufo.database.AccessArgoNC;
import miniufo.lagrangian.ArgoFloat;


//
public class ArgoTracking{
	
	/** test*/
	public static void main(String[] args){
		final String[] centers={"aoml","coriolis","csio","incois","jma","kma","kordi","meds"};
		//final String[] centers={"aoml"};
		
		StringBuffer sb=new StringBuffer();
		sb.append("'sdfopen d:/Data/uwnd.2010.4dl.500.nc'\n");
		sb.append("'enable print d:/Data/Argo/Traj/trajectory.gmf'\n\n");
		sb.append("'setvpage 1.3 1.1 1 1'\n");
		sb.append("'setlopts 8 0.2 60 30'\n\n");
		sb.append("'set line 2 1 1'\n\n");
		
		for(String center:centers){
			ArrayList<ArgoFloat> afs=new ArrayList<ArgoFloat>();
			
			File[] fs=new File("d:/Data/Argo/Traj/"+center).listFiles();
			
			System.out.print(String.format("%8s has %5d files:  ",center,fs.length));
			
			for(File f:fs) if(!f.isDirectory()) AccessArgoNC.parseBasicInfo(afs,f.getAbsolutePath());
			
			int osize=afs.size();
			int nsize=0;
			
			for(ArgoFloat af:afs){
				if(af.getID().equalsIgnoreCase("2900586")) continue;
				if(af.getID().equalsIgnoreCase("2900662")) continue;
				if(af.getID().equalsIgnoreCase("3900465")) continue;
				if(af.getID().equalsIgnoreCase("4901072")) continue;
				if(af.getID().equalsIgnoreCase("4900541")) continue;
				if(af.getID().equalsIgnoreCase("5900488")) continue;
				if(af.getID().equalsIgnoreCase("5900943")) continue;
				if(af.getID().equalsIgnoreCase("5901116")) continue;
				if(af.getID().equalsIgnoreCase("5901286")) continue;
				if(af.getID().equalsIgnoreCase("6900121")) continue;
				if(af.getID().equalsIgnoreCase("6900358")) continue;
				if(af.getID().equalsIgnoreCase("6900371")) continue;
				if(af.getID().equalsIgnoreCase("6900384")) continue;
				if(af.getID().equalsIgnoreCase("7900045")) continue;
				if(af.getID().equalsIgnoreCase("7900046")) continue;
				
				if(af.getTCount()<2) continue;
				
				af.sort();
				af.crossIDLToContinuousRecord();
				
				ArgoFloat af2=af.toDailyData(); if(af2.getTCount()<2) continue;
				af2.interpolateDailyPosition();
				af2.cDailyCurrentSpeed();
				af2.truncate(20070101000000L,20071231000000L);
				af2.crossIDLToDiscontinuousRecord();
				
				if(af2.getTCount()==365){
					af2.toTrajectoryFile("d:/Data/Argo/Traj/TXT/");
					sb.append("'tctrack uwnd d:/Data/Argo/Traj/TXT/"+af.getID()+".txt'\n");
					nsize++;
				}
			}
			
			System.gc();
			System.out.println(String.format("%5d/%5d (%6.2f%%)",nsize,osize,((float)nsize/osize*100f)));
		}
		
		sb.append("\n'draw title Argo trajectories'\n\n");
		sb.append("'print'\n");
		sb.append("'c'\n\n");
		sb.append("'disable print'\n");
		sb.append("'close 1'\n");
		sb.append("'reinit'\n");
		
		try{
			FileWriter fw=new FileWriter(new File("d:/Data/Argo/Traj/trajectory.gs"));
			fw.write(sb.toString());	fw.close();
			
		}catch(IOException e){ e.printStackTrace(); System.exit(0);}
	}
}
