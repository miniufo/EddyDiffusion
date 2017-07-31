//
package GDPIO;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import diffuse.DiffusionModel;
import miniufo.lagrangian.GDPDrifter;
import miniufo.util.Region2D;


//
public final class DrifterTracks{
	// Indian Ocean
	private static final Region2D SCSWNP=new Region2D(100,0,135,30);
	
	private static final String path="d:/Data/GDP/";
	
	private static final String[] dfiles={
		path+"buoydata_1_5000.dat",
		path+"buoydata_5001_10000.dat",
		path+"buoydata_10001_15000.dat",
		path+"buoydata_15001_mar16_krigged_files.dat"
	};
	
	private static final String[] mfiles={
		path+"dirfl_1_5000.dat",
		path+"dirfl_5001_10000.dat",
		path+"dirfl_10001_15000.dat",
		path+"dirfl_15001_mar16.dat"
	};
	
	private static final List<GDPDrifter> all=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,4,SCSWNP);
	
	
	/** test*/
	public static void main(String[] args){
		StringBuilder sb=new StringBuilder();
		
		for(GDPDrifter dr:all){
			 long[] date=dr.getTimes();
			float[] lons=dr.getLongitudes();
			float[] lats=dr.getLatitudes();
			float[] ucur=dr.getZonalVelocity();
			float[] vcur=dr.getMeridionalVelocity();
			
			for(int l=0,L=lons.length;l<L;l++) if(date[l]>=20140101000000L)
			sb.append(String.format("%d %8.3f %8.3f %10.5f %10.5f\n",date[l],lons[l],lats[l],ucur[l],vcur[l]));
		}
		
		try(FileWriter fw=new FileWriter("d:/GDP2014SCSWP.txt")){ fw.write(sb.toString());}
		catch(IOException e){ e.printStackTrace(); System.exit(0);}
	}
}
