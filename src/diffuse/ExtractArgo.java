//
package diffuse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import miniufo.database.AccessArgoNC;
import miniufo.database.AccessBestTrack;
import miniufo.database.AccessBestTrack.DataSets;
import miniufo.diagnosis.SpatialModel;
import miniufo.lagrangian.ArgoFloat;
import miniufo.lagrangian.Typhoon;

//
public class ExtractArgo{
	//
	private static final String PATH="d:/ExtractArgo/";
	private static final String NAME="MEGI";
	private static final String YEAR="2010";
	
	
	/** test*/
	public static void main(String[] args){
		final String[] centers={"aoml","coriolis","csio","incois","jma","kma","kordi","meds"};
		
		StringBuilder sb=new StringBuilder();
		
		sb.append("'open d:/Data/GDP/Traj/KHGrid.ctl'\n");
		sb.append("'enable print "+PATH+"Argo"+NAME+".gmf'\n\n");
		sb.append("'set line 2 1 8'\n");
		sb.append("'set lon 105 150'\n");
		sb.append("'set lat 7 30'\n");
		sb.append("'set mpdset hires'\n");
		sb.append("'set strsiz 0.08 0.08'\n\n");
		
		sb.append("'setvpage 1 1 1 1'\n");
		
		Typhoon tr=AccessBestTrack.getTyphoons(
			"d:/Data/Typhoons/CMA/CMA.txt","name="+NAME+";time=1Jan"+YEAR+"-31Dec"+YEAR,DataSets.CMA
		).get(0);
		
		System.out.println(tr);
		
		for(String center:centers){
			ArrayList<ArgoFloat> afs=new ArrayList<ArgoFloat>();
			
			File[] fs=new File("d:/Data/Argo/Traj/"+center).listFiles();
			
			System.out.println(String.format("%8s has %5d files",center,fs.length));
			
			for(File f:fs) if(!f.isDirectory()) AccessArgoNC.parseBasicInfo(afs,f.getAbsolutePath());
			
			for(ArgoFloat af:afs){
				StringBuilder buf=new StringBuilder();
				
				int count=0;
				for(int l=0,L=af.getTCount();l<L;l++){
					long time=af.getTime(l);
					float lon=af.getLongitude(l);
					float lat=af.getLatitude(l);
					
					for(int ll=0,LL=tr.getTCount();ll<LL;ll++)
					if(Math.abs(time-tr.getTime(ll))<30000
					&&SpatialModel.cSphericalDistanceByDegree(lon,lat,tr.getLongitude(ll),tr.getLatitude(ll))<600000){
						buf.append(af.getRecord(l)+"   "+af.getRecord(l).getCycleNum());
						buf.append("\n");
						count++;
					}
				}
				
				if(count!=0){
					buf.insert(0," * "+count+" "+af.getID()+"\n");
					
					writeTXT(buf,af.getID()+".txt");
					
					sb.append("'tctrack varu "+PATH+af.getID()+".txt'\n");
				}
			}
		}
		
		sb.append("\n'draw title Argo floats during Typhoon "+NAME+" ("+YEAR+")'\n\n");
		sb.append("'print'\n");
		sb.append("'c'\n\n");
		sb.append("'disable print'\n");
		sb.append("'close 1'\n");
		sb.append("'reinit'\n");
		
		try(FileWriter fw=new FileWriter(PATH+"ExtractArgo.gs")){
			fw.write(sb.toString());
			
		}catch(IOException e){ e.printStackTrace(); System.exit(0);}
	}
	
	public static void writeTXT(StringBuilder sb,String filename){
		try{
			FileWriter fw=new FileWriter(PATH+filename);
			
			fw.write(sb.toString());
			
			fw.close();
			
		}catch(IOException e){ e.printStackTrace(); System.exit(0);}
	}
}
