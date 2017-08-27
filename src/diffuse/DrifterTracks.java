//
package diffuse;

import java.util.ArrayList;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.lagrangian.GDPDrifter;
import miniufo.util.Region2D;


//
public final class DrifterTracks{
	// Indian Ocean
	private static final Region2D IO=new Region2D(20,-60,120,30);
	
	private static final String path="/lustre/home/qianyk/Data/GDP/";
	
	private static final String[] dfiles={
		path+"buoydata_1_5001.dat",
		path+"buoydata_5001_10000.dat",
		path+"buoydata_10001_jun13.dat"
	};
	
	private static final String[] mfiles={
		path+"dirfl_1_5000.dat",
		path+"dirfl_5001_10000.dat",
		path+"dirfl_10001_jun13.dat"
	};
	
	private static final List<GDPDrifter> all=DiffusionModel.getGDPDrifters(dfiles,mfiles,4,IO);
	
	
	/** test*/
	public static void main(String[] args){
		Region2D[] rs=new Region2D[]{
			new Region2D(30,-22,45,-21,"mzbc"),
			new Region2D(48,-26,51,-24,"mdg1"),
			new Region2D(46,-13,49,-10,"mdg2"),
			new Region2D(44,  1,46,  3,"wbc1"),
			new Region2D(75,  4,77,  6,"ind1"),
			new Region2D(98, -5,100,-3,"smtr"),
			new Region2D(110,-29,116,-26,"astr"),
			new Region2D(68, -2,72,  2,"eqtr"),
			new Region2D(37,-29,43,-28,"mzbc2"),
		};
		
		//writeTracks(rs[0],3);
		//writeTracks(rs[1],1);
		//writeTracks(rs[2],7);
		//writeTracks(rs[3],7);
		//writeTracks(rs[4],8);
		//writeTracks(rs[5],5);
		//writeTracks(rs[6]);
		//writeTracks(rs[7],11);
		writeTracks(rs[8],11);
	}
	
	
	static void writeTracks(Region2D r,int month){
		List<GDPDrifter> re=new ArrayList<>();
		
		for(GDPDrifter dr:all)
		for(int l=0,L=dr.getTCount();l<L;l++){
			float lon=dr.getXPosition(l);
			float lat=dr.getYPosition(l);
			long time=dr.getTime(l);	// 2002 0304 000000
			
			boolean inT=time/100000000L%100==month;
			
			if(r.inRange(lon,lat)&&inT){ re.add(dr); break;}
		}
		
		DiffusionModel.writeTrajAndGS(re,path+"Track/"+r.getName()+"/",r);
	}
	
	static void writeTracks(Region2D r){
		List<GDPDrifter> re=new ArrayList<>();
		
		for(GDPDrifter dr:all)
		for(int l=0,L=dr.getTCount();l<L;l++){
			float lon=dr.getXPosition(l);
			float lat=dr.getYPosition(l);
			
			if(r.inRange(lon,lat)){ re.add(dr); break;}
		}
		
		DiffusionModel.writeTrajAndGS(re,path+"Track/"+r.getName()+"/",r);
	}
}
