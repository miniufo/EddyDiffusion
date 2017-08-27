//
package GDPSCS;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import diffuse.DiffusionModel;
import diffuse.ParticleStatistics;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.util.Region2D;


//
public class Regions{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final int seglen=180;
	
	private static final String path="d:/Data/GDP/SCS/Boxes/";
	
	private static final String[] dfiles={
		path+"GDP/buoydata_1_5001.dat",
		path+"GDP/buoydata_5001_10000.dat",
		path+"GDP/buoydata_10001_dec12.dat"
	};
	
	private static final String[] mfiles={
		path+"GDP/dirfl_1_5000.dat",
		path+"GDP/dirfl_5001_10000.dat",
		path+"GDP/dirfl_10001_dec12.dat"
	};
	
	
	/** test*/
	public static void main(String[] args){
		List<? extends Particle> ls=DiffusionModel.getGDPDrifterSegments(
			DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,SCS),
			seglen
		);
		postProcess(ls);
	}
	
	static void postProcess(List<? extends Particle> ls){
		Region2D box1=new Region2D(121.5f,18   ,124   ,22.5f,"Box1");
		Region2D box2=new Region2D(117.5f,19   ,120   ,23   ,"Box2");
		Region2D box3=new Region2D(113.5f,18   ,116   ,22   ,"Box3");
		Region2D box4=new Region2D(108   ,15   ,112.5f,19   ,"Box4");
		Region2D box5=new Region2D(104.5f, 6.5f,110   ,11.5f,"Box5");
		
		List<Particle> ls1=getRecordInRegion(ls,box1);
		List<Particle> ls2=getRecordInRegion(ls,box2);
		List<Particle> ls3=getRecordInRegion(ls,box3);
		List<Particle> ls4=getRecordInRegion(ls,box4);
		List<Particle> ls5=getRecordInRegion(ls,box5);
		
		new ParticleStatistics(ls1,box1.getName()).printResults();
		new ParticleStatistics(ls2,box2.getName()).printResults();
		new ParticleStatistics(ls3,box3.getName()).printResults();
		new ParticleStatistics(ls4,box4.getName()).printResults();
		new ParticleStatistics(ls5,box5.getName()).printResults();
		
		for(Particle p:ls1) p.toTrajectoryFile(path+box1.getName()+"/");
		for(Particle p:ls2) p.toTrajectoryFile(path+box2.getName()+"/");
		for(Particle p:ls3) p.toTrajectoryFile(path+box3.getName()+"/");
		for(Particle p:ls4) p.toTrajectoryFile(path+box4.getName()+"/");
		for(Particle p:ls5) p.toTrajectoryFile(path+box5.getName()+"/");
		
		StringBuilder sb=new StringBuilder();
		
		sb.append("'sdfopen d:/Data/NCEP/OriginalNC/lsmask.192.94.nc'\n");
		sb.append("'enable print "+path+"TrajBox.gmf'\n\n");
		sb.append("'set grid off'\n");
		sb.append("'set grads off'\n");
		sb.append("'set lon "+SCS.getLonMin()+" "+SCS.getLonMax()+"'\n");
		sb.append("'set lat "+SCS.getLatMin()+" "+SCS.getLatMax()+"'\n");
		sb.append("'set mpdset mres'\n\n");
		
		sb.append(box1.getName()+"='"+box1+"'\n");
		sb.append(box2.getName()+"='"+box2+"'\n");
		sb.append(box3.getName()+"='"+box3+"'\n");
		sb.append(box4.getName()+"='"+box4+"'\n");
		sb.append(box5.getName()+"='"+box5+"'\n\n");
		
		sb.append("'setvpage 2 3 2 1'\n");
		sb.append("'setlopts 7 0.2 5 5'\n");
		sb.append("'set line 2 1 1'\n");
		sb.append("'distribute lsmask 0.05 d:/Data/GDP/SCS/SegmentDistr/median180.txt'\n");
		sb.append("'basemap L 15 1 M'\n");
		sb.append("'set line 1 1 9'\n");
		sb.append("'drawrect '"+box1.getName()+"\n");
		sb.append("'drawrect '"+box2.getName()+"\n");
		sb.append("'drawrect '"+box3.getName()+"\n");
		sb.append("'drawrect '"+box4.getName()+"\n");
		sb.append("'drawrect '"+box5.getName()+"\n");
		sb.append("'draw title GDP drifter 180-segment distribution'\n\n\n");
		
		addGSForRegion(sb,ls1,box1);
		addGSForRegion(sb,ls2,box2);
		addGSForRegion(sb,ls3,box3);
		addGSForRegion(sb,ls4,box4);
		addGSForRegion(sb,ls5,box5);
		
		sb.append("'print'\n");
		sb.append("'c'\n");
		sb.append("'disable print'\n");
		sb.append("'close 1'\n");
		sb.append("'reinit'\n");
		
		writeGS(sb,path+"TrajBox.gs");
	}
	
	static List<Particle> getRecordInRegion(List<? extends Particle> all,Region2D box){
		int len=all.size();
		
		float[][] pos=new float[2][len];
		
		for(int i=0;i<len;i++){
			Particle p=all.get(i);
			
			Record r=p.getRecord(p.getMedianIndex());
			
			pos[0][i]=r.getXPos();
			pos[1][i]=r.getYPos();
		}
		
		List<Particle> ls=new ArrayList<Particle>();
		
		for(int i=0;i<len;i++)
		if(box.inRange(pos[0][i],pos[1][i])) ls.add(all.get(i));
		
		return ls;
	}
	
	static void addGSForRegion(StringBuilder sb,List<? extends Particle> ls,Region2D box){
		sb.append("'setvpage 2 3 2 1'\n");
		sb.append("'setlopts 7 0.2 5 5'\n");
		sb.append("'set line 2 1 0.1'\n");
		
		for(Particle drSeg:ls)
		sb.append("'tctrack lsmask "+path+box.getName()+"/"+drSeg.getID()+".txt'\n");
		
		sb.append("'basemap L 15 1 M'\n");
		sb.append("'set line 1 1 9'\n");
		sb.append("'drawrect '"+box.getName()+"\n");
		sb.append("'draw title trajectories for "+box.getName()+"'\n\n\n");
	}
	
	static void writeGS(StringBuilder sb,String fname){
		try{
			FileWriter fw=new FileWriter(fname);
			fw.write(sb.toString());	fw.close();
			
		}catch(IOException e){ e.printStackTrace(); System.exit(0);}
	}
}
